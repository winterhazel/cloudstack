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

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.cloud.user.Account.State;

public class QuotaSummaryResponse extends BaseResponse {

    @SerializedName("accountid")
    @Param(description = "account id")
    private String accountId;

    @SerializedName("account")
    @Param(description = "account name")
    private String accountName;

    @SerializedName("domainid")
    @Param(description = "domain id")
    private String domainId;

    @SerializedName("domain")
    @Param(description = "domain path")
    private String domainPath;

    @SerializedName("balance")
    @Param(description = "account balance")
    private BigDecimal balance;

    @SerializedName("state")
    @Param(description = "account state")
    private State state;

    @SerializedName("domainremoved")
    @Param(description = "domain is removed or not")
    private boolean domainRemoved;

    @SerializedName("accountremoved")
    @Param(description = "account is removed or not")
    private boolean accountRemoved;

    @SerializedName("currency")
    @Param(description = "currency")
    private String currency;

    @SerializedName("quotaenabled")
    @Param(description = "if the account has the quota config enabled")
    private boolean quotaEnabled;

    @SerializedName("projectname")
    @Param(description = "name of the project")
    private String projectName;

    @SerializedName("projectid")
    @Param(description = "project id")
    private String projectId;

    @SerializedName("projectremoved")
    @Param(description = "project is removed or not")
    private Boolean projectRemoved;

    public QuotaSummaryResponse() {
        super();
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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

    public String getDomainPath() {
        return domainPath;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance.setScale(2, RoundingMode.HALF_EVEN);
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isDomainRemoved() {
        return domainRemoved;
    }

    public void setDomainRemoved(boolean domainRemoved) {
        this.domainRemoved = domainRemoved;
    }

    public boolean isAccountRemoved() {
        return accountRemoved;
    }

    public void setAccountRemoved(boolean accountRemoved) {
        this.accountRemoved = accountRemoved;
    }

    public boolean getQuotaEnabled() {
        return quotaEnabled;
    }

    public void setQuotaEnabled(boolean quotaEnabled) {
        this.quotaEnabled = quotaEnabled;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Boolean isProjectRemoved() {
        return projectRemoved;
    }

    public void setProjectRemoved(Boolean projectRemoved) {
        this.projectRemoved = projectRemoved;
    }
}
