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

import java.lang.reflect.Type;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.InternalServerErrorException;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.serializer.GsonHelper;
import com.cloud.user.User;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaConfigureEmailCmd;
import org.apache.cloudstack.api.command.QuotaCreditsListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaSummaryCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.discovery.ApiDiscoveryService;
import org.apache.cloudstack.quota.QuotaManager;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.cloudstack.quota.activationrule.presetvariables.GenericPresetVariable;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariableHelper;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariables;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.quota.vo.ResourcesQuotingResultResponse;
import org.apache.cloudstack.quota.vo.ResourcesToQuoteVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Stubber;
import org.mockito.verification.VerificationMode;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.net.Ip;

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

    @Mock
    ApiDiscoveryService apiDiscoveryServiceMock;

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

    @Mock
    Map<Long, AccountVO> mapAccountMock;

    @Mock
    QuotaCreditsVO quotaCreditsVoMock;

    @Mock
    AccountVO accountVoMock;

    @Mock
    QuotaStatementItemDetailResourceResponse quotaStatementItemDetailResourceResponseMock;

    @Mock
    IPAddressDao ipAddressDaoMock;

    @Mock
    UsageVO usageVoMock;

    @Mock
    IPAddressVO ipAddressVoMock;

    @Mock
    PresetVariableHelper presetVariableHelperMock;

    @Mock
    LinkedList<ResourcesToQuoteVo> linkedListResourcesToQuoteVoMock;

    @Mock
    QuotaManager quotaManagerMock;

    @Mock
    User userMock;

    @Mock
    QuotaConfigureEmailCmd quotaConfigureEmailCmdMock;

    @Mock
    QuotaAccountDao quotaAccountDaoMock;

    @Mock
    QuotaAccountVO quotaAccountVOMock;

    LinkedList<ResourcesToQuoteVo> linkedListResourcesToQuoteVo = new LinkedList<>(Arrays.asList(new ResourcesToQuoteVo(), new ResourcesToQuoteVo(), new ResourcesToQuoteVo()));

    Date date = new Date();
    Set<Account.Type> accountTypes = Sets.newHashSet(Account.Type.values());

    AccountVO accountVo = new AccountVO();
    DomainVO domainVo = new DomainVO();

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
        QuotaTariffResponse response = quotaResponseBuilderImplSpy.createQuotaTariffResponse(tariffVO, true);
        assertTrue(tariffVO.getUsageType() == response.getUsageType());
        assertTrue(tariffVO.getCurrencyValue().equals(response.getTariffValue()));
    }

    @Test
    public void createQuotaTariffResponseTestIfReturnsActivationRuleWithPermission() {
        QuotaTariffVO tariffVO = makeTariffTestData();
        tariffVO.setActivationRule("a = 10;");
        QuotaTariffResponse response = quotaResponseBuilderImplSpy.createQuotaTariffResponse(tariffVO, true);
        assertEquals("a = 10;", response.getActivationRule());
    }

    @Test
    public void createQuotaTariffResponseTestIfReturnsActivationRuleWithoutPermission() {
        QuotaTariffVO tariffVO = makeTariffTestData();
        tariffVO.setActivationRule("a = 10;");
        QuotaTariffResponse response = quotaResponseBuilderImplSpy.createQuotaTariffResponse(tariffVO, false);
        assertNull(response.getActivationRule());
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
        account.setState(Account.State.LOCKED);
        Mockito.when(accountDaoMock.findById(Mockito.anyLong())).thenReturn(account);

        QuotaCreditsResponse resp = quotaResponseBuilderImplSpy.addQuotaCredits(accountId, domainId, amount, updatedBy, true);
        assertTrue(resp.getCredit().compareTo(credit.getCredit()) == 0);
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
        Mockito.doReturn(accountVoMock).when(accountDaoMock).findByIdIncludingRemoved(Mockito.any());
        Mockito.doReturn(domainVoMock).when(domainDaoMock).findByIdIncludingRemoved(Mockito.any());

        String testAccountUuid = "test-account-uuid";
        String testDomainUuid = "test-domain-uuid";

        Mockito.doReturn(testAccountUuid).when(accountVoMock).getUuid();
        Mockito.doReturn(testDomainUuid).when(domainVoMock).getUuid();

        QuotaStatementItemResponse result = quotaResponseBuilderImplSpy.createStatementItem(quotaUsages, false);

        QuotaUsageVO expected = quotaUsages.get(0);
        QuotaTypes quotaTypeExpected = QuotaTypes.listQuotaTypes().get(expected.getUsageType());
        Assert.assertEquals(BigDecimal.valueOf(15).setScale(2, RoundingMode.HALF_EVEN), result.getQuotaUsed());
        Assert.assertEquals(testAccountUuid, result.getAccountUuid());
        Assert.assertEquals(testDomainUuid, result.getDomainUuid());
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

        Mockito.doReturn(quotaStatementItemDetailResourceResponseMock).when(quotaResponseBuilderImplSpy).getQuotaStatementItemDetailResourceResponse(Mockito.any());

        Mockito.doReturn(accountVoMock).when(accountDaoMock).findByIdIncludingRemoved(Mockito.any());
        Mockito.doReturn(domainVoMock).when(domainDaoMock).findByIdIncludingRemoved(Mockito.any());

        String testAccountUuid = "test-account-uuid";
        String testDomainUuid = "test-domain-uuid";

        Mockito.doReturn(testAccountUuid).when(accountVoMock).getUuid();
        Mockito.doReturn(testDomainUuid).when(domainVoMock).getUuid();

        quotaResponseBuilderImplSpy.setStatementItemDetails(item, expecteds, true);

        expecteds = expecteds.stream().filter(detail -> detail.getQuotaUsed() != null && detail.getQuotaUsed().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());
        for (int i = 0; i < expecteds.size(); i++) {
            QuotaUsageVO expected = expecteds.get(i);
            QuotaStatementItemDetailResponse result = item.getDetails().get(i);

            Assert.assertEquals(testAccountUuid, result.getAccountUuid());
            Assert.assertEquals(testDomainUuid, result.getDomainUuid());
            Assert.assertEquals(expected.getQuotaUsed(), result.getQuotaUsed());
            Assert.assertEquals(expected.getStartDate(), result.getStartDate());
            Assert.assertEquals(expected.getEndDate(), result.getEndDate());
            Assert.assertEquals(quotaStatementItemDetailResourceResponseMock, result.getResource());
        }
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

        Set<Account.Type> accountTypesThatCanListAllQuotaSummaries = Sets.newHashSet(Account.Type.ADMIN, Account.Type.DOMAIN_ADMIN);

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
        Collection<Account.Type> accountTypesWithoutDomainAdmin = CollectionUtils.removeAll(accountTypes, Arrays.asList(Account.Type.DOMAIN_ADMIN));

        accountTypesWithoutDomainAdmin.forEach(type -> {
            Mockito.doReturn(type).when(accountMock).getType();
            Assert.assertNull(quotaResponseBuilderImplSpy.getDomainPathByDomainIdForDomainAdmin(accountMock));
        });
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getDomainPathByDomainIdForDomainAdminTestDomainFromCallerIsNullThrowsInvalidParameterValueException() {
        Mockito.doReturn(Account.Type.DOMAIN_ADMIN).when(accountMock).getType();
        Mockito.doReturn(null).when(domainDaoMock).findById(Mockito.anyLong());
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));

        quotaResponseBuilderImplSpy.getDomainPathByDomainIdForDomainAdmin(accountMock);
    }

    @Test
    public void getDomainPathByDomainIdForDomainAdminTestDomainFromCallerIsNotNullReturnsPath() {
        String expected = "/test/";

        Mockito.doReturn(Account.Type.DOMAIN_ADMIN).when(accountMock).getType();
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

    @Test
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

    @Test
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

    @Test
    public void getQuotaSummaryResponseWithListAllTestAccountNameIsNullAndDomainIdIsNotNullPassDomainId() {
        Long expectedDomainId = 26l;

        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName(null);
        cmd.setDomainId("test");

        Mockito.doReturn(domainVoMock).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doReturn(expectedDomainId).when(domainVoMock).getId();
        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getAccountIdByAccountName(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderImplSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getQuotaSummaryResponseWithListAllTestAccountNameIsNullAndDomainIdIsNotNullButDomainDoesNotExistThrowInvalidParameterValueException() {
        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName(null);
        cmd.setDomainId("test");

        Mockito.doReturn(null).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        quotaResponseBuilderImplSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);
    }

    @Test
    public void getQuotaSummaryResponseWithListAllTestAccountNameAndDomainIdAreNotNullPassDomainId() {
        Long expectedDomainId = 9837l;

        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName("test");
        cmd.setDomainId("test");

        Mockito.doReturn(domainVoMock).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doReturn(expectedDomainId).when(domainVoMock).getId();
        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getAccountIdByAccountName(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderImplSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderImplSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void getAccountByIdTestMapHasAccountReturnIt() {
        Mockito.doReturn(1l).when(quotaCreditsVoMock).getUpdatedBy();
        Mockito.doReturn(accountVoMock).when(mapAccountMock).get(Mockito.any());
        AccountVO result = quotaResponseBuilderImplSpy.getAccountById(quotaCreditsVoMock, mapAccountMock);

        Assert.assertEquals(accountVoMock, result);
    }

    @Test(expected = InternalServerErrorException.class)
    public void getAccountByIdTestFindByIdInlcudingRemovedReturnsNullThrowInternalServerErrorException() {
        Mockito.doReturn(1l).when(quotaCreditsVoMock).getUpdatedBy();
        Mockito.doReturn(null).when(mapAccountMock).get(Mockito.any());
        Mockito.doReturn(null).when(accountDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        quotaResponseBuilderImplSpy.getAccountById(quotaCreditsVoMock, mapAccountMock);
    }

    @Test
    public void getAccountByIdTestFindByIdInlcudingRemovedReturnsAccountAddToMapAndReturnIt() {
        Map<Long, AccountVO> mapAccount = new HashMap<>();

        long updatedBy = 1l;
        Mockito.doReturn(updatedBy).when(quotaCreditsVoMock).getUpdatedBy();
        Mockito.doReturn(null).when(mapAccountMock).get(Mockito.any());
        Mockito.doReturn(accountVoMock).when(accountDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        AccountVO result = quotaResponseBuilderImplSpy.getAccountById(quotaCreditsVoMock, mapAccount);

        Assert.assertEquals(accountVoMock, result);
        Assert.assertEquals(accountVoMock, mapAccount.get(updatedBy));
    }

    @Test
    public void getQuotaCreditsResponseTestReturnsObject() {
        QuotaCreditsResponse expected = new QuotaCreditsResponse();

        expected.setAccountCreditorId("test_uuid");
        expected.setAccountCreditorName("test_name");
        expected.setCredit(new BigDecimal(41.5));
        expected.setCreditedOn(new Date());
        expected.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        expected.setObjectName("credit");

        Mockito.doReturn(accountVoMock).when(quotaResponseBuilderImplSpy).getAccountById(Mockito.any(), Mockito.any());
        Mockito.doReturn(expected.getAccountCreditorId()).when(accountVoMock).getUuid();
        Mockito.doReturn(expected.getAccountCreditorName()).when(accountVoMock).getAccountName();
        Mockito.doReturn(expected.getCredit()).when(quotaCreditsVoMock).getCredit();
        Mockito.doReturn(expected.getCreditedOn()).when(quotaCreditsVoMock).getUpdatedOn();

        QuotaCreditsResponse result = quotaResponseBuilderImplSpy.getQuotaCreditsResponse(mapAccountMock, quotaCreditsVoMock);

        Assert.assertEquals(expected.getAccountCreditorId(), result.getAccountCreditorId());
        Assert.assertEquals(expected.getAccountCreditorName(), result.getAccountCreditorName());
        Assert.assertEquals(expected.getCredit(), result.getCredit());
        Assert.assertEquals(expected.getCreditedOn(), result.getCreditedOn());
        Assert.assertEquals(expected.getCurrency(), result.getCurrency());
        Assert.assertEquals(expected.getObjectName(), result.getObjectName());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getCreditsForQuotaCreditsListTestStartDateIsAfterEndDateThrowsInvalidParameterValueException() {
        QuotaCreditsListCmd cmd = getQuotaCreditsListCmdForTests();
        cmd.setStartDate(new Date());
        cmd.setEndDate(DateUtils.addDays(new Date(), -1));

        quotaResponseBuilderImplSpy.getCreditsForQuotaCreditsList(cmd);
    }

    @Test
    public void getCreditsForQuotaCreditsListTestFindCreditsReturnsData() {
        List<QuotaCreditsVO> expected = new ArrayList<>();
        expected.add(new QuotaCreditsVO());

        QuotaCreditsListCmd cmd = getQuotaCreditsListCmdForTests();

        Mockito.doReturn(expected).when(quotaCreditsDaoMock).findCredits(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());
        List<QuotaCreditsVO> result = quotaResponseBuilderImplSpy.getCreditsForQuotaCreditsList(cmd);

        Assert.assertEquals(expected, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getCreditsForQuotaCreditsListTestFindCreditsReturnsEmptyThrowsInvalidParameterValueException() {
        QuotaCreditsListCmd cmd = getQuotaCreditsListCmdForTests();

        Mockito.doReturn(new ArrayList<>()).when(quotaCreditsDaoMock).findCredits(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());
        quotaResponseBuilderImplSpy.getCreditsForQuotaCreditsList(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getCreditsForQuotaCreditsListTestFindCreditsReturnsNullThrowsInvalidParameterValueException() {
        QuotaCreditsListCmd cmd = getQuotaCreditsListCmdForTests();

        Mockito.doReturn(null).when(quotaCreditsDaoMock).findCredits(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());
        quotaResponseBuilderImplSpy.getCreditsForQuotaCreditsList(cmd);
    }

    protected QuotaCreditsListCmd getQuotaCreditsListCmdForTests() {
        QuotaCreditsListCmd cmd = new QuotaCreditsListCmd();
        cmd.setAccountId(1l);
        cmd.setDomainId(2l);
        return cmd;
    }

    @Test
    public void getQuotaStatementItemDetailResourceResponseTestTypeIpAddressSearchOnDatabase() {
        QuotaStatementItemDetailResourceResponse expected = new QuotaStatementItemDetailResourceResponse("test_id", "test_name", false);

        Mockito.doReturn(usageVoMock).when(usageDaoMock).findUsageById(Mockito.anyLong());
        Mockito.doReturn(QuotaTypes.IP_ADDRESS).when(usageVoMock).getUsageType();
        Mockito.doReturn(1l).when(usageVoMock).getUsageId();
        Mockito.doReturn(ipAddressVoMock).when(ipAddressDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        Mockito.doReturn(expected.getId()).when(ipAddressVoMock).getUuid();
        Mockito.doReturn(null).when(ipAddressVoMock).getRemoved();

        Ip ipMock = Mockito.mock(Ip.class);
        Mockito.doReturn(ipMock).when(ipAddressVoMock).getAddress();
        Mockito.doReturn(expected.getDisplayName()).when(ipMock).addr();

        QuotaStatementItemDetailResourceResponse result = quotaResponseBuilderImplSpy.getQuotaStatementItemDetailResourceResponse(2l);

        Assert.assertEquals(expected.getId(), result.getId());
        Assert.assertEquals(expected.getDisplayName(), result.getDisplayName());
        Assert.assertEquals(expected.isRemoved(), result.isRemoved());
        Mockito.verify(presetVariableHelperMock, Mockito.never()).getResourceToAddToQuotaStatementResponse(Mockito.any());
    }

    @Test
    public void getQuotaStatementItemDetailResourceResponseTestTypeAlreadyLoadedByPresetVariableHelper() {
        QuotaStatementItemDetailResourceResponse expected = new QuotaStatementItemDetailResourceResponse("test_id2", "test_name3", true);

        Mockito.doReturn(usageVoMock).when(usageDaoMock).findUsageById(Mockito.anyLong());

        GenericPresetVariable genericPresetVariableMock = Mockito.mock(GenericPresetVariable.class);
        Mockito.doReturn(genericPresetVariableMock).when(presetVariableHelperMock).getResourceToAddToQuotaStatementResponse(Mockito.any());
        Mockito.doReturn(expected.getDisplayName()).when(genericPresetVariableMock).getName();
        Mockito.doReturn(expected.isRemoved()).when(genericPresetVariableMock).isRemoved();

        Mockito.doReturn(genericPresetVariableMock).when(presetVariableHelperMock).getResourceToAddToQuotaStatementResponse(Mockito.any());

        Set<Integer> quotaTypes = new HashSet<>(QuotaTypes.getQuotaTypeMap().keySet());
        quotaTypes.remove(QuotaTypes.IP_ADDRESS);

        Set<Integer> quotaTypesLoadedInPresetVariableHelper = Sets.newHashSet(QuotaTypes.RUNNING_VM, QuotaTypes.ALLOCATED_VM, QuotaTypes.VOLUME, QuotaTypes.NETWORK_OFFERING,
                QuotaTypes.SNAPSHOT, QuotaTypes.TEMPLATE, QuotaTypes.ISO, QuotaTypes.VM_SNAPSHOT);

        quotaTypes.forEach(type -> {
            Mockito.doReturn(type).when(usageVoMock).getUsageType();

            if (!quotaTypesLoadedInPresetVariableHelper.contains(type)) {
                Mockito.doReturn(null).when(genericPresetVariableMock).getId();
                QuotaStatementItemDetailResourceResponse result = quotaResponseBuilderImplSpy.getQuotaStatementItemDetailResourceResponse(4l);
                Assert.assertNull(result);
                return;
            }

            Mockito.doReturn(expected.getId()).when(genericPresetVariableMock).getId();
            QuotaStatementItemDetailResourceResponse result = quotaResponseBuilderImplSpy.getQuotaStatementItemDetailResourceResponse(2l);

            Assert.assertEquals(expected.getId(), result.getId());
            Assert.assertEquals(expected.getDisplayName(), result.getDisplayName());
            Assert.assertEquals(expected.isRemoved(), result.isRemoved());
        });

        Mockito.verify(presetVariableHelperMock, Mockito.times(quotaTypes.size())).getResourceToAddToQuotaStatementResponse(Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    @PrepareForTest({GsonHelper.class, Gson.class})
    public void quoteResourcesTestThrowJsonSyntaxExceptionWhenPassingAnInvalidJsonAsParameter() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Mockito.doThrow(JsonSyntaxException.class).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));

        quotaResponseBuilderImplSpy.quoteResources("");
    }

    @Test(expected = InvalidParameterValueException.class)
    @PrepareForTest({GsonHelper.class, Gson.class})
    public void quoteResourcesTestThrowInvalidParameterValueExceptionWhenResourcesToQuoteIsNull() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Mockito.doReturn(null).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));

        quotaResponseBuilderImplSpy.quoteResources("");
    }

    @Test(expected = InvalidParameterValueException.class)
    @PrepareForTest({GsonHelper.class, Gson.class})
    public void quoteResourcesTestThrowInvalidParameterValueExceptionWhenResourcesToQuoteIsEmpty() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Mockito.doReturn(null).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));

        quotaResponseBuilderImplSpy.quoteResources("");
    }

    @Test(expected = InvalidParameterValueException.class)
    @PrepareForTest({GsonHelper.class, Gson.class})
    public void quoteResourcesTestThrowInvalidParameterValueExceptionWhenPassingAnInvalidArgumentInTheJson() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Mockito.doReturn(linkedListResourcesToQuoteVo).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));
        Mockito.doThrow(InvalidParameterValueException.class).when(quotaResponseBuilderImplSpy).validateResourcesToQuoteFieldsAndReturnUsageTypes(Mockito.any());

        quotaResponseBuilderImplSpy.quoteResources("");
    }

    @Test(expected = PermissionDeniedException.class)
    @PrepareForTest({GsonHelper.class, Gson.class})
    public void quoteResourcesTestThrowPermissionDeniedExceptionWhenCallerDoesNotHaveAccessToAccountsOrDomainsPassedAsParameter() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Set<Integer> setIntegerMock = Mockito.mock(Set.class);

        Mockito.doReturn(linkedListResourcesToQuoteVo).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));
        Mockito.doReturn(setIntegerMock).when(quotaResponseBuilderImplSpy).validateResourcesToQuoteFieldsAndReturnUsageTypes(Mockito.any());
        Mockito.doThrow(PermissionDeniedException.class).when(quotaResponseBuilderImplSpy).validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(Mockito.any());

        quotaResponseBuilderImplSpy.quoteResources("");
    }

    @Test
    @PrepareForTest({GsonHelper.class, Gson.class})
    public void quoteResourcesTestReturnListOfResourcesQuotingResultResponse() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Set<Integer> setIntegerMock = Mockito.mock(Set.class);
        List<ResourcesQuotingResultResponse> expected = new ArrayList<>();

        Mockito.doReturn(linkedListResourcesToQuoteVoMock).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));
        Mockito.doReturn(setIntegerMock).when(quotaResponseBuilderImplSpy).validateResourcesToQuoteFieldsAndReturnUsageTypes(Mockito.any());
        Mockito.doNothing().when(quotaResponseBuilderImplSpy).validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(Mockito.any());
        Mockito.doReturn(expected).when(quotaManagerMock).quoteResources(Mockito.any(), Mockito.any());

        List<ResourcesQuotingResultResponse> result = quotaResponseBuilderImplSpy.quoteResources("");

        Assert.assertEquals(expected, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateResourcesToQuoteFieldsAndReturnUsageTypesTestThrowInvalidParameterValueExceptionWhenUsageTypeIsInvalid() {
        Mockito.doThrow(InvalidParameterValueException.class).when(quotaResponseBuilderImplSpy).validateResourceToQuoteUsageTypeAndReturnsItsId(Mockito.anyInt(), Mockito.any());
        quotaResponseBuilderImplSpy.validateResourcesToQuoteFieldsAndReturnUsageTypes(List.of(new ResourcesToQuoteVo()));
    }

    @Test
    public void validateResourcesToQuoteFieldsAndReturnUsageTypesTestReturnSetOfInteger() {
        List<ResourcesToQuoteVo> resourcesToQuoteVos = new ArrayList<>();
        QuotaTypes.listQuotaTypes().values().forEach(type -> {
            ResourcesToQuoteVo resourcesToQuoteVo = new ResourcesToQuoteVo();
            resourcesToQuoteVo.setUsageType(type.getQuotaName());
            resourcesToQuoteVos.add(resourcesToQuoteVo);
        });

        Set<Integer> expected = QuotaTypes.listQuotaTypes().values().stream().map(QuotaTypes::getQuotaType).collect(Collectors.toSet());
        Stubber stubber = null;

        for (Integer type : expected) {
            if (stubber == null) {
                stubber = Mockito.doReturn(type);
                continue;
            }

            stubber = stubber.doReturn(type);
        }

        stubber.when(quotaResponseBuilderImplSpy).validateResourceToQuoteUsageTypeAndReturnsItsId(Mockito.anyInt(), Mockito.any());
        Mockito.doNothing().when(quotaResponseBuilderImplSpy).addIdToResourceToQuoteIfNotSet(Mockito.anyInt(), Mockito.any());

        Set<Integer> result = quotaResponseBuilderImplSpy.validateResourcesToQuoteFieldsAndReturnUsageTypes(resourcesToQuoteVos);
        Assert.assertArrayEquals(expected.toArray(), result.toArray());
    }

    @Test
    public void addIdToResourceToQuoteIfNotSetTestIdIsSomethingThenDoNothing() {
        ResourcesToQuoteVo resourcesToQuoteVo = new ResourcesToQuoteVo();
        String expected = "test";
        resourcesToQuoteVo.setId(expected);

        quotaResponseBuilderImplSpy.addIdToResourceToQuoteIfNotSet(1, resourcesToQuoteVo);

        Assert.assertEquals(expected, resourcesToQuoteVo.getId());
    }

    @Test
    public void addIdToResourceToQuoteIfNotSetTestIdIsEmptyThenSetAsIndex() {
        ResourcesToQuoteVo resourcesToQuoteVo = new ResourcesToQuoteVo();
        String expected = "2";
        resourcesToQuoteVo.setId("");

        quotaResponseBuilderImplSpy.addIdToResourceToQuoteIfNotSet(Integer.parseInt(expected), resourcesToQuoteVo);

        Assert.assertEquals(expected, resourcesToQuoteVo.getId());
    }

    @Test
    public void addIdToResourceToQuoteIfNotSetTestIdIsWhitespaceThenSetAsIndex() {
        ResourcesToQuoteVo resourcesToQuoteVo = new ResourcesToQuoteVo();
        String expected = "3";
        resourcesToQuoteVo.setId("       ");

        quotaResponseBuilderImplSpy.addIdToResourceToQuoteIfNotSet(Integer.parseInt(expected), resourcesToQuoteVo);

        Assert.assertEquals(expected, resourcesToQuoteVo.getId());
    }

    @Test
    public void addIdToResourceToQuoteIfNotSetTestIdIsNullThenSetAsIndex() {
        ResourcesToQuoteVo resourcesToQuoteVo = new ResourcesToQuoteVo();
        String expected = "4";
        resourcesToQuoteVo.setId(null);

        quotaResponseBuilderImplSpy.addIdToResourceToQuoteIfNotSet(Integer.parseInt(expected), resourcesToQuoteVo);

        Assert.assertEquals(expected, resourcesToQuoteVo.getId());
    }

    @Test
    public void getPresetVariableIdIfItIsNotNullTestReturnTheIdWhenThePresetVariableIsNotNull() {
        String expected = "test";
        GenericPresetVariable gpv = new GenericPresetVariable();
        gpv.setId(expected);

        String result = quotaResponseBuilderImplSpy.getPresetVariableIdIfItIsNotNull(gpv);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getPresetVariableIdIfItIsNotNullTestReturnTheNullWhenThePresetVariableIsnull() {
        String result = quotaResponseBuilderImplSpy.getPresetVariableIdIfItIsNotNull(null);
        Assert.assertNull(result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateResourceToQuoteUsageTypeAndReturnsItsIdTestThrowInvalidParameterValueExceptionWhenUsageTypeIsNull() {
        quotaResponseBuilderImplSpy.validateResourceToQuoteUsageTypeAndReturnsItsId(0, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateResourceToQuoteUsageTypeAndReturnsItsIdTestThrowInvalidParameterValueExceptionWhenUsageTypeIsEmpty() {
        quotaResponseBuilderImplSpy.validateResourceToQuoteUsageTypeAndReturnsItsId(0, "");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateResourceToQuoteUsageTypeAndReturnsItsIdTestThrowInvalidParameterValueExceptionWhenUsageTypeIsWhitespace() {
        quotaResponseBuilderImplSpy.validateResourceToQuoteUsageTypeAndReturnsItsId(0, "   ");
    }

    @Test(expected = CloudRuntimeException.class)
    public void validateResourceToQuoteUsageTypeAndReturnsItsIdTestThrowCloudRuntimeExceptionWhenUsageTypeIsInvalid() {
        quotaResponseBuilderImplSpy.validateResourceToQuoteUsageTypeAndReturnsItsId(0, "anything");
    }

    @Test
    public void validateResourceToQuoteUsageTypeAndReturnsItsIdTestAllTypesReturnItsId() {
        QuotaTypes.listQuotaTypes().forEach((key, value) -> {
            int expected = key;
            int result = quotaResponseBuilderImplSpy.validateResourceToQuoteUsageTypeAndReturnsItsId(0, value.getQuotaName());
            Assert.assertEquals(expected, result);
        });
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadataTestDoNothingWhenMetadataIsNull() {
        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();
        quotaResponseBuilderImplSpy.validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(linkedListResourcesToQuoteVo);

        Mockito.verify(quotaResponseBuilderImplSpy, Mockito.never()).validateCallerAccessToAccountSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
        Mockito.verify(quotaResponseBuilderImplSpy, Mockito.never()).validateCallerAccessToDomainSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
    }

    @Test(expected = PermissionDeniedException.class)
    @PrepareForTest(CallContext.class)
    public void validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadataTestThrowPermissionDeniedExceptionOnAccountAccessValidation() {
        linkedListResourcesToQuoteVo.get(0).setMetadata(new PresetVariables());

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();
        Mockito.doThrow(PermissionDeniedException.class).when(quotaResponseBuilderImplSpy).validateCallerAccessToAccountSetInQuotingMetadata(Mockito.any(), Mockito.any(),
         Mockito.anyInt());
        quotaResponseBuilderImplSpy.validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(linkedListResourcesToQuoteVo);
    }

    @Test(expected = PermissionDeniedException.class)
    @PrepareForTest(CallContext.class)
    public void validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadataTestThrowPermissionDeniedExceptionOnDomainAccessValidation() {
        linkedListResourcesToQuoteVo.get(0).setMetadata(new PresetVariables());

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();
        Mockito.doNothing().when(quotaResponseBuilderImplSpy).validateCallerAccessToAccountSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
        Mockito.doThrow(PermissionDeniedException.class).when(quotaResponseBuilderImplSpy).validateCallerAccessToDomainSetInQuotingMetadata(Mockito.any(), Mockito.any(),
         Mockito.anyInt());
        quotaResponseBuilderImplSpy.validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(linkedListResourcesToQuoteVo);
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadataTestDoNothingWhenCallerHasAccessToAccountAndDomain() {
        linkedListResourcesToQuoteVo.get(0).setMetadata(new PresetVariables());
        linkedListResourcesToQuoteVo.get(2).setMetadata(new PresetVariables());

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();
        Mockito.doNothing().when(quotaResponseBuilderImplSpy).validateCallerAccessToAccountSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
        Mockito.doNothing().when(quotaResponseBuilderImplSpy).validateCallerAccessToDomainSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());

        quotaResponseBuilderImplSpy.validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(linkedListResourcesToQuoteVo);

        VerificationMode times = Mockito.times((int) linkedListResourcesToQuoteVo.stream().filter(item -> item.getMetadata() != null).count());
        Mockito.verify(quotaResponseBuilderImplSpy, times).validateCallerAccessToAccountSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
        Mockito.verify(quotaResponseBuilderImplSpy, times).validateCallerAccessToDomainSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
    }

    @Test
    public void validateCallerAccessToAccountSetInQuotingMetadataTestDoNothingWhenAccountIdIsNull() {
        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        quotaResponseBuilderImplSpy.validateCallerAccessToAccountSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test
    public void validateCallerAccessToAccountSetInQuotingMetadataTestDoNothingWhenAccountDoesNotExist() {
        Mockito.doReturn("something").when(quotaResponseBuilderImplSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(null).when(accountDaoMock).findByUuidIncludingRemoved(Mockito.anyString());

        quotaResponseBuilderImplSpy.validateCallerAccessToAccountSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test(expected = PermissionDeniedException.class)
    public void validateCallerAccessToAccountSetInQuotingMetadataTestThrowPermissionDeniedExceptionOnCheckAccess() {
        Mockito.doReturn("something").when(quotaResponseBuilderImplSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(accountVo).when(accountDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doThrow(PermissionDeniedException.class).when(accountManagerMock).checkAccess(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

        quotaResponseBuilderImplSpy.validateCallerAccessToAccountSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test
    public void validateCallerAccessToAccountSetInQuotingMetadataTestDoNothingWhenCallerHasAccessToAccount() {
        Mockito.doReturn("something").when(quotaResponseBuilderImplSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(accountVo).when(accountDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

        quotaResponseBuilderImplSpy.validateCallerAccessToAccountSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test
    public void validateCallerAccessToDomainSetInQuotingMetadataTestDoNothingWhenDomainIdIsNull() {
        Mockito.doReturn(null).when(quotaResponseBuilderImplSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        quotaResponseBuilderImplSpy.validateCallerAccessToDomainSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test
    public void validateCallerAccessToDomainSetInQuotingMetadataTestDoNothingWhenDomainDoesNotExist() {
        Mockito.doReturn("something").when(quotaResponseBuilderImplSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(null).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());

        quotaResponseBuilderImplSpy.validateCallerAccessToDomainSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test(expected = PermissionDeniedException.class)
    public void validateCallerAccessToDomainSetInQuotingMetadataThrowPermissionDeniedExceptionOnCheckAccess() {
        Mockito.doReturn("something").when(quotaResponseBuilderImplSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(domainVo).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doThrow(PermissionDeniedException.class).when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));

        quotaResponseBuilderImplSpy.validateCallerAccessToDomainSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test
    public void validateCallerAccessToDomainSetInQuotingMetadataDoNothingWhenCallerHasAccessToDomain() {
        Mockito.doReturn("something").when(quotaResponseBuilderImplSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(domainVo).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));

        quotaResponseBuilderImplSpy.validateCallerAccessToDomainSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test
    public void checkIfUserHasPermissionToSeeActivationRulesTestWithPermissionToCreateTariff() {
        ApiDiscoveryResponse response = new ApiDiscoveryResponse();
        response.setName("quotaTariffCreate");
        List<ApiDiscoveryResponse> cmdList = new ArrayList<>();
        cmdList.add(response);

        ListResponse<ApiDiscoveryResponse> responseList =  new ListResponse();
        responseList.setResponses(cmdList);

        Mockito.doReturn(responseList).when(apiDiscoveryServiceMock).listApis(userMock, null);

        assertTrue(quotaResponseBuilderImplSpy.isUserAllowedToSeeActivationRules(userMock));
    }

    @Test
    public void checkIfUserHasPermissionToSeeActivationRulesTestWithPermissionToUpdateTariff() {
        ApiDiscoveryResponse response = new ApiDiscoveryResponse();
        response.setName("quotaTariffUpdate");

        List<ApiDiscoveryResponse> cmdList = new ArrayList<>();
        cmdList.add(response);

        ListResponse<ApiDiscoveryResponse> responseList =  new ListResponse();
        responseList.setResponses(cmdList);

        Mockito.doReturn(responseList).when(apiDiscoveryServiceMock).listApis(userMock, null);

        assertTrue(quotaResponseBuilderImplSpy.isUserAllowedToSeeActivationRules(userMock));
    }

    @Test
    public void checkIfUserHasPermissionToSeeActivationRulesTestWithNoPermission() {
        ApiDiscoveryResponse response = new ApiDiscoveryResponse();
        response.setName("testCmd");

        List<ApiDiscoveryResponse> cmdList = new ArrayList<>();
        cmdList.add(response);

        ListResponse<ApiDiscoveryResponse> responseList =  new ListResponse();
        responseList.setResponses(cmdList);

        Mockito.doReturn(responseList).when(apiDiscoveryServiceMock).listApis(userMock, null);

        assertFalse(quotaResponseBuilderImplSpy.isUserAllowedToSeeActivationRules(userMock));
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateQuotaConfigureEmailCmdParametersTestNullQuotaAccount() {
        Mockito.doReturn(null).when(quotaAccountDaoMock).findByIdQuotaAccount(Mockito.any());
        quotaResponseBuilderImplSpy.validateQuotaConfigureEmailCmdParameters(quotaConfigureEmailCmdMock);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateQuotaConfigureEmailCmdParametersTestNullTemplateNameAndMinBalance() {
        Mockito.doReturn(quotaAccountVOMock).when(quotaAccountDaoMock).findByIdQuotaAccount(Mockito.any());
        Mockito.doReturn(null).when(quotaConfigureEmailCmdMock).getTemplateName();
        Mockito.doReturn(null).when(quotaConfigureEmailCmdMock).getMinBalance();
        quotaResponseBuilderImplSpy.validateQuotaConfigureEmailCmdParameters(quotaConfigureEmailCmdMock);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateQuotaConfigureEmailCmdParametersTestEnableNullAndTemplateNameNotNull() {
        Mockito.doReturn(quotaAccountVOMock).when(quotaAccountDaoMock).findByIdQuotaAccount(Mockito.any());
        Mockito.doReturn(QuotaConfig.QuotaEmailTemplateTypes.QUOTA_LOW.toString()).when(quotaConfigureEmailCmdMock).getTemplateName();
        Mockito.doReturn(null).when(quotaConfigureEmailCmdMock).getEnable();
        quotaResponseBuilderImplSpy.validateQuotaConfigureEmailCmdParameters(quotaConfigureEmailCmdMock);
    }


    @Test
    public void validateQuotaConfigureEmailCmdParametersTestNullTemplateName() {
        Mockito.doReturn(quotaAccountVOMock).when(quotaAccountDaoMock).findByIdQuotaAccount(Mockito.any());
        Mockito.doReturn(null).when(quotaConfigureEmailCmdMock).getTemplateName();
        Mockito.doReturn(100D).when(quotaConfigureEmailCmdMock).getMinBalance();
        quotaResponseBuilderImplSpy.validateQuotaConfigureEmailCmdParameters(quotaConfigureEmailCmdMock);
    }


    @Test
    public void validateQuotaConfigureEmailCmdParametersTestWithTemplateNameAndEnable() {
        Mockito.doReturn(quotaAccountVOMock).when(quotaAccountDaoMock).findByIdQuotaAccount(Mockito.any());
        Mockito.doReturn(QuotaConfig.QuotaEmailTemplateTypes.QUOTA_LOW.toString()).when(quotaConfigureEmailCmdMock).getTemplateName();
        Mockito.doReturn(true).when(quotaConfigureEmailCmdMock).getEnable();
        quotaResponseBuilderImplSpy.validateQuotaConfigureEmailCmdParameters(quotaConfigureEmailCmdMock);
    }

}
