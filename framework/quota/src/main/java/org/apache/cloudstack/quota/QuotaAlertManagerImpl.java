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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaConfig.QuotaEmailTemplateTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaEmailConfigurationDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaEmailConfigurationVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.Account.State;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.TransactionLegacy;
import java.util.HashSet;
import java.util.Set;
import org.apache.cloudstack.utils.mailing.MailAddress;
import org.apache.cloudstack.utils.mailing.SMTPMailProperties;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;

@Component
public class QuotaAlertManagerImpl extends ManagerBase implements QuotaAlertManager {
    private static final Logger s_logger = Logger.getLogger(QuotaAlertManagerImpl.class);

    @Inject
    private AccountDao _accountDao;
    @Inject
    private QuotaAccountDao _quotaAcc;
    @Inject
    private UserDao _userDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private QuotaEmailTemplatesDao _quotaEmailTemplateDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private QuotaManager _quotaManager;

    @Inject
    private QuotaEmailConfigurationDao quotaEmailConfigurationDao;

    protected boolean _lockAccountEnforcement = false;
    private String senderAddress;
    protected SMTPMailSender mailSender;

    boolean _smtpDebug = false;

    public QuotaAlertManagerImpl() {
        super();
    }

    private void mergeConfigs(Map<String, String> dbParams, Map<String, Object> xmlParams) {
        for (Map.Entry<String, Object> param : xmlParams.entrySet()) {
            dbParams.put(param.getKey(), (String)param.getValue());
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        Map<String, String> configs = _configDao.getConfiguration(params);

        if (params != null) {
            mergeConfigs(configs, params);
        }

        senderAddress = configs.get(QuotaConfig.QuotaSmtpSender.key());
        _lockAccountEnforcement = BooleanUtils.toBoolean(configs.get(QuotaConfig.QuotaEnableEnforcement.key()));
        String smtpUsername = configs.get(QuotaConfig.QuotaSmtpUser.key());

        String namespace = "quota.usage.smtp";
        configs.put(String.format("%s.debug", namespace), String.valueOf(_smtpDebug));
        configs.put(String.format("%s.username", namespace), smtpUsername);

        mailSender = new SMTPMailSender(configs, namespace);

        return true;
    }

    @Override
    public boolean start() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Starting Alert Manager");
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Stopping Alert Manager");
        }
        return true;
    }

    @Override
    public void checkAndSendQuotaAlertEmails() {
        List<DeferredQuotaEmail> deferredQuotaEmailList = new ArrayList<DeferredQuotaEmail>();

        s_logger.info("Checking and sending quota alert emails.");
        for (final QuotaAccountVO quotaAccount : _quotaAcc.listAllQuotaAccount()) {
            checkQuotaAlertEmailForAccount(deferredQuotaEmailList, quotaAccount);
        }

        for (DeferredQuotaEmail emailToBeSent : deferredQuotaEmailList) {
            s_logger.debug(String.format("Attempting to send quota alert email to users of account [%s].", emailToBeSent.getAccount().getAccountName()));
            sendQuotaAlert(emailToBeSent);
        }
    }

    protected void checkQuotaAlertEmailForAccount(List<DeferredQuotaEmail> deferredQuotaEmailList, QuotaAccountVO quotaAccount) {
        s_logger.debug(String.format("Checking %s for email alerts.", quotaAccount));
        BigDecimal accountBalance = quotaAccount.getQuotaBalance();

        if (accountBalance == null) {
            s_logger.debug(String.format("%s has a null balance, therefore it will not receive quota alert emails.", quotaAccount));
            return;
        }

        AccountVO account = _accountDao.findById(quotaAccount.getId());
        if (account == null) {
            s_logger.debug(String.format("Account of %s is removed, thus it will not receive quota alert emails.", quotaAccount));
            return;
        }

        Date balanceDate = quotaAccount.getQuotaBalanceDate();
        Date alertDate = quotaAccount.getQuotaAlertDate();
        int lockable = quotaAccount.getQuotaEnforce();
        BigDecimal thresholdBalance = quotaAccount.getQuotaMinBalance();

        s_logger.debug(String.format("Checking %s with accountBalance [%s], alertDate [%s] and lockable [%s] to see if we should send a quota alert email.", account,
                accountBalance, alertDate, lockable));

        QuotaEmailConfigurationVO quotaEmpty = quotaEmailConfigurationDao.findByAccountIdAndEmailTemplateType(account.getAccountId(), QuotaEmailTemplateTypes.QUOTA_EMPTY);
        QuotaEmailConfigurationVO quotaLow = quotaEmailConfigurationDao.findByAccountIdAndEmailTemplateType(account.getAccountId(), QuotaEmailTemplateTypes.QUOTA_LOW);

        boolean shouldSendEmail = alertDate == null || (balanceDate.after(alertDate) && getDifferenceDays(alertDate, new Date()) > 1);

        if (accountBalance.compareTo(BigDecimal.ZERO) < 0) {
            if (_lockAccountEnforcement && (lockable == 1) && _quotaManager.isLockable(account)) {
                s_logger.info(String.format("Locking %s due to quota balance being lower than 0.", account));
                lockAccount(account.getId());
            }
            if (quotaEmpty != null && quotaEmpty.isEnabled() && shouldSendEmail) {
                s_logger.debug(String.format("Adding %s to deferred email list due to quota balance being lower than 0.", account));
                deferredQuotaEmailList.add(new DeferredQuotaEmail(account, quotaAccount, QuotaEmailTemplateTypes.QUOTA_EMPTY));
                return;
            }
        } else if (accountBalance.compareTo(thresholdBalance) < 0 && quotaLow != null && quotaLow.isEnabled() && shouldSendEmail) {
            s_logger.debug(String.format("Adding %s to deferred email list due to quota balance [%s] below threshold [%s].", account, accountBalance, thresholdBalance));
            deferredQuotaEmailList.add(new DeferredQuotaEmail(account, quotaAccount, QuotaEmailTemplateTypes.QUOTA_LOW));
            return;
        }
        s_logger.debug(String.format("%s will not receive any quota alert email in this round.", account));
    }

    @Override
    public void sendQuotaAlert(DeferredQuotaEmail emailToBeSent) {
        final AccountVO account = emailToBeSent.getAccount();
        final BigDecimal balance = emailToBeSent.getQuotaBalance();
        final BigDecimal usage = emailToBeSent.getQuotaUsage();
        final QuotaConfig.QuotaEmailTemplateTypes emailType = emailToBeSent.getEmailTemplateType();

        final List<QuotaEmailTemplatesVO> emailTemplates = _quotaEmailTemplateDao.listAllQuotaEmailTemplates(emailType.toString());
        if (emailTemplates != null && emailTemplates.get(0) != null) {
            final QuotaEmailTemplatesVO emailTemplate = emailTemplates.get(0);

            final DomainVO accountDomain = _domainDao.findByIdIncludingRemoved(account.getDomainId());
            final List<UserVO> usersInAccount = _userDao.listByAccount(account.getId());

            String userNames = "";
            final List<String> emailRecipients = new ArrayList<String>();
            for (UserVO user : usersInAccount) {
                userNames += String.format("%s <%s>,", user.getUsername(), user.getEmail());
                emailRecipients.add(user.getEmail());
            }
            if (userNames.endsWith(",")) {
                userNames = userNames.substring(0, userNames.length() - 1);
            }

            final Map<String, String> optionMap = new HashMap<String, String>();
            optionMap.put("accountName", account.getAccountName());
            optionMap.put("accountID", account.getUuid());
            optionMap.put("accountUsers", userNames);
            optionMap.put("domainName", accountDomain.getName());
            optionMap.put("domainID", accountDomain.getUuid());
            optionMap.put("quotaBalance", QuotaConfig.QuotaCurrencySymbol.value() + " " + balance.toString());
            if (emailType == QuotaEmailTemplateTypes.QUOTA_STATEMENT) {
                optionMap.put("quotaUsage", QuotaConfig.QuotaCurrencySymbol.value() + " " + usage.toString());
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("accountName" + account.getAccountName() + "accountID" + account.getUuid() + "accountUsers" + userNames + "domainName" + accountDomain.getName() + "domainID"
                        + accountDomain.getUuid());
            }

            final StrSubstitutor templateEngine = new StrSubstitutor(optionMap);
            final String subject = templateEngine.replace(emailTemplate.getTemplateSubject());
            final String body = templateEngine.replace(emailTemplate.getTemplateBody());
            try {
                sendQuotaAlert(account.getUuid(), emailRecipients, subject, body);
                emailToBeSent.sentSuccessfully(_quotaAcc);
            } catch (Exception e) {
                s_logger.error(String.format("Unable to send quota alert email (subject=%s; body=%s) to account %s (%s) recipients (%s) due to error (%s)", subject, body, account.getAccountName(),
                        account.getUuid(), emailRecipients, e));
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Exception", e);
                }
            }
        } else {
            s_logger.error(String.format("No quota email template found for type %s, cannot send quota alert email to account %s(%s)", emailType, account.getAccountName(), account.getUuid()));
        }
    }

    public long getDifferenceDays(Date d1, Date d2) {
        long diff = d2.getTime() - d1.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }

    protected boolean lockAccount(long accountId) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        boolean success = false;
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB)) {
            Account account = _accountDao.findById(accountId);
            if (account != null) {
                if (account.getState() == State.LOCKED) {
                    return true; // already locked, no-op
                } else if (account.getState() == State.ENABLED) {
                    AccountVO acctForUpdate = _accountDao.createForUpdate();
                    acctForUpdate.setState(State.LOCKED);
                    success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
                } else {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Attempting to lock a non-enabled account, current state is " + account.getState() + " (accountId: " + accountId + "), locking failed.");
                    }
                }
            } else {
                s_logger.warn("Failed to lock account " + accountId + ", account not found.");
            }
        } catch (Exception e) {
            s_logger.error("Exception occured while locking account by Quota Alert Manager", e);
            throw e;
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return success;
    }

    public static class DeferredQuotaEmail {
        private AccountVO account;
        private QuotaAccountVO quotaAccount;
        private QuotaConfig.QuotaEmailTemplateTypes emailTemplateType;
        private BigDecimal quotaUsage;

        public DeferredQuotaEmail(AccountVO account, QuotaAccountVO quotaAccount, BigDecimal quotaUsage, QuotaConfig.QuotaEmailTemplateTypes emailTemplateType) {
            this.account = account;
            this.quotaAccount = quotaAccount;
            this.emailTemplateType = emailTemplateType;
            this.quotaUsage = quotaUsage;
        }

        public DeferredQuotaEmail(AccountVO account, QuotaAccountVO quotaAccount, QuotaConfig.QuotaEmailTemplateTypes emailTemplateType) {
            this.account = account;
            this.quotaAccount = quotaAccount;
            this.emailTemplateType = emailTemplateType;
            this.quotaUsage = new BigDecimal(-1);
        }

        public AccountVO getAccount() {
            return account;
        }

        public BigDecimal getQuotaBalance() {
            return quotaAccount.getQuotaBalance();
        }

        public BigDecimal getQuotaUsage() {
            BigDecimal roundedUsage = quotaUsage.setScale(QuotaConfig.QUOTA_USAGE_ROUNDING, RoundingMode.HALF_EVEN);
            s_logger.debug(String.format("Quota usage is [%s] for [%s], but it will be rounded to [%s] decimal places to be sent via email, it will be sent as [%s].", quotaUsage,
                    quotaAccount, QuotaConfig.QUOTA_USAGE_ROUNDING, roundedUsage));
            return roundedUsage;
        }

        public Date getSendDate() {
            if (emailTemplateType == QuotaEmailTemplateTypes.QUOTA_STATEMENT) {
                return quotaAccount.getLastStatementDate();
            } else {
                return quotaAccount.getQuotaAlertDate();
            }
        }

        public QuotaConfig.QuotaEmailTemplateTypes getEmailTemplateType() {
            return emailTemplateType;
        }

        public void sentSuccessfully(final QuotaAccountDao quotaAccountDao) {
            if (emailTemplateType == QuotaEmailTemplateTypes.QUOTA_STATEMENT) {
                quotaAccount.setLastStatementDate(new Date());
            } else {
                quotaAccount.setQuotaAlertDate(new Date());
                quotaAccount.setQuotaAlertType(emailTemplateType.ordinal());
            }
            quotaAccountDao.updateQuotaAccount(quotaAccount.getAccountId(), quotaAccount);
        }
    };

    protected void sendQuotaAlert(String accountUuid, List<String> emails, String subject, String body) {
        SMTPMailProperties mailProperties = new SMTPMailProperties();

        mailProperties.setSender(new MailAddress(senderAddress));
        mailProperties.setSubject(subject);
        mailProperties.setContent(body);
        mailProperties.setContentType("text/html; charset=utf-8");

        if (CollectionUtils.isEmpty(emails)) {
            s_logger.warn(String.format("Unable to send quota alert email with subject [%s] and content [%s]. "
                    + "Account [%s] does not have users with email registered.", subject, body, accountUuid));
            return;
        }

        Set<MailAddress> addresses = new HashSet<>();
        for (String email : emails) {
            addresses.add(new MailAddress(email));
        }

        mailProperties.setRecipients(addresses);

        mailSender.sendMail(mailProperties);
    }

}
