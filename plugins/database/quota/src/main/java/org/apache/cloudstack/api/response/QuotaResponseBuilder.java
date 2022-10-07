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

import com.cloud.user.User;
import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaConfigureEmailCmd;
import org.apache.cloudstack.api.command.QuotaCreditsListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.command.QuotaSummaryCmd;
import org.apache.cloudstack.api.command.QuotaTariffCreateCmd;
import org.apache.cloudstack.api.command.QuotaTariffListCmd;
import org.apache.cloudstack.api.command.QuotaTariffUpdateCmd;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaEmailConfigurationVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.quota.vo.ResourcesToQuoteVo;

import java.util.Date;
import java.util.List;

import com.cloud.utils.Pair;
import org.apache.cloudstack.quota.vo.ResourcesQuotingResultResponse;

public interface QuotaResponseBuilder {

    QuotaTariffVO updateQuotaTariffPlan(QuotaTariffUpdateCmd cmd);

    QuotaTariffVO createQuotaTariff(QuotaTariffCreateCmd cmd);

    Pair<List<QuotaTariffVO>, Integer> listQuotaTariffPlans(QuotaTariffListCmd cmd);

    QuotaTariffResponse createQuotaTariffResponse(QuotaTariffVO configuration, boolean returnActivationRule);

    QuotaStatementResponse createQuotaStatementResponse(List<QuotaUsageVO> quotaUsage, QuotaStatementCmd cmd);

    QuotaBalanceResponse createQuotaBalanceResponse(QuotaBalanceCmd cmd);

    Pair<List<QuotaSummaryResponse>, Integer> createQuotaSummaryResponse(QuotaSummaryCmd cmd);

    List<QuotaUsageVO> getQuotaUsage(QuotaStatementCmd cmd);

    List<QuotaBalanceVO> getQuotaBalance(QuotaBalanceCmd cmd);

    QuotaCreditsResponse addQuotaCredits(Long accountId, Long domainId, Double amount, Long updatedBy, Boolean enforce);

    List<QuotaEmailTemplateResponse> listQuotaEmailTemplates(QuotaEmailTemplateListCmd cmd);

    boolean updateQuotaEmailTemplate(QuotaEmailTemplateUpdateCmd cmd);

    Date startOfNextDay(Date dt);

    Date startOfNextDay();

    boolean deleteQuotaTariff(String quotaTariffUuid);

    Pair<List<QuotaCreditsResponse>, Integer> createQuotaCreditsListResponse(QuotaCreditsListCmd cmd);

    /**
     * Quotes the resources based in the current valid Quota tariffs.
     * @param resourcesToQuoteAsJson String containing the resources to be quoted. This string will be converted to a list of
     * {@link ResourcesToQuoteVo} with Gson.
     * @return a list of {@link ResourcesQuotingResultResponse}, containing the total of each quoting.
     */
    List<ResourcesQuotingResultResponse> quoteResources(String resourcesToQuoteAsJson);

    boolean isUserAllowedToSeeActivationRules(User user);

    Pair<QuotaEmailConfigurationVO, Double> configureQuotaEmail(QuotaConfigureEmailCmd cmd);

    QuotaConfigureEmailResponse createQuotaConfigureEmailResponse(QuotaEmailConfigurationVO quotaEmailConfigurationVO, Double minBalance, long accountId);

    List<QuotaConfigureEmailResponse> listEmailConfiguration(long accountId);
}
