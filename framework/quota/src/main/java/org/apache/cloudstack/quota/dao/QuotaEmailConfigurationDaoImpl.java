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
package org.apache.cloudstack.quota.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import org.apache.cloudstack.quota.vo.QuotaEmailConfigurationVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class QuotaEmailConfigurationDaoImpl extends GenericDaoBase<QuotaEmailConfigurationVO, Long> implements QuotaEmailConfigurationDao {

    private SearchBuilder<QuotaEmailConfigurationVO> searchBuilderFindByIds;

    private static Logger LOGGER = Logger.getLogger(QuotaEmailConfigurationDaoImpl.class);

    public QuotaEmailConfigurationDaoImpl() {
        super();
        searchBuilderFindByIds = createSearchBuilder();
        searchBuilderFindByIds.and("account_id", searchBuilderFindByIds.entity().getAccountId(), SearchCriteria.Op.EQ);
        searchBuilderFindByIds.and("email_template_id", searchBuilderFindByIds.entity().getEmailTemplateId(), SearchCriteria.Op.EQ);
        searchBuilderFindByIds.done();
    }

    public QuotaEmailConfigurationVO findByIds(long accountId, long emailTemplateId) {
        SearchCriteria<QuotaEmailConfigurationVO> sc = searchBuilderFindByIds.create();
        sc.setParameters("account_id", accountId);
        sc.setParameters("email_template_id", emailTemplateId);
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaEmailConfigurationVO>() {
            @Override public QuotaEmailConfigurationVO doInTransaction(TransactionStatus status) {
                return findOneBy(sc);
            }
        });
    }

    public QuotaEmailConfigurationVO updateQuotaEmailConfiguration(final QuotaEmailConfigurationVO quotaEmailConfigurationVO) {
        SearchCriteria<QuotaEmailConfigurationVO> sc = searchBuilderFindByIds.create();
        sc.setParameters("account_id", quotaEmailConfigurationVO.getAccountId());
        sc.setParameters("email_template_id", quotaEmailConfigurationVO.getEmailTemplateId());
        int result = Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<Integer>() {
            @Override public Integer doInTransaction(TransactionStatus status) {
                return update(quotaEmailConfigurationVO, sc);
            }
        });

        if (result == 0) {
            LOGGER.debug(String.format("Unable to update [%s]", quotaEmailConfigurationVO));
            return null;
        }

        return quotaEmailConfigurationVO;
    }

    public QuotaEmailConfigurationVO persistQuotaEmailConfiguration(QuotaEmailConfigurationVO quotaEmailConfigurationVO) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaEmailConfigurationVO>() {
            @Override public QuotaEmailConfigurationVO doInTransaction(TransactionStatus status) {
                return persist(quotaEmailConfigurationVO);
            }
        });
    }
}
