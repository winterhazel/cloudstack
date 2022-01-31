// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.upgrade.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.response.UsageTypeResponse;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41602to41603 implements DbUpgrade {
    protected Logger logger = Logger.getLogger(Upgrade41602to41603.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"4.16.0.2", "4.16.0.3"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.16.0.3";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41602to41603.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        convertQuotaTariffsToNewParadigm(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return new InputStream[] {};
    }

    protected void convertQuotaTariffsToNewParadigm(Connection conn) {
        logger.info("Converting quota tariffs to new paradigm.");

        List<UsageTypeResponse> usageTypeResponses = UsageTypes.listUsageTypes();

        for (UsageTypeResponse usageTypeResponse : usageTypeResponses) {
            Integer usageType = usageTypeResponse.getUsageType();

            String tariffTypeDescription = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(usageTypeResponse, "description", "usageType");

            logger.info(String.format("Converting quota tariffs of type %s to new paradigm.", tariffTypeDescription));

            for (boolean previousTariff : Arrays.asList(true, false)) {
                Map<Long, Date> tariffs = selectTariffs(conn, usageType, previousTariff, tariffTypeDescription);

                int tariffsSize = tariffs.size();
                if (tariffsSize <  2) {
                    logger.info(String.format("Quota tariff of type %s has [%s] %s register(s). Tariffs with less than 2 register do not need to be converted to new paradigm.",
                            tariffTypeDescription, tariffsSize, previousTariff ? "previous of current" : "next to current"));
                    continue;
                }

                executeUpdateQuotaTariffSetEndDateAndRemoved(conn, usageType, tariffs, previousTariff, tariffTypeDescription);
            }
        }
    }

    protected Map<Long, Date> selectTariffs(Connection conn, Integer usageType, boolean previousTariff, String tariffTypeDescription) {
        Map<Long, Date> quotaTariffs = new LinkedHashMap<>();

        String selectQuotaTariffs = String.format("SELECT id, effective_on FROM cloud_usage.quota_tariff WHERE %s AND usage_type = ? ORDER BY effective_on, updated_on;",
                previousTariff ? "usage_name = name" : "removed is null");

        logger.info(String.format("Selecting %s quota tariffs of type [%s] according to SQL [%s].", previousTariff ? "previous of current" : "next to current",
                tariffTypeDescription, selectQuotaTariffs));

        try (PreparedStatement pstmt = conn.prepareStatement(selectQuotaTariffs)) {
            pstmt.setInt(1, usageType);

            try (ResultSet result = pstmt.executeQuery()) {
                while (result.next()) {
                    quotaTariffs.put(result.getLong("id"), result.getDate("effective_on"));
                }
            }
            return quotaTariffs;
        } catch (SQLException e) {
            String message = String.format("Unable to retrieve %s quota tariffs of type [%s] due to [%s].", previousTariff ? "previous" : "next", tariffTypeDescription, e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    protected void executeUpdateQuotaTariffSetEndDateAndRemoved(Connection conn, Integer usageType, Map<Long, Date> tariffs, boolean setRemoved, String tariffTypeDescription) {
        String updateQuotaTariff = String.format("UPDATE cloud_usage.quota_tariff SET end_date = ? %s WHERE id = ?;", setRemoved ? ", removed = ?" : "");

        Object[] ids = tariffs.keySet().toArray();

        logger.info(String.format("Updating %s registers of %s quota tariffs of type [%s] with SQL [%s].", tariffs.size() -1, setRemoved ? "previous of current" : "next to current",
                tariffTypeDescription, updateQuotaTariff));

        for (int i = 0; i < tariffs.size() - 1; i++) {
            Long id = Long.valueOf(String.valueOf(ids[i]));
            Long nextId = Long.valueOf(String.valueOf(ids[i + 1]));

            Date endDate = tariffs.get(nextId);

            if (!DateUtils.isSameDay(endDate, tariffs.get(id))) {
                endDate = DateUtils.addDays(endDate, -1);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateQuotaTariff)) {
                java.sql.Date sqlEndDate = new java.sql.Date(endDate.getTime());
                pstmt.setDate(1, sqlEndDate);

                String updateRemoved = "";
                if (setRemoved) {
                    pstmt.setDate(2, sqlEndDate);
                    pstmt.setLong(3, id);

                    updateRemoved = String.format("and \"removed\" to [%s] ", sqlEndDate);
                } else {
                    pstmt.setLong(2, id);
                }

                logger.info(String.format("Updating \"end_date\" to [%s] %sof quota tariff with ID [%s].", sqlEndDate, updateRemoved, id));
                pstmt.executeUpdate();
            } catch (SQLException e) {
                String message = String.format("Unable to update \"end_date\" %s of quota tariffs of usage type [%s] due to [%s].", setRemoved ? "and \"removed\"" : "",
                        usageType, e.getMessage());
                logger.error(message, e);
                throw new CloudRuntimeException(message, e);
            }
        }
    }

}
