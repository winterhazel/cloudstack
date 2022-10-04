//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.quota;

import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaConfigureEmailCmd;
import org.apache.cloudstack.api.command.QuotaCreditsListCmd;
import org.apache.cloudstack.api.command.QuotaCreditsCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaEnabledCmd;
import org.apache.cloudstack.api.command.QuotaListEmailConfigurationCmd;
import org.apache.cloudstack.api.command.QuotaResourceQuotingCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.command.QuotaSummaryCmd;
import org.apache.cloudstack.api.command.QuotaTariffCreateCmd;
import org.apache.cloudstack.api.command.QuotaTariffDeleteCmd;
import org.apache.cloudstack.api.command.QuotaTariffListCmd;
import org.apache.cloudstack.api.command.QuotaTariffUpdateCmd;
import org.apache.cloudstack.api.command.QuotaUpdateCmd;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.utils.usage.UsageUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.configuration.Config;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;

@Component
public class QuotaServiceImpl extends ManagerBase implements QuotaService, Configurable, QuotaConfig {
    private static final Logger s_logger = Logger.getLogger(QuotaServiceImpl.class);

    @Inject
    private AccountDao _accountDao;
    @Inject
    private QuotaAccountDao _quotaAcc;
    @Inject
    private QuotaUsageDao _quotaUsageDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private QuotaBalanceDao quotaBalanceDao;
    @Inject
    private QuotaResponseBuilder _respBldr;

    private TimeZone _usageTimezone;
    private int _aggregationDuration = 0;

    public QuotaServiceImpl() {
        super();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        String timeZoneStr = _configDao.getValue(Config.UsageAggregationTimezone.toString());
        String aggregationRange = _configDao.getValue(Config.UsageStatsJobAggregationRange.toString());
        if (timeZoneStr == null) {
            timeZoneStr = "GMT";
        }
        _usageTimezone = TimeZone.getTimeZone(timeZoneStr);

        _aggregationDuration = Integer.parseInt(aggregationRange);
        if (_aggregationDuration < UsageUtils.USAGE_AGGREGATION_RANGE_MIN) {
            s_logger.warn("Usage stats job aggregation range is to small, using the minimum value of " + UsageUtils.USAGE_AGGREGATION_RANGE_MIN);
            _aggregationDuration = UsageUtils.USAGE_AGGREGATION_RANGE_MIN;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Usage timezone = " + _usageTimezone + " AggregationDuration=" + _aggregationDuration);
        }
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(QuotaEnabledCmd.class);
        if (!isQuotaServiceEnabled()) {
            return cmdList;
        }
        cmdList.add(QuotaStatementCmd.class);
        cmdList.add(QuotaBalanceCmd.class);
        cmdList.add(QuotaSummaryCmd.class);
        cmdList.add(QuotaUpdateCmd.class);
        cmdList.add(QuotaTariffListCmd.class);
        cmdList.add(QuotaTariffUpdateCmd.class);
        cmdList.add(QuotaCreditsCmd.class);
        cmdList.add(QuotaCreditsListCmd.class);
        cmdList.add(QuotaEmailTemplateListCmd.class);
        cmdList.add(QuotaEmailTemplateUpdateCmd.class);
        cmdList.add(QuotaTariffCreateCmd.class);
        cmdList.add(QuotaTariffDeleteCmd.class);
        cmdList.add(QuotaResourceQuotingCmd.class);
        cmdList.add(QuotaConfigureEmailCmd.class);
        cmdList.add(QuotaListEmailConfigurationCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return "QUOTA-PLUGIN";
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {QuotaPluginEnabled, QuotaEnableEnforcement, QuotaCurrencySymbol, QuotaStatementPeriod, QuotaSmtpHost, QuotaSmtpPort, QuotaSmtpTimeout,
                QuotaSmtpUser, QuotaSmtpPassword, QuotaSmtpAuthType, QuotaSmtpSender, QuotaSmtpEnabledSecurityProtocols, QuotaSmtpUseStartTLS, QuotaActivationRuleTimeout,
                QuotaAccountEnabled};
    }

    @Override
    public Boolean isQuotaServiceEnabled() {
        return QuotaPluginEnabled.value();
    }

    @Override
    public List<QuotaBalanceVO> listDailyQuotaBalancesForAccount(Long accountId, String accountName, Long domainId, Date startDate, Date endDate) {
        accountId = getAccountToWhomQuotaBalancesWillBeListed(accountId, accountName, domainId);

        validateStartDateAndEndDateForListDailyQuotaBalancesForAccount(startDate, endDate);

        if (startDate == null && endDate == null) {
            s_logger.debug(String.format("Retrieving last quota balance for account [%s] and domain [%s].", accountId, domainId));
            QuotaBalanceVO lastQuotaBalance = quotaBalanceDao.getLastQuotaBalanceEntry(accountId, domainId, null);

            if (lastQuotaBalance == null) {
                s_logger.debug(String.format("Did not found a quota balance entry for account [%s] and domain [%s].", accountId, domainId));
                return null;
            }

            return Arrays.asList(lastQuotaBalance);
        }

        if (endDate == null) {
            endDate = DateUtils.addDays(new Date(), -1);
        }

        Date adjustedStartDate = computeAdjustedTime(startDate);
        Date adjustedEndDate = computeAdjustedTime(endDate);

        List<QuotaBalanceVO> quotaBalances = quotaBalanceDao.listQuotaBalances(accountId, domainId, adjustedStartDate, adjustedEndDate);

        if (quotaBalances.isEmpty()) {
            s_logger.info(String.format("There are no quota balances for account [%s] in domain [%s], between [%s] and [%s].", accountId, domainId,
                    DateUtil.getOutputString(adjustedStartDate), DateUtil.getOutputString(adjustedEndDate)));
        }

        return quotaBalances;

    }

    protected void validateStartDateAndEndDateForListDailyQuotaBalancesForAccount(Date startDate, Date endDate) {
        if (startDate == null && endDate != null) {
            throw new InvalidParameterException("Parameter \"enddate\" must be informed together with parameter \"startdate\".");
        }

        Date now = new Date();
        if (startDate != null && startDate.after(now)) {
            throw new InvalidParameterValueException("The last balance can be at most from yesterday; therefore, the start date must be before today.");
        }

        if (endDate != null && endDate.after(now)) {
            throw new InvalidParameterValueException("The last balance can be at most from yesterday; therefore, the end date must be before today.");
        }

        if (ObjectUtils.allNotNull(startDate, endDate) && startDate.after(endDate)) {
            throw new InvalidParameterValueException("The start date cannot be after the end date.");
        }
    }

    protected Long getAccountToWhomQuotaBalancesWillBeListed(Long accountId, String accountName, Long domainId) {
        if (accountId != null) {
            return accountId;
        }

        validateIsChildDomain(accountName, domainId);

        List<AccountVO> accounts = _accountDao.listAccounts(accountName, domainId, new Filter(AccountVO.class, "id", false));
        if (!accounts.isEmpty()) {
            Account userAccount = accounts.get(0);

            if (userAccount != null) {
                return userAccount.getId();
            }
        }

        throw new InvalidParameterValueException(String.format("Unable to find account [%s] in domain [%s].", accountName, domainId));
    }

    protected void validateIsChildDomain(String accountName, Long domainId) {
        Account caller = CallContext.current().getCallingAccount();

        long callerDomainId = caller.getDomainId();
        if (_domainDao.isChildDomain(callerDomainId, domainId)) {
            return;
        }

        s_logger.debug(String.format("Domain with ID [%s] is not a child of the caller's domain [%s].", domainId, callerDomainId));
        throw new PermissionDeniedException(String.format("Account [%s] or domain [%s] is invalid.", accountName, domainId));
    }

    @Override
    public List<QuotaUsageVO> getQuotaUsage(Long accountId, String accountName, Long domainId, Integer usageType, Date startDate, Date endDate) {
        accountId = getAccountToWhomQuotaBalancesWillBeListed(accountId, accountName, domainId);

        if (startDate.after(endDate)) {
            throw new InvalidParameterValueException("Incorrect Date Range. Start date: " + startDate + " is after end date:" + endDate);
        }
        if (endDate.after(_respBldr.startOfNextDay())) {
            throw new InvalidParameterValueException("Incorrect Date Range. End date:" + endDate + " should not be in future. ");
        }
        Date adjustedEndDate = computeAdjustedTime(endDate);
        Date adjustedStartDate = computeAdjustedTime(startDate);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Getting quota records for account: " + accountId + ", domainId: " + domainId + ", between " + adjustedStartDate + " and " + adjustedEndDate);
        }
        return _quotaUsageDao.findQuotaUsage(accountId, domainId, usageType, adjustedStartDate, adjustedEndDate);
    }

    @Override
    public Date computeAdjustedTime(final Date date) {
        if (date == null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        TimeZone localTZ = cal.getTimeZone();
        int timezoneOffset = cal.get(Calendar.ZONE_OFFSET);
        if (localTZ.inDaylightTime(date)) {
            timezoneOffset += (60 * 60 * 1000);
        }
        cal.add(Calendar.MILLISECOND, timezoneOffset);

        Date newTime = cal.getTime();

        Calendar calTS = Calendar.getInstance(_usageTimezone);
        calTS.setTime(newTime);
        timezoneOffset = calTS.get(Calendar.ZONE_OFFSET);
        if (_usageTimezone.inDaylightTime(date)) {
            timezoneOffset += (60 * 60 * 1000);
        }

        calTS.add(Calendar.MILLISECOND, -1 * timezoneOffset);

        return calTS.getTime();
    }

    @Override
    public void setLockAccount(Long accountId, Boolean enforce) {
        QuotaAccountVO acc = _quotaAcc.findByIdQuotaAccount(accountId);
        if (acc == null) {
            acc = new QuotaAccountVO(accountId);
            acc.setQuotaEnforce(enforce ? 1 : 0);
            _quotaAcc.persistQuotaAccount(acc);
        } else {
            acc.setQuotaEnforce(enforce ? 1 : 0);
            _quotaAcc.updateQuotaAccount(accountId, acc);
        }
    }

    @Override
    public boolean saveQuotaAccount(final AccountVO account, final BigDecimal aggrUsage, final Date endDate) {
        // update quota_accounts
        QuotaAccountVO quota_account = _quotaAcc.findByIdQuotaAccount(account.getAccountId());

        if (quota_account == null) {
            quota_account = new QuotaAccountVO(account.getAccountId());
            quota_account.setQuotaBalance(aggrUsage);
            quota_account.setQuotaBalanceDate(endDate);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(quota_account);
            }
            _quotaAcc.persistQuotaAccount(quota_account);
            return true;
        } else {
            quota_account.setQuotaBalance(aggrUsage);
            quota_account.setQuotaBalanceDate(endDate);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(quota_account);
            }
            return _quotaAcc.updateQuotaAccount(account.getAccountId(), quota_account);
        }
    }

    @Override
    public void setMinBalance(Long accountId, Double balance) {
        QuotaAccountVO acc = _quotaAcc.findByIdQuotaAccount(accountId);
        if (acc == null) {
            acc = new QuotaAccountVO(accountId);
            acc.setQuotaMinBalance(new BigDecimal(balance));
            _quotaAcc.persistQuotaAccount(acc);
        } else {
            acc.setQuotaMinBalance(new BigDecimal(balance));
            _quotaAcc.updateQuotaAccount(accountId, acc);
        }
    }

}
