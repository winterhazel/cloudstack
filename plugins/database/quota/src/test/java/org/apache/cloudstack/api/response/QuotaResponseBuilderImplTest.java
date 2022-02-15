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
package org.apache.cloudstack.api.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaSummaryCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;

import junit.framework.TestCase;

@RunWith(PowerMockRunner.class)
public class QuotaResponseBuilderImplTest extends TestCase {

    @Mock
    QuotaTariffDao quotaTariffDaoMock;

    @Mock
    QuotaBalanceDao quotaBalanceDaoMock;

    @Mock
    QuotaCreditsDao quotaCreditsDaoMock;

    @Mock
    QuotaEmailTemplatesDao quotaEmailTemplateDaoMock;

    @Mock
    UserDao userDaoMock;

    @Mock
    QuotaService quotaServiceMock;

    @Mock
    AccountDao accountDaoMock;

    @Mock
    Consumer<String> consumerStringMock;

    @Mock
    QuotaTariffVO quotaTariffVoMock;

    @Mock
    UsageDao usageDaoMock;

    @InjectMocks
    QuotaResponseBuilderImpl quotaResponseBuilderImplSpy = Mockito.spy(QuotaResponseBuilderImpl.class);

    @Mock
    CallContext callContextMock;

    @Mock
    Account accountMock;

    @Mock
    DomainDao domainDaoMock;

    @Mock
    AccountManager accountManagerMock;

    @Mock
    DomainVO domainVoMock;

    @Mock
    Pair<List<QuotaSummaryResponse>, Integer> quotaSummaryResponseMock1, quotaSummaryResponseMock2;

    Date date = new Date();
    Set<Short> accountTypes = Sets.newHashSet(Account.ACCOUNT_TYPE_NORMAL, Account.ACCOUNT_TYPE_ADMIN, Account.ACCOUNT_TYPE_DOMAIN_ADMIN,
            Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN, Account.ACCOUNT_TYPE_READ_ONLY_ADMIN, Account.ACCOUNT_TYPE_PROJECT);

    private QuotaTariffVO makeTariffTestData() {
        QuotaTariffVO tariffVO = new QuotaTariffVO();
        tariffVO.setUsageType(QuotaTypes.IP_ADDRESS);
        tariffVO.setUsageName("ip address");
        tariffVO.setUsageUnit("IP-Month");
        tariffVO.setCurrencyValue(BigDecimal.valueOf(100.19));
        tariffVO.setEffectiveOn(new Date());
        tariffVO.setUsageDiscriminator("");
        return tariffVO;
    }

    @Test
    public void testQuotaResponse() {
        QuotaTariffVO tariffVO = makeTariffTestData();
        QuotaTariffResponse response = quotaResponseBuilderImplSpy.createQuotaTariffResponse(tariffVO);
        assertTrue(tariffVO.getUsageType() == response.getUsageType());
        assertTrue(tariffVO.getCurrencyValue().equals(response.getTariffValue()));
    }

    @Test
    public void testAddQuotaCredits() {
        final long accountId = 2L;
        final long domainId = 1L;
        final double amount = 11.0;
        final long updatedBy = 2L;

        QuotaCreditsVO credit = new QuotaCreditsVO();
        credit.setCredit(new BigDecimal(amount));

        Mockito.when(quotaCreditsDaoMock.saveCredits(Mockito.any(QuotaCreditsVO.class))).thenReturn(credit);
        Mockito.when(quotaBalanceDaoMock.getLastQuotaBalance(Mockito.anyLong(), Mockito.anyLong())).thenReturn(new BigDecimal(111));
        Mockito.when(quotaServiceMock.computeAdjustedTime(Mockito.any(Date.class))).thenReturn(new Date());

        AccountVO account = new AccountVO();
        account.setState(Account.State.locked);
        Mockito.when(accountDaoMock.findById(Mockito.anyLong())).thenReturn(account);

        QuotaCreditsResponse resp = quotaResponseBuilderImplSpy.addQuotaCredits(accountId, domainId, amount, updatedBy, true);
        assertTrue(resp.getCredits().compareTo(credit.getCredit()) == 0);
    }

    @Test
    public void testListQuotaEmailTemplates() {
        QuotaEmailTemplateListCmd cmd = new QuotaEmailTemplateListCmd();
        cmd.setTemplateName("some name");
        List<QuotaEmailTemplatesVO> templates = new ArrayList<>();
        QuotaEmailTemplatesVO template = new QuotaEmailTemplatesVO();
        template.setTemplateName("template");
        templates.add(template);
        Mockito.when(quotaEmailTemplateDaoMock.listAllQuotaEmailTemplates(Mockito.anyString())).thenReturn(templates);

        assertTrue(quotaResponseBuilderImplSpy.listQuotaEmailTemplates(cmd).size() == 1);
    }

    @Test
    public void testUpdateQuotaEmailTemplate() {
        QuotaEmailTemplateUpdateCmd cmd = new QuotaEmailTemplateUpdateCmd();
        cmd.setTemplateBody("some body");
        cmd.setTemplateName("some name");
        cmd.setTemplateSubject("some subject");

        List<QuotaEmailTemplatesVO> templates = new ArrayList<>();

        Mockito.when(quotaEmailTemplateDaoMock.listAllQuotaEmailTemplates(Mockito.anyString())).thenReturn(templates);
        Mockito.when(quotaEmailTemplateDaoMock.updateQuotaEmailTemplate(Mockito.any(QuotaEmailTemplatesVO.class))).thenReturn(true);

        // invalid template test
        assertFalse(quotaResponseBuilderImplSpy.updateQuotaEmailTemplate(cmd));

        // valid template test
        QuotaEmailTemplatesVO template = new QuotaEmailTemplatesVO();
        template.setTemplateName("template");
        templates.add(template);
        assertTrue(quotaResponseBuilderImplSpy.updateQuotaEmailTemplate(cmd));
    }

    @Test
    public void testStartOfNextDayWithoutParameters() {
        Date nextDate = quotaResponseBuilderImplSpy.startOfNextDay();

        LocalDateTime tomorrowAtStartOfTheDay = LocalDate.now().atStartOfDay().plusDays(1);
        Date expectedNextDate = Date.from(tomorrowAtStartOfTheDay.atZone(ZoneId.systemDefault()).toInstant());

        Assert.assertEquals(expectedNextDate, nextDate);
    }

    @Test
    public void testStartOfNextDayWithParameter() {
        Date anyDate = new Date(1242421545757532l);

        Date nextDayDate = quotaResponseBuilderImplSpy.startOfNextDay(anyDate);

        LocalDateTime nextDayLocalDateTimeAtStartOfTheDay = anyDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1).atStartOfDay();
        Date expectedNextDate = Date.from(nextDayLocalDateTimeAtStartOfTheDay.atZone(ZoneId.systemDefault()).toInstant());

        Assert.assertEquals(expectedNextDate, nextDayDate);
    }

    @Test
    public void validateStringsOnCreatingNewQuotaTariffTestNullValueDoNothing() {
        quotaResponseBuilderImplSpy.validateStringsOnCreatingNewQuotaTariff(consumerStringMock, null);
        Mockito.verify(consumerStringMock, Mockito.never()).accept(Mockito.anyString());
    }

    @Test
    public void validateStringsOnCreatingNewQuotaTariffTestEmptyValueCallMethodWithNull() {
        quotaResponseBuilderImplSpy.validateStringsOnCreatingNewQuotaTariff(consumerStringMock, "");
        Mockito.verify(consumerStringMock).accept(null);
    }

    @Test
    public void validateStringsOnCreatingNewQuotaTariffTestValueCallMethodWithValue() {
        String value = "test";
        quotaResponseBuilderImplSpy.validateStringsOnCreatingNewQuotaTariff(consumerStringMock, value);
        Mockito.verify(consumerStringMock).accept(value);
    }

    @Test
    public void validateValueOnCreatingNewQuotaTariffTestNullValueDoNothing() {
        quotaResponseBuilderImplSpy.validateValueOnCreatingNewQuotaTariff(quotaTariffVoMock, null);
        Mockito.verify(quotaTariffVoMock, Mockito.never()).setCurrencyValue(Mockito.any(BigDecimal.class));
    }

    @Test
    public void validateValueOnCreatingNewQuotaTariffTestAnyValueIsSet() {
        Double value = 0.0;
        quotaResponseBuilderImplSpy.validateValueOnCreatingNewQuotaTariff(quotaTariffVoMock, value);
        Mockito.verify(quotaTariffVoMock).setCurrencyValue(new BigDecimal(value));
    }

    @Test
    public void validateEndDateOnCreatingNewQuotaTariffTestNullEndDateDoNothing() {
        Date startDate = null;
        Date endDate = null;

        quotaResponseBuilderImplSpy.validateEndDateOnCreatingNewQuotaTariff(quotaTariffVoMock, startDate, endDate);
        Mockito.verify(quotaTariffVoMock, Mockito.never()).setEndDate(Mockito.any(Date.class));
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateEndDateOnCreatingNewQuotaTariffTestEndDateLessThanStartDateThrowInvalidParameterValueException() {
        Date startDate = date;
        Date endDate = DateUtils.addSeconds(startDate, -1);

        quotaResponseBuilderImplSpy.validateEndDateOnCreatingNewQuotaTariff(quotaTariffVoMock, startDate, endDate);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateEndDateOnCreatingNewQuotaTariffTestEndDateLessThanNowThrowInvalidParameterValueException() {
        Date startDate = DateUtils.addDays(date, -100);
        Date endDate = DateUtils.addDays(new Date(), -1);

        Mockito.doReturn(date).when(quotaServiceMock).computeAdjustedTime(Mockito.any(Date.class));
        quotaResponseBuilderImplSpy.validateEndDateOnCreatingNewQuotaTariff(quotaTariffVoMock, startDate, endDate);
    }

    @Test
    public void validateEndDateOnCreatingNewQuotaTariffTestSetValidEndDate() {
        Date startDate = DateUtils.addDays(date, -100);
        Date endDate = date;

        Mockito.doReturn(DateUtils.addDays(date, -10)).when(quotaServiceMock).computeAdjustedTime(Mockito.any(Date.class));
        quotaResponseBuilderImplSpy.validateEndDateOnCreatingNewQuotaTariff(quotaTariffVoMock, startDate, endDate);
        Mockito.verify(quotaTariffVoMock).setEndDate(Mockito.any(Date.class));
    }

    @Test
    @PrepareForTest(QuotaResponseBuilderImpl.class)
    public void getNewQuotaTariffObjectTestCreateFromCurrentQuotaTariff() throws Exception {
        PowerMockito.whenNew(QuotaTariffVO.class).withArguments(Mockito.any(QuotaTariffVO.class)).thenReturn(quotaTariffVoMock);

        quotaResponseBuilderImplSpy.getNewQuotaTariffObject(quotaTariffVoMock, "", 0);
        PowerMockito.verifyNew(QuotaTariffVO.class).withArguments(Mockito.any(QuotaTariffVO.class));
    }

    @Test (expected = InvalidParameterValueException.class)
    public void getNewQuotaTariffObjectTestSetInvalidUsageTypeThrowsInvalidParameterValueException() throws InvalidParameterValueException {
        quotaResponseBuilderImplSpy.getNewQuotaTariffObject(null, "test", 0);
    }

    @Test
    public void getNewQuotaTariffObjectTestReturnValidObject() throws InvalidParameterValueException {
        String name = "test";
        int usageType = 1;
        QuotaTariffVO result = quotaResponseBuilderImplSpy.getNewQuotaTariffObject(null, name, usageType);

        Assert.assertEquals(name, result.getName());
        Assert.assertEquals(usageType, result.getUsageType());
    }

    @Test
    public void persistNewQuotaTariffTestpersistNewQuotaTariff() {
        Mockito.doReturn(quotaTariffVoMock).when(quotaResponseBuilderImplSpy).getNewQuotaTariffObject(Mockito.any(QuotaTariffVO.class), Mockito.anyString(), Mockito.anyInt());
        Mockito.doNothing().when(quotaResponseBuilderImplSpy).validateEndDateOnCreatingNewQuotaTariff(Mockito.any(QuotaTariffVO.class), Mockito.any(Date.class), Mockito.any(Date.class));
        Mockito.doNothing().when(quotaResponseBuilderImplSpy).validateValueOnCreatingNewQuotaTariff(Mockito.any(QuotaTariffVO.class), Mockito.anyDouble());
        Mockito.doNothing().when(quotaResponseBuilderImplSpy).validateStringsOnCreatingNewQuotaTariff(Mockito.<Consumer<String>>any(), Mockito.anyString());
        Mockito.doReturn(quotaTariffVoMock).when(quotaTariffDaoMock).addQuotaTariff(Mockito.any(QuotaTariffVO.class));

        quotaResponseBuilderImplSpy.persistNewQuotaTariff(quotaTariffVoMock, "", 1, date, 1l, date, 1.0, "", "");

        Mockito.verify(quotaTariffDaoMock).addQuotaTariff(Mockito.any(QuotaTariffVO.class));
    }

    @Test (expected = ServerApiException.class)
    public void deleteQuotaTariffTestQuotaDoesNotExistThrowsServerApiException() {
        Mockito.doReturn(null).when(quotaTariffDaoMock).findById(Mockito.anyLong());
        quotaResponseBuilderImplSpy.deleteQuotaTariff("");
    }

    @Test
    public void deleteQuotaTariffTestUpdateRemoved() {
        Mockito.doReturn(quotaTariffVoMock).when(quotaTariffDaoMock).findByUuid(Mockito.anyString());
        Mockito.doReturn(true).when(quotaTariffDaoMock).updateQuotaTariff(Mockito.any(QuotaTariffVO.class));
        Mockito.doReturn(new Date()).when(quotaServiceMock).computeAdjustedTime(Mockito.any(Date.class));

        Assert.assertTrue(quotaResponseBuilderImplSpy.deleteQuotaTariff(""));

        Mockito.verify(quotaTariffVoMock).setRemoved(Mockito.any(Date.class));
    }

    @Test
    public void createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformedTestUsageTypeDifferentFromNullDoNothing() {
        List<QuotaUsageVO> listUsage = new ArrayList<>();

        quotaResponseBuilderImplSpy.createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformed(listUsage, 1);

        Assert.assertTrue(listUsage.isEmpty());
    }

    @Test
    public void createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformedTestUsageTypeIsNullAddDummyForAllQuotaTypes() {
        List<QuotaUsageVO> listUsage = new ArrayList<>();
        listUsage.add(new QuotaUsageVO());

        quotaResponseBuilderImplSpy.createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformed(listUsage, null);

        Assert.assertEquals(QuotaTypes.listQuotaTypes().size() + 1, listUsage.size());

        QuotaTypes.listQuotaTypes().entrySet().forEach(entry -> {
            Assert.assertTrue(listUsage.stream().anyMatch(usage -> usage.getUsageType() == entry.getKey() && usage.getQuotaUsed().equals(BigDecimal.ZERO)));
        });
    }

    private List<QuotaUsageVO> getQuotaUsagesForTest() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        List<QuotaUsageVO> quotaUsages = new ArrayList<>();

        QuotaUsageVO quotaUsage = new QuotaUsageVO();
        quotaUsage.setAccountId(1l);
        quotaUsage.setDomainId(2l);
        quotaUsage.setUsageType(3);
        quotaUsage.setQuotaUsed(BigDecimal.valueOf(10).setScale(2, RoundingMode.HALF_EVEN));
        try {
            quotaUsage.setStartDate(sdf.parse("2022-01-01"));
            quotaUsage.setEndDate(sdf.parse("2022-01-02"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        quotaUsages.add(quotaUsage);

        quotaUsage = new QuotaUsageVO();
        quotaUsage.setAccountId(4l);
        quotaUsage.setDomainId(5l);
        quotaUsage.setUsageType(3);
        quotaUsage.setQuotaUsed(null);
        try {
            quotaUsage.setStartDate(sdf.parse("2022-01-03"));
            quotaUsage.setEndDate(sdf.parse("2022-01-04"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        quotaUsages.add(quotaUsage);

        quotaUsage = new QuotaUsageVO();
        quotaUsage.setAccountId(6l);
        quotaUsage.setDomainId(7l);
        quotaUsage.setUsageType(3);
        quotaUsage.setQuotaUsed(BigDecimal.valueOf(5).setScale(2, RoundingMode.HALF_EVEN));
        try {
            quotaUsage.setStartDate(sdf.parse("2022-01-05"));
            quotaUsage.setEndDate(sdf.parse("2022-01-06"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        quotaUsages.add(quotaUsage);

        return quotaUsages;
    }

    @Test
    public void createStatementItemTestReturnItem() {
        List<QuotaUsageVO> quotaUsages = getQuotaUsagesForTest();
        Mockito.doNothing().when(quotaResponseBuilderImplSpy).setStatementItemDetails(Mockito.any(), Mockito.any(), Mockito.anyBoolean());

        QuotaStatementItemResponse result = quotaResponseBuilderImplSpy.createStatementItem(quotaUsages, false);

        QuotaUsageVO expected = quotaUsages.get(0);
        QuotaTypes quotaTypeExpected = QuotaTypes.listQuotaTypes().get(expected.getUsageType());
        Assert.assertEquals(BigDecimal.valueOf(15).setScale(2, RoundingMode.HALF_EVEN), result.getQuotaUsed());
        Assert.assertEquals(expected.getAccountId(), result.getAccountId());
        Assert.assertEquals(expected.getDomainId(), result.getDomainId());
        Assert.assertEquals(quotaTypeExpected.getQuotaUnit(), result.getUsageUnit());
        Assert.assertEquals(quotaTypeExpected.getQuotaName(), result.getUsageName());
    }

    @Test
    public void setStatementItemDetailsTestDoNotShowDetailsDoNothing() {
        QuotaStatementItemResponse item = new QuotaStatementItemResponse(1);

        quotaResponseBuilderImplSpy.setStatementItemDetails(item, getQuotaUsagesForTest(), false);

        Assert.assertNull(item.getDetails());
    }

    @Test
    public void setStatementItemDetailsTestShowDetailsAddDetailsToItem() {
        QuotaStatementItemResponse item = new QuotaStatementItemResponse(1);
        List<QuotaUsageVO> expecteds = getQuotaUsagesForTest();

        Mockito.doNothing().when(quotaResponseBuilderImplSpy).addResourceIdToItemDetail(Mockito.any(), Mockito.any());

        quotaResponseBuilderImplSpy.setStatementItemDetails(item, expecteds, true);

        expecteds = expecteds.stream().filter(detail -> detail.getQuotaUsed() != null && detail.getQuotaUsed().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());
        for (int i = 0; i < expecteds.size(); i++) {
            QuotaUsageVO expected = expecteds.get(i);
            QuotaStatementItemDetailResponse result = item.getDetails().get(i);

            Assert.assertEquals(expected.getAccountId(), result.getAccountId());
            Assert.assertEquals(expected.getDomainId(), result.getDomainId());
            Assert.assertEquals(expected.getQuotaUsed(), result.getQuotaUsed());
            Assert.assertEquals(expected.getStartDate(), result.getStartDate());
            Assert.assertEquals(expected.getEndDate(), result.getEndDate());
        }
    }

    @Test
    public void addResourceIdToItemDetailTestQuotaTypeIsNetworkOfferingAddOfferingId() {
        UsageVO expected = new UsageVO();
        expected.setUsageId(1l);
        expected.setOfferingId(2l);

        Mockito.doReturn(expected).when(usageDaoMock).findUsageById(Mockito.anyLong());

        QuotaStatementItemDetailResponse detail = new QuotaStatementItemDetailResponse();

        QuotaTypes.listQuotaTypes().entrySet().forEach(entry -> {
            expected.setUsageType(entry.getKey());

            quotaResponseBuilderImplSpy.addResourceIdToItemDetail(1l, detail);

            if (entry.getKey() == QuotaTypes.NETWORK_OFFERING) {
                Assert.assertEquals(expected.getOfferingId(), detail.getResourceId());
            } else {
                Assert.assertEquals(expected.getUsageId(), detail.getResourceId());
            }
        });

    }

    @Test (expected = InvalidParameterValueException.class)
    public void createQuotaBalanceResponseTestNullQuotaBalancesThrowsInvalidParameterValueException() {
        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getQuotaBalance(Mockito.any());
        quotaResponseBuilderImplSpy.createQuotaBalanceResponse(null);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void createQuotaBalanceResponseTestEmptyQuotaBalancesThrowsInvalidParameterValueException() {
        Mockito.doReturn(new ArrayList<>()).when(quotaResponseBuilderImplSpy).getQuotaBalance(Mockito.any());
        quotaResponseBuilderImplSpy.createQuotaBalanceResponse(null);
    }

    private List<QuotaBalanceVO> getQuotaBalancesForTest() {
        List<QuotaBalanceVO> balances = new ArrayList<>();

        QuotaBalanceVO balance = new QuotaBalanceVO();
        balance.setUpdatedOn(new Date());
        balance.setCreditBalance(new BigDecimal(-10.42));
        balances.add(balance);

        balance = new QuotaBalanceVO();
        balance.setUpdatedOn(new Date());
        balance.setCreditBalance(new BigDecimal(-18.94));
        balances.add(balance);

        balance = new QuotaBalanceVO();
        balance.setUpdatedOn(new Date());
        balance.setCreditBalance(new BigDecimal(-29.37));
        balances.add(balance);

        return balances;
    }

    @Test
    public void createQuotaBalancesResponseTestCreateResponse() {
        List<QuotaBalanceVO> balances = getQuotaBalancesForTest();

        QuotaBalanceResponse expected = new QuotaBalanceResponse();
        expected.setObjectName("balance");
        expected.setCurrency("$");

        Mockito.doReturn(balances).when(quotaResponseBuilderImplSpy).getQuotaBalance(Mockito.any());
        QuotaBalanceResponse result = quotaResponseBuilderImplSpy.createQuotaBalanceResponse(new QuotaBalanceCmd());

        Assert.assertEquals(expected.getCurrency(), result.getCurrency());
        Assert.assertEquals(expected.getObjectName(), result.getObjectName());

        for (int i = 0; i < balances.size(); i++) {
            Assert.assertEquals(balances.get(i).getUpdatedOn(), result.getDailyBalances().get(i).getDate());
            Assert.assertEquals(balances.get(i).getCreditBalance().setScale(2, RoundingMode.HALF_EVEN), result.getDailyBalances().get(i).getBalance());
        }
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void createQuotaSummaryResponseTestNotListAllAndAllAccountTypesReturnsSingleRecord() {
        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setListAll(false);

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();

        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());

        accountTypes.forEach(type -> {
            Mockito.doReturn(type).when(accountMock).getType();

            Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderImplSpy.createQuotaSummaryResponse(cmd);
            Assert.assertEquals(quotaSummaryResponseMock1, result);
        });

        Mockito.verify(quotaResponseBuilderImplSpy, Mockito.times(accountTypes.size())).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void createQuotaSummaryResponseTestListAllAndAccountTypesAdminReturnsAllAndTheRestReturnsSingleRecord() {
        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setListAll(true);

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();

        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock2).when(quotaResponseBuilderImplSpy).getQuotaSummaryResponseWithListAll(Mockito.any(), Mockito.any());

        Set<Short> accountTypesThatCanListAllQuotaSummaries = Sets.newHashSet(Account.ACCOUNT_TYPE_ADMIN, Account.ACCOUNT_TYPE_DOMAIN_ADMIN);

        accountTypes.forEach(type -> {
            Mockito.doReturn(type).when(accountMock).getType();

            Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderImplSpy.createQuotaSummaryResponse(cmd);

            if (accountTypesThatCanListAllQuotaSummaries.contains(type)) {
                Assert.assertEquals(quotaSummaryResponseMock2, result);
            } else {
                Assert.assertEquals(quotaSummaryResponseMock1, result);
            }
        });

        Mockito.verify(quotaResponseBuilderImplSpy, Mockito.times(accountTypes.size() - accountTypesThatCanListAllQuotaSummaries.size())).getQuotaSummaryResponse(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.verify(quotaResponseBuilderImplSpy, Mockito.times(accountTypesThatCanListAllQuotaSummaries.size())).getQuotaSummaryResponseWithListAll(Mockito.any(), Mockito.any());
    }

    @Test
    public void getDomainPathByDomainIdForDomainAdminTestAccountNotDomainAdminReturnsNull() {
        Collection<Short> accountTypesWithoutDomainAdmin = CollectionUtils.removeAll(accountTypes, Arrays.asList(Account.ACCOUNT_TYPE_DOMAIN_ADMIN));

        accountTypesWithoutDomainAdmin.forEach(type -> {
            Mockito.doReturn(type).when(accountMock).getType();
            Assert.assertNull(quotaResponseBuilderImplSpy.getDomainPathByDomainIdForDomainAdmin(accountMock));
        });
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getDomainPathByDomainIdForDomainAdminTestDomainFromCallerIsNullThrowsInvalidParameterValueException() {
        Mockito.doReturn(Account.ACCOUNT_TYPE_DOMAIN_ADMIN).when(accountMock).getType();
        Mockito.doReturn(null).when(domainDaoMock).findById(Mockito.anyLong());
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));

        quotaResponseBuilderImplSpy.getDomainPathByDomainIdForDomainAdmin(accountMock);
    }

    @Test
    public void getDomainPathByDomainIdForDomainAdminTestDomainFromCallerIsNotNullReturnsPath() {
        String expected = "/test/";

        Mockito.doReturn(Account.ACCOUNT_TYPE_DOMAIN_ADMIN).when(accountMock).getType();
        Mockito.doReturn(domainVoMock).when(domainDaoMock).findById(Mockito.anyLong());
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
        Mockito.doReturn(expected).when(domainVoMock).getPath();

        String result = quotaResponseBuilderImplSpy.getDomainPathByDomainIdForDomainAdmin(accountMock);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getAccountIdByAccountNameTestAccountNameIsNullReturnsNull() {
        Assert.assertNull(quotaResponseBuilderImplSpy.getAccountIdByAccountName(null, 1l, accountMock));
    }

    @Test
    public void getAccountIdByAccountNameTestDomainIdIsNullReturnsNull() {
        Assert.assertNull(quotaResponseBuilderImplSpy.getAccountIdByAccountName("test", null, accountMock));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getAccountIdByAccountNameTestAccountIsNullThrowsInvalidParameterValueException() {
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
        Mockito.doReturn(null).when(accountDaoMock).findAccountIncludingRemoved(Mockito.anyString(), Mockito.anyLong());

        quotaResponseBuilderImplSpy.getAccountIdByAccountName("test", 1l, accountMock);
    }

    @Test
    public void getAccountIdByAccountNameTestAccountIsNotNullReturnsAccountId() {
        Long expected = 61l;

        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
        Mockito.doReturn(accountMock).when(accountDaoMock).findAccountIncludingRemoved(Mockito.anyString(), Mockito.anyLong());
        Mockito.doReturn(expected).when(accountMock).getAccountId();

        Long result = quotaResponseBuilderImplSpy.getAccountIdByAccountName("test", 1l, accountMock);

        Assert.assertEquals(expected, result);
    }

    public void getQuotaSummaryResponseWithListAllTestAccountNameIsNotNullAndDomainIdIsNullGetsDomainIdFromCaller() {
        Long expectedDomainId = 78l;

        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName("test");
        cmd.setDomainId(null);

        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getAccountIdByAccountName(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(expectedDomainId).when(accountMock).getDomainId();
        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderImplSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    public void getQuotaSummaryResponseWithListAllTestAccountNameAndDomainIdAreNullPassDomainIdAsNull() {
        Long expectedDomainId = null;

        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName(null);
        cmd.setDomainId(null);

        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getAccountIdByAccountName(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderImplSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    public void getQuotaSummaryResponseWithListAllTestAccountNameIsNullAndDomainIdIsNotNullPassDomainId() {
        Long expectedDomainId = 26l;

        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName(null);
        cmd.setDomainId(expectedDomainId);

        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getAccountIdByAccountName(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderImplSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    public void getQuotaSummaryResponseWithListAllTestAccountNameAndDomainIdAreNotNullPassDomainId() {
        Long expectedDomainId = 9837l;

        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName("test");
        cmd.setDomainId(expectedDomainId);

        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getAccountIdByAccountName(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderImplSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }
}
