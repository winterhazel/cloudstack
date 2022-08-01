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
package org.apache.cloudstack.api.response;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.serializer.GsonHelper;
import com.google.common.reflect.TypeToken;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaCreditsListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaResourceQuotingCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.command.QuotaSummaryCmd;
import org.apache.cloudstack.api.command.QuotaTariffCreateCmd;
import org.apache.cloudstack.api.command.QuotaTariffListCmd;
import org.apache.cloudstack.api.command.QuotaTariffUpdateCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.quota.QuotaManager;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.cloudstack.quota.activationrule.presetvariables.GenericPresetVariable;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariableHelper;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariables;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaSummaryDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.cloudstack.quota.vo.QuotaSummaryVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.quota.vo.ResourcesQuotingResultResponse;
import org.apache.cloudstack.quota.vo.ResourcesToQuoteVo;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;

@Component
public class QuotaResponseBuilderImpl implements QuotaResponseBuilder {
    private static final Logger s_logger = Logger.getLogger(QuotaResponseBuilderImpl.class);

    @Inject
    private QuotaTariffDao _quotaTariffDao;
    @Inject
    private QuotaBalanceDao _quotaBalanceDao;
    @Inject
    private QuotaCreditsDao quotaCreditsDao;

    @Inject
    private QuotaEmailTemplatesDao _quotaEmailTemplateDao;

    @Inject
    private UserDao _userDao;
    @Inject
    private QuotaService _quotaService;
    @Inject
    private AccountDao _accountDao;

    @Inject
    private DomainDao domainDao;

    @Inject
    private AccountManager _accountMgr;

    @Inject
    private QuotaManager _quotaManager;

    @Inject
    private UsageDao usageDao;

    @Inject
    private QuotaSummaryDao quotaSummaryDao;

    @Inject
    private PresetVariableHelper presetVariableHelper;

    @Inject
    private IPAddressDao ipAddressDao;

    private Set<Account.Type> accountTypesThatCanListAllQuotaSummaries = Sets.newHashSet(Account.Type.ADMIN, Account.Type.DOMAIN_ADMIN);

    private final Type linkedListOfResourcesToQuoteType = new TypeToken<LinkedList<ResourcesToQuoteVo>>() {
    }.getType();

    @Override
    public QuotaTariffResponse createQuotaTariffResponse(QuotaTariffVO tariff) {
        final QuotaTariffResponse response = new QuotaTariffResponse();
        response.setUsageType(tariff.getUsageType());
        response.setUsageName(tariff.getUsageName());
        response.setUsageUnit(tariff.getUsageUnit());
        response.setUsageDiscriminator(tariff.getUsageDiscriminator());
        response.setTariffValue(tariff.getCurrencyValue());
        response.setEffectiveOn(tariff.getEffectiveOn());
        response.setUsageTypeDescription(tariff.getUsageTypeDescription());
        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        response.setActivationRule(tariff.getActivationRule());
        response.setName(tariff.getName());
        response.setEndDate(tariff.getEndDate());
        response.setDescription(tariff.getDescription());
        response.setUuid(tariff.getUuid());
        response.setRemoved(tariff.getRemoved());
        return response;
    }

    public Pair<List<QuotaSummaryResponse>, Integer> createQuotaSummaryResponse(QuotaSummaryCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();

        if (!accountTypesThatCanListAllQuotaSummaries.contains(caller.getType()) || !cmd.isListAll()) {
            return getQuotaSummaryResponse(caller.getAccountId(), null, null, null, null, cmd);
        }

        return getQuotaSummaryResponseWithListAll(cmd, caller);
    }

    protected Pair<List<QuotaSummaryResponse>, Integer> getQuotaSummaryResponseWithListAll(QuotaSummaryCmd cmd, Account caller) {
        String accountName = cmd.getAccountName();
        String domainUuid = cmd.getDomainId();

        Long domainId = null;
        if (accountName != null && domainUuid == null) {
            domainId = caller.getDomainId();
        } else if (domainUuid != null) {
            DomainVO domain = domainDao.findByUuidIncludingRemoved(domainUuid);

            if (domain == null) {
                throw new InvalidParameterValueException(String.format("Domain [%s] does not exist.", domainUuid));
            }

            domainId = domain.getId();
        }

        Long accountId = getAccountIdByAccountName(accountName, domainId, caller);
        String domainPath = getDomainPathByDomainIdForDomainAdmin(caller);

        return getQuotaSummaryResponse(accountId, domainId, domainPath, cmd.getStartIndex(), cmd.getPageSizeVal(), cmd);
    }

    protected Long getAccountIdByAccountName(String accountName, Long domainId, Account caller) {
        if (ObjectUtils.anyNull(accountName, domainId)) {
            return null;
        }

        Domain domain = domainDao.findByIdIncludingRemoved(domainId);
        _accountMgr.checkAccess(caller, domain);

        Account account = _accountDao.findAccountIncludingRemoved(accountName, domainId);

        if (account == null) {
            throw new InvalidParameterValueException(String.format("Account name [%s] or domain id [%s] is invalid.", accountName, domainId));
        }

        return account.getAccountId();
    }

    protected String getDomainPathByDomainIdForDomainAdmin(Account caller) {
        if (caller.getType() != Account.Type.DOMAIN_ADMIN) {
            return null;
        }

        Long domainId = caller.getDomainId();
        Domain domain = domainDao.findById(domainId);
        _accountMgr.checkAccess(caller, domain);

        if (domain == null) {
            throw new InvalidParameterValueException(String.format("Domain id [%s] is invalid.", domainId));
        }

        return domain.getPath();
    }

    protected Pair<List<QuotaSummaryResponse>, Integer> getQuotaSummaryResponse(Long accountId, Long domainId, String domainPath, Long startIndex, Long pageSize, QuotaSummaryCmd cmd) {
        Pair<List<QuotaSummaryVO>, Integer> pairSummaries = quotaSummaryDao.listQuotaSummariesForAccountAndOrDomain(accountId, domainId, domainPath, cmd.getShowRemovedAccounts(),
                startIndex, pageSize);
        List<QuotaSummaryVO> summaries = pairSummaries.first();

        if (CollectionUtils.isEmpty(summaries)) {
            s_logger.info(String.format("There are no summaries to list for parameters [%s].",
                    ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "accountName", "domainId", "listAll", "page", "pageSize")));
            return new Pair<>(new ArrayList<>(), 0);
        }

        List<QuotaSummaryResponse> responses = summaries.stream().map(summary -> {
            QuotaSummaryResponse response = new QuotaSummaryResponse();

            response.setAccountId(summary.getAccountUuid());
            response.setAccountName(summary.getAccountName());
            response.setDomainId(summary.getDomainUuid());
            response.setDomainPath(summary.getDomainPath());
            response.setBalance(summary.getQuotaBalance());
            response.setState(summary.getAccountState());
            response.setDomainRemoved(summary.getDomainRemoved() != null);
            response.setAccountRemoved(summary.getAccountRemoved() != null);
            response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
            response.setObjectName("summary");

            return response;
        }).collect(Collectors.toList());

        return new Pair<>(responses, pairSummaries.second());
    }

    @Override
    public QuotaBalanceResponse createQuotaBalanceResponse(QuotaBalanceCmd cmd) {
        List<QuotaBalanceVO> quotaBalances = getQuotaBalance(cmd);

        if (CollectionUtils.isEmpty(quotaBalances)) {
            throw new InvalidParameterValueException(String.format("There are no quota balances for the parameters [%s].",
                    ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "accountId", "accountName", "domainId", "startDate", "endDate")));
        }

        QuotaBalanceResponse response = new QuotaBalanceResponse();

        List<QuotaDailyBalanceResponse> dailyBalances = quotaBalances
                .stream()
                .map(balance -> new QuotaDailyBalanceResponse(balance.getUpdatedOn(), balance.getCreditBalance()))
                .collect(Collectors.toList());

        response.setDailyBalances(dailyBalances);
        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        response.setObjectName("balance");
        return response;
    }

    @Override
    public QuotaStatementResponse createQuotaStatementResponse(final List<QuotaUsageVO> quotaUsages, QuotaStatementCmd cmd) {
        if (CollectionUtils.isEmpty(quotaUsages)) {
            throw new InvalidParameterValueException(String.format("There is no usage data for parameters [%s].", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd,
                    "accountName", "accountId", "domainId", "startDate", "endDate", "type", "showDetails")));
        }

        s_logger.debug(String.format("Creating quota statement from [%s] usage records for parameters [%s].", quotaUsages.size(),
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "accountName", "accountId", "domainId", "startDate", "endDate", "type", "showDetails")));

        createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformed(quotaUsages, cmd.getUsageType());

        Map<Integer, List<QuotaUsageVO>> recordsPerUsageTypes = quotaUsages
                .stream()
                .sorted(Comparator.comparingInt(QuotaUsageVO::getUsageType))
                .collect(Collectors.groupingBy(QuotaUsageVO::getUsageType));

        List<QuotaStatementItemResponse> items = new ArrayList<>();

        recordsPerUsageTypes.values().forEach(usageRecords -> {
            items.add(createStatementItem(usageRecords, cmd.isShowDetails()));
        });

        QuotaStatementResponse statement = new QuotaStatementResponse();
        statement.setLineItem(items);
        statement.setTotalQuota(items.stream().map(item -> item.getQuotaUsed()).reduce(BigDecimal.ZERO, BigDecimal::add));
        statement.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        statement.setObjectName("statement");
        return statement;
    }

    protected void setStatementItemDetails(QuotaStatementItemResponse statementItem, List<QuotaUsageVO> quotaUsages, boolean showDetails) {
        if (!showDetails) {
            return;
        }

        List<QuotaStatementItemDetailResponse> itemDetails = new ArrayList<>();

        for (QuotaUsageVO quotaUsage : quotaUsages) {
            BigDecimal quotaUsed = quotaUsage.getQuotaUsed();
            if (quotaUsed == null || quotaUsed.equals(BigDecimal.ZERO)) {
                continue;
            }

            QuotaStatementItemDetailResponse detail = new QuotaStatementItemDetailResponse();
            detail.setAccountId(quotaUsage.getAccountId());
            detail.setDomainId(quotaUsage.getDomainId());
            detail.setQuotaUsed(quotaUsed);
            detail.setStartDate(quotaUsage.getStartDate());
            detail.setEndDate(quotaUsage.getEndDate());
            detail.setResponseName("quotausagedetail");
            detail.setResource(getQuotaStatementItemDetailResourceResponse(quotaUsage.getUsageItemId()));

            itemDetails.add(detail);
        }

        statementItem.setDetails(itemDetails);
    }

    protected QuotaStatementItemDetailResourceResponse getQuotaStatementItemDetailResourceResponse(Long usageItemId) {
        UsageVO usageVo = usageDao.findUsageById(usageItemId);

        if (QuotaTypes.IP_ADDRESS == usageVo.getUsageType()) {
            IPAddressVO ipAddressVo = ipAddressDao.findByIdIncludingRemoved(usageVo.getUsageId());

            return new QuotaStatementItemDetailResourceResponse(ipAddressVo.getUuid(), ipAddressVo.getAddress().addr(),
                    ipAddressVo.getRemoved() != null);
        }

        GenericPresetVariable resource = presetVariableHelper.getResourceToAddToQuotaStatementResponse(usageVo);

        if (resource.getId() != null) {
            return new QuotaStatementItemDetailResourceResponse(resource.getId(), resource.getName(), resource.isRemoved());
        }

        s_logger.debug(String.format("There is no data to load for quota type [%s] - usage record [%s].", usageVo.getUsageType(), usageVo.toString()));
        return null;
    }

    protected QuotaStatementItemResponse createStatementItem(List<QuotaUsageVO> usageRecords, boolean showDetails) {
        QuotaUsageVO firstRecord = usageRecords.get(0);
        int type = firstRecord.getUsageType();

        QuotaTypes quotaType = QuotaTypes.listQuotaTypes().get(type);

        QuotaStatementItemResponse item = new QuotaStatementItemResponse(type);
        item.setQuotaUsed(usageRecords.stream().map(record -> record.getQuotaUsed() == null ? BigDecimal.ZERO : record.getQuotaUsed()).reduce(BigDecimal.ZERO, BigDecimal::add));
        item.setAccountId(firstRecord.getAccountId());
        item.setDomainId(firstRecord.getDomainId());
        item.setUsageUnit(quotaType.getQuotaUnit());
        item.setUsageName(quotaType.getQuotaName());
        item.setObjectName("quotausage");

        setStatementItemDetails(item, usageRecords, showDetails);
        return item;
    }

    protected void createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformed(List<QuotaUsageVO> quotaUsages, Integer usageType) {
        if (usageType != null) {
            s_logger.debug(String.format("As the usage type [%s] was informed as parameter of the API quotaStatement, we will not create dummy records.", usageType));
            return;
        }

        QuotaUsageVO quotaUsage = quotaUsages.get(0);
        for (Integer quotaType : QuotaTypes.listQuotaTypes().keySet()) {
            QuotaUsageVO dummy = new QuotaUsageVO(quotaUsage);
            dummy.setUsageType(quotaType);
            dummy.setQuotaUsed(BigDecimal.ZERO);
            quotaUsages.add(dummy);
        }
    }

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listQuotaTariffPlans(final QuotaTariffListCmd cmd) {
        Date startDate = _quotaService.computeAdjustedTime(cmd.getEffectiveDate());
        Date endDate = _quotaService.computeAdjustedTime(cmd.getEndDate());
        Integer usageType = cmd.getUsageType();
        String name = cmd.getName();
        boolean listAll = cmd.isListAll();
        boolean listOnlyRemoved = cmd.isListOnlyRemoved();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();
        String uuid = cmd.getUuid();

        s_logger.debug(String.format("Listing quota tariffs for parameters [%s].", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "effectiveDate",
                "endDate", "listAll", "name", "page", "pageSize", "usageType", "uuid", "listOnlyRemoved")));

        return _quotaTariffDao.listQuotaTariffs(startDate, endDate, usageType, name, uuid, listAll, listOnlyRemoved, startIndex, pageSize);
    }

    @Override
    public QuotaTariffVO updateQuotaTariffPlan(QuotaTariffUpdateCmd cmd) {
        String name = cmd.getName();
        Double value = cmd.getValue();
        Date endDate = _quotaService.computeAdjustedTime(cmd.getEndDate());
        String description = cmd.getDescription();
        String activationRule = cmd.getActivationRule();
        Date now = _quotaService.computeAdjustedTime(new Date());

        warnQuotaTariffUpdateDeprecatedFields(cmd);

        QuotaTariffVO currentQuotaTariff = _quotaTariffDao.findByName(name);

        if (currentQuotaTariff == null) {
            throw new InvalidParameterValueException(String.format("There is no quota tariffs with name [%s].", name));
        }

        Date currentQuotaTariffStartDate = currentQuotaTariff.getEffectiveOn();

        currentQuotaTariff.setRemoved(now);

        QuotaTariffVO newQuotaTariff = persistNewQuotaTariff(currentQuotaTariff, name, 0, currentQuotaTariffStartDate, cmd.getEntityOwnerId(), endDate, value, description,
                activationRule);
        _quotaTariffDao.updateQuotaTariff(currentQuotaTariff);
        return newQuotaTariff;
    }

    protected void warnQuotaTariffUpdateDeprecatedFields(QuotaTariffUpdateCmd cmd) {
        String warnMessage = "The parameter 's%s' for API 'quotaTariffUpdate' is no longer needed and it will be removed in future releases.";

        if (cmd.getStartDate() != null) {
            s_logger.warn(String.format(warnMessage, "startdate"));
        }

        if (cmd.getUsageType() != null) {
            s_logger.warn(String.format(warnMessage, "usagetype"));
        }
    }

    @Override
    public QuotaCreditsResponse addQuotaCredits(Long accountId, Long domainId, Double amount, Long updatedBy, Boolean enforce) {
        Date despositedOn = _quotaService.computeAdjustedTime(new Date());
        QuotaBalanceVO qb = _quotaBalanceDao.findLaterBalanceEntry(accountId, domainId, despositedOn);

        if (qb != null) {
            throw new InvalidParameterValueException("Incorrect deposit date: " + despositedOn + " there are balance entries after this date");
        }

        QuotaCreditsVO credits = new QuotaCreditsVO(accountId, domainId, new BigDecimal(amount), updatedBy);
        credits.setUpdatedOn(despositedOn);
        QuotaCreditsVO result = quotaCreditsDao.saveCredits(credits);

        final AccountVO account = _accountDao.findById(accountId);
        if (account == null) {
            throw new InvalidParameterValueException("Account does not exist with account id " + accountId);
        }
        final boolean lockAccountEnforcement = "true".equalsIgnoreCase(QuotaConfig.QuotaEnableEnforcement.value());
        final BigDecimal currentAccountBalance = _quotaBalanceDao.getLastQuotaBalance(accountId, domainId);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("AddQuotaCredits: Depositing " + amount + " on adjusted date " + despositedOn + ", current balance " + currentAccountBalance);
        }
        // update quota account with the balance
        _quotaService.saveQuotaAccount(account, currentAccountBalance, despositedOn);
        if (lockAccountEnforcement) {
            if (currentAccountBalance.compareTo(new BigDecimal(0)) >= 0) {
                if (account.getState() == Account.State.LOCKED) {
                    s_logger.info("UnLocking account " + account.getAccountName() + " , due to positive balance " + currentAccountBalance);
                    _accountMgr.enableAccount(account.getAccountName(), domainId, accountId);
                }
            } else { // currentAccountBalance < 0 then lock the account
                if (_quotaManager.isLockable(account) && account.getState() == Account.State.ENABLED && enforce) {
                    s_logger.info("Locking account " + account.getAccountName() + " , due to negative balance " + currentAccountBalance);
                    _accountMgr.lockAccount(account.getAccountName(), domainId, accountId);
                }
            }
        }

        String creditor = String.valueOf(Account.ACCOUNT_ID_SYSTEM);
        User creditorUser = _userDao.getUser(updatedBy);
        if (creditorUser != null) {
            creditor = creditorUser.getUsername();
        }

        QuotaCreditsResponse response = new QuotaCreditsResponse();

        if (result != null) {
            response.setCredit(result.getCredit());
            response.setCreditedOn(new Date());
            response.setAccountCreditorId(creditor);
        }

        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        return response;
    }

    private QuotaEmailTemplateResponse createQuotaEmailResponse(QuotaEmailTemplatesVO template) {
        QuotaEmailTemplateResponse response = new QuotaEmailTemplateResponse();
        response.setTemplateType(template.getTemplateName());
        response.setTemplateSubject(template.getTemplateSubject());
        response.setTemplateText(template.getTemplateBody());
        response.setLocale(template.getLocale());
        response.setLastUpdatedOn(template.getLastUpdated());
        return response;
    }

    @Override
    public List<QuotaEmailTemplateResponse> listQuotaEmailTemplates(QuotaEmailTemplateListCmd cmd) {
        final String templateName = cmd.getTemplateName();
        List<QuotaEmailTemplatesVO> templates = _quotaEmailTemplateDao.listAllQuotaEmailTemplates(templateName);
        final List<QuotaEmailTemplateResponse> responses = new ArrayList<QuotaEmailTemplateResponse>();
        for (final QuotaEmailTemplatesVO template : templates) {
            responses.add(createQuotaEmailResponse(template));
        }
        return responses;
    }

    @Override
    public boolean updateQuotaEmailTemplate(QuotaEmailTemplateUpdateCmd cmd) {
        final String templateName = cmd.getTemplateName();
        final String templateSubject = cmd.getTemplateSubject();
        final String templateBody = cmd.getTemplateBody();
        final String locale = cmd.getLocale();

        final List<QuotaEmailTemplatesVO> templates = _quotaEmailTemplateDao.listAllQuotaEmailTemplates(templateName);
        if (templates.size() == 1) {
            final QuotaEmailTemplatesVO template = templates.get(0);
            template.setTemplateSubject(templateSubject);
            template.setTemplateBody(templateBody);
            if (locale != null) {
                template.setLocale(locale);
            }
            return _quotaEmailTemplateDao.updateQuotaEmailTemplate(template);
        }
        return false;
    }

    @Override
    public List<QuotaUsageVO> getQuotaUsage(QuotaStatementCmd cmd) {
        return _quotaService.getQuotaUsage(cmd.getAccountId(), cmd.getAccountName(), cmd.getDomainId(), cmd.getUsageType(), cmd.getStartDate(), cmd.getEndDate());
    }

    @Override
    public List<QuotaBalanceVO> getQuotaBalance(QuotaBalanceCmd cmd) {
        return _quotaService.listDailyQuotaBalancesForAccount(cmd.getAccountId(), cmd.getAccountName(), cmd.getDomainId(), cmd.getStartDate(), cmd.getEndDate());
    }

    @Override
    public Date startOfNextDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return createDateAtTheStartOfNextDay(localDate);
    }

    @Override
    public Date startOfNextDay() {
        LocalDate localDate = LocalDate.now();
        return createDateAtTheStartOfNextDay(localDate);
    }

    private Date createDateAtTheStartOfNextDay(LocalDate localDate) {
        LocalDate nextDayLocalDate = localDate.plusDays(1);
        return Date.from(nextDayLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public QuotaTariffVO createQuotaTariff(QuotaTariffCreateCmd cmd) {
        String name = cmd.getName();
        int usageType = cmd.getUsageType();
        Date startDate = cmd.getStartDate();
        Date now = new Date();
        startDate = _quotaService.computeAdjustedTime(startDate == null ? now : startDate);
        Date endDate = _quotaService.computeAdjustedTime(cmd.getEndDate());
        Double value = cmd.getValue();
        String description = cmd.getDescription();
        String activationRule = cmd.getActivationRule();

        QuotaTariffVO currentQuotaTariff = _quotaTariffDao.findByName(name);

        if (currentQuotaTariff != null) {
            throw new InvalidParameterValueException(String.format("A quota tariff with name [%s] already exist.", name));
        }

        if (startDate.compareTo(now) < 0) {
            throw new InvalidParameterValueException(String.format("The quota tariff's start date [%s] cannot be less than now [%s]", startDate, now));
        }

        return persistNewQuotaTariff(null, name, usageType, startDate, cmd.getEntityOwnerId(), endDate, value, description, activationRule);
    }

    protected QuotaTariffVO persistNewQuotaTariff(QuotaTariffVO currentQuotaTariff, String name, int usageType, Date startDate, Long entityOwnerId, Date endDate, Double value,
                                                  String description, String activationRule) {

        QuotaTariffVO newQuotaTariff = getNewQuotaTariffObject(currentQuotaTariff, name, usageType);

        newQuotaTariff.setEffectiveOn(startDate);
        newQuotaTariff.setUpdatedOn(startDate);
        newQuotaTariff.setUpdatedBy(entityOwnerId);

        validateEndDateOnCreatingNewQuotaTariff(newQuotaTariff, startDate, endDate);
        validateValueOnCreatingNewQuotaTariff(newQuotaTariff, value);
        validateStringsOnCreatingNewQuotaTariff(newQuotaTariff::setDescription, description);
        validateStringsOnCreatingNewQuotaTariff(newQuotaTariff::setActivationRule, activationRule);

        _quotaTariffDao.addQuotaTariff(newQuotaTariff);
        return newQuotaTariff;
    }

    protected QuotaTariffVO getNewQuotaTariffObject(QuotaTariffVO currentQuotaTariff, String name, int usageType) {
        if (currentQuotaTariff != null) {
            return new QuotaTariffVO(currentQuotaTariff);
        }

        QuotaTariffVO newQuotaTariff = new QuotaTariffVO();

        if (!newQuotaTariff.setUsageTypeData(usageType)) {
            throw new InvalidParameterValueException(String.format("There is no usage type with value [%s].", usageType));
        }

        newQuotaTariff.setName(name);
        return newQuotaTariff;
    }

    protected void validateStringsOnCreatingNewQuotaTariff(Consumer<String> method, String value) {
        if (value != null) {
            method.accept(value.isBlank() ? null : value);
        }
    }

    protected void validateValueOnCreatingNewQuotaTariff(QuotaTariffVO newQuotaTariff, Double value) {
        if (value != null) {
            newQuotaTariff.setCurrencyValue(new BigDecimal(value));
        }
    }

    protected void validateEndDateOnCreatingNewQuotaTariff(QuotaTariffVO newQuotaTariff, Date startDate, Date endDate) {
        if (endDate == null) {
            return;
        }

        if (endDate.compareTo(startDate) < 0) {
            throw new InvalidParameterValueException(String.format("The quota tariff's end date [%s] cannot be less than the start date [%s]", endDate, startDate));
        }

        Date now = _quotaService.computeAdjustedTime(new Date());
        if (endDate.compareTo(now) < 0) {
            throw new InvalidParameterValueException(String.format("The quota tariff's end date [%s] cannot be less than now [%s].", endDate, now));
        }

        newQuotaTariff.setEndDate(endDate);
    }

    public boolean deleteQuotaTariff(String quotaTariffUuid) {
        QuotaTariffVO quotaTariff = _quotaTariffDao.findByUuid(quotaTariffUuid);

        if (quotaTariff == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Cannot find quota tariff with the provided uuid.");
        }

        quotaTariff.setRemoved(_quotaService.computeAdjustedTime(new Date()));
        return _quotaTariffDao.updateQuotaTariff(quotaTariff);
    }

    @Override
    public Pair<List<QuotaCreditsResponse>, Integer> createQuotaCreditsListResponse(QuotaCreditsListCmd cmd) {
        List<QuotaCreditsVO> credits = getCreditsForQuotaCreditsList(cmd);

        Map<Long, AccountVO> mapAccount = new HashMap<>();
        List<QuotaCreditsResponse> creditResponses = credits.stream().map(credit -> getQuotaCreditsResponse(mapAccount, credit)).collect(Collectors.toList());

        return new Pair<>(creditResponses, creditResponses.size());
    }

    protected QuotaCreditsResponse getQuotaCreditsResponse(Map<Long, AccountVO> mapAccount, QuotaCreditsVO credit) {
        QuotaCreditsResponse response = new QuotaCreditsResponse();

        AccountVO account = getAccountById(credit, mapAccount);

        response.setAccountCreditorId(account.getUuid());
        response.setAccountCreditorName(account.getAccountName());
        response.setCredit(credit.getCredit());
        response.setCreditedOn(credit.getUpdatedOn());
        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        response.setObjectName("credit");

        return response;
    }

    protected List<QuotaCreditsVO> getCreditsForQuotaCreditsList(QuotaCreditsListCmd cmd) {
        Long accountId = cmd.getAccountId();
        Long domainId = cmd.getDomainId();
        Date startDate = cmd.getStartDate();
        Date endDate = cmd.getEndDate();

        if (startDate.after(endDate)) {
            throw new InvalidParameterValueException("The start date must be before the end date.");
        }

        List<QuotaCreditsVO> credits = quotaCreditsDao.findCredits(accountId, domainId, startDate, endDate);

        if (CollectionUtils.isNotEmpty(credits)) {
            return credits;
        }

        String message = String.format("There are no credit statements for parameters [%s].", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "accountName",
                "domainId", "startDate", "endDate"));
        s_logger.debug(message);
        throw new InvalidParameterValueException(message);
    }

    protected AccountVO getAccountById(QuotaCreditsVO credit, Map<Long, AccountVO> mapAccount) {
        Long accountId = credit.getUpdatedBy();
        AccountVO accountVo = mapAccount.get(accountId);

        if (accountVo != null) {
            return accountVo;
        }

        accountVo = _accountDao.findByIdIncludingRemoved(accountId);

        if (accountVo == null) {
            s_logger.error(String.format("Could not find creditor account with ID [%s] for credit [%s].", accountId, credit.toString()));
            throw new InternalServerErrorException("Could not find creditor's account.");
        }

        mapAccount.put(accountId, accountVo);
        return accountVo;
    }

    @Override
    public List<ResourcesQuotingResultResponse> quoteResources(String resourcesToQuoteAsJson) {
        s_logger.trace(String.format("Parsing JSON [%s] to a list of [%s].", resourcesToQuoteAsJson, ResourcesToQuoteVo.class.getName()));
        LinkedList<ResourcesToQuoteVo> resourcesToQuote;
        try {
            resourcesToQuote = GsonHelper.getGson().fromJson(resourcesToQuoteAsJson, linkedListOfResourcesToQuoteType);
        } catch (Exception e) {
            s_logger.error(String.format("Could not parse the JSON passed as parameter [%s] due to [%s].", resourcesToQuoteAsJson, e.getMessage()), e);
            throw new InvalidParameterValueException("Could not parse the JSON passed as parameter.");
        }

        if (CollectionUtils.isEmpty(resourcesToQuote)) {
            throw new InvalidParameterValueException("No resources were informed for quoting.");
        }

        s_logger.info("Validating quotings fields.");
        Set<Integer> usageTypes = validateResourcesToQuoteFieldsAndReturnUsageTypes(resourcesToQuote);
        validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(resourcesToQuote);

        return _quotaManager.quoteResources(resourcesToQuote, usageTypes);
    }

    /**
     * Validates if all the usage types informed are valid and return them. Also adds an ID (its index in the list) to the quotings, if not informed.
     */
    protected Set<Integer> validateResourcesToQuoteFieldsAndReturnUsageTypes(List<ResourcesToQuoteVo> resourcesToQuote) throws InvalidParameterValueException {
        Map<Integer, String> usageTypes = new HashMap<>();
        s_logger.debug("Validating usage types set in the quotings.");

        for (int index = 0; index < resourcesToQuote.size(); index++) {
            ResourcesToQuoteVo resourceToQuote = resourcesToQuote.get(index);

            int usageType = validateResourceToQuoteUsageTypeAndReturnsItsId(index, resourceToQuote.getUsageType());

            s_logger.debug(String.format("Adding [%s] to the set of usage types that are being quoted.", usageType));
            usageTypes.put(usageType, resourceToQuote.getUsageType());

            addIdToResourceToQuoteIfNotSet(index, resourceToQuote);
        }

        s_logger.debug(String.format("The following usage types were identified for quoting and will be used to retrieve the quota tariffs: %s.", usageTypes.values()));
        return usageTypes.keySet();
    }

    /**
     *  If the ID is not informed, the index will be set as ID.
     */
    protected void addIdToResourceToQuoteIfNotSet(int index, ResourcesToQuoteVo resourceToQuote) {
        if (StringUtils.isNotBlank(resourceToQuote.getId())) {
            return;
        }

        s_logger.debug(String.format("Quoting at index [%s] does not have an ID. We set [%s] as the ID for this quote.", index, index));
        resourceToQuote.setId(String.valueOf(index));
    }

    /**
     * Validates if the usage type passed as parameter is valid and returns its ID.
     */
    protected int validateResourceToQuoteUsageTypeAndReturnsItsId(int index, String usageType) throws InvalidParameterValueException {
        s_logger.debug(String.format("Validating quoting field \"usageType\" at index [%s].", index));

        if (StringUtils.isNotBlank(usageType)) {
            return QuotaTypes.getQuotaTypeByName(usageType).getQuotaType();
        }

        String msg = String.format("Quoting field \"usageType\"'s value [%s], at index [%s], is not a valid value. See [%s] for more information.", usageType, index,
                QuotaResourceQuotingCmd.LINK_TO_QUOTA_SPEC);
        s_logger.error(msg);
        throw new InvalidParameterValueException(msg);
    }

    protected void validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(List<ResourcesToQuoteVo> resourcesToQuote) throws PermissionDeniedException {
        Account caller = CallContext.current().getCallingAccount();

        s_logger.debug(String.format("Validating caller [%s] access to the accounts and domains passed as parameter in the quotings.", caller));

        for (int index = 0; index < resourcesToQuote.size(); index++) {
            PresetVariables metadata = resourcesToQuote.get(index).getMetadata();

            if (metadata == null) {
                s_logger.trace(String.format("Quoting at index [%s] does not have metadata. Skipping account and domain access check.", index));
                continue;
            }

            validateCallerAccessToAccountSetInQuotingMetadata(caller, metadata, index);
            validateCallerAccessToDomainSetInQuotingMetadata(caller, metadata, index);
        }

        s_logger.debug(String.format("Caller [%s] has access to the accounts and domains passed as parameter in the quoting metadata.", caller));
    }

    protected void validateCallerAccessToAccountSetInQuotingMetadata(Account caller, PresetVariables metadata, int index) throws PermissionDeniedException {
        String accountId = getPresetVariableIdIfItIsNotNull(metadata.getAccount());
        if (accountId == null) {
            s_logger.debug(String.format("Quoting metadata at index [%s] does not have an account. Skipping account access check.", index));
            return;
        }

        s_logger.trace(String.format("Searching account with UUID [%s], informed in quoting metadata at index [%s].", accountId, index));
        Account account = _accountDao.findByUuidIncludingRemoved(accountId);

        if (account == null) {
            s_logger.debug(String.format("Account with UUID [%s], informed in quoting metadata at index [%s], does not exist. Skipping account access check.", accountId, index));
            return;
        }

        s_logger.trace(String.format("Checking caller [%s] access to account [%s], informed in quoting metadata at index [%s].", caller, account, index));
        _accountMgr.checkAccess(caller, null, false, account);
        s_logger.trace(String.format("Caller [%s] has access to account [%s], informed in quoting metadata at index [%s].", caller, account, index));
    }

    protected void validateCallerAccessToDomainSetInQuotingMetadata(Account caller, PresetVariables metadata, int index) throws PermissionDeniedException {
        String domainId = getPresetVariableIdIfItIsNotNull(metadata.getDomain());
        if (domainId == null) {
            s_logger.debug(String.format("Quoting metadata at index [%s] does not have a domain. Skipping domain access check.", index));
            return;
        }

        s_logger.trace(String.format("Searching domain with UUID [%s], informed in quoting metadata at index [%s].", domainId, index));
        DomainVO domain = domainDao.findByUuidIncludingRemoved(domainId);

        if (domain == null) {
            s_logger.debug(String.format("Domain with UUID [%s], informed in quoting metadata at index [%s], does not exist. Skipping domain access check.", domainId, index));
            return;
        }

        s_logger.trace(String.format("Checking caller [%s] access to domain [%s], informed in quoting metadata at index [%s].", caller, domain, index));
        _accountMgr.checkAccess(caller, domain);
        s_logger.trace(String.format("Caller [%s] has access to domain [%s], informed in quoting metadata at index [%s].", caller, domain, index));
    }

    protected String getPresetVariableIdIfItIsNotNull(GenericPresetVariable genericPresetVariable) {
        if (genericPresetVariable != null) {
            return genericPresetVariable.getId();
        }
        return null;
    }

}
