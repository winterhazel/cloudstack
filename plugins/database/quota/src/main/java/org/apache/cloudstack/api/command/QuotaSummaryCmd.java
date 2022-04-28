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
package org.apache.cloudstack.api.command;

import com.cloud.user.Account;
import com.cloud.utils.Pair;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaSummaryResponse;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.log4j.Logger;

import java.util.List;

import javax.inject.Inject;

@APICommand(name = "quotaSummary", responseObject = QuotaSummaryResponse.class, description = "Lists accounts' balance summary.", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaSummaryCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(QuotaSummaryCmd.class);
    private static final String s_name = "quotasummaryresponse";

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, required = false, description = "Account's name for which balance will be listed.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.STRING, required = false, entityType = DomainResponse.class,
            description = "If domain's id is given and the caller is domain admin then the statement is generated for domain.", validations = {ApiArgValidator.UuidString})
    private String domainId;

    @Parameter(name = ApiConstants.LIST_ALL, type = CommandType.BOOLEAN, required = false, description = "False (default) lists balance summary for account. True lists"
            + " balance summary for accounts which te account has access.")
    private Boolean listAll;

    @Parameter(name = ApiConstants.SHOW_REMOVED_ACCOUNTS, type = CommandType.STRING, required = false, description = "If set to 'true', we will list also the removed accounts' summaries."
            + "If set to 'false', we will not list summaries of removed accounts. If set to 'only', we will list only removed accounts' summaries. If one set other than these"
            + " values, we will consider it as the 'default'. The default is 'false'.")
    private String showRemovedAccounts = "false";

    @Inject
    QuotaResponseBuilder quotaResponseBuilder;

    public QuotaSummaryCmd() {
        super();
    }

    @Override
    public void execute() {
        Pair<List<QuotaSummaryResponse>, Integer> responses = quotaResponseBuilder.createQuotaSummaryResponse(this);
        ListResponse<QuotaSummaryResponse> response = new ListResponse<QuotaSummaryResponse>();
        response.setResponses(responses.first(), responses.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    public Boolean isListAll() {
        return BooleanUtils.toBoolean(listAll);
    }

    public void setListAll(Boolean listAll) {
        this.listAll = listAll;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public String getShowRemovedAccounts() {
        return showRemovedAccounts;
    }

    public void setShowRemovedAccounts(String showRemoved) {
        this.showRemovedAccounts = showRemoved;
    }

}
