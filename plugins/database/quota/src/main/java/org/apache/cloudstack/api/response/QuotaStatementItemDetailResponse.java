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
import java.util.Date;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class QuotaStatementItemDetailResponse extends BaseResponse {

    @SerializedName("accountid")
    @Param(description = "Account's id.")
    private Long accountId;

    @SerializedName("domainid")
    @Param(description = "Domain's id.")
    private Long domainId;

    @SerializedName("quotaconsumed")
    @Param(description = "Quota consumed.")
    private BigDecimal quotaUsed;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "Item's start date.")
    private Date startDate;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "Item's end date.")
    private Date endDate;

    @SerializedName(ApiConstants.RESOURCE_ID)
    @Param(description = "Resource's id.")
    private Long resourceId;

    public void setQuotaUsed(BigDecimal quotaUsed) {
        this.quotaUsed = quotaUsed.setScale(2, RoundingMode.HALF_EVEN);
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getQuotaUsed() {
        return quotaUsed;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

}
