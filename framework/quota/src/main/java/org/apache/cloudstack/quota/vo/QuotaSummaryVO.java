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

package org.apache.cloudstack.quota.vo;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.user.Account;

@Entity
@Table(name = "quota_summary_view")
public class QuotaSummaryVO {

    @Id
    @Column(name = "account_id")
    private Long accountId = null;

    @Column(name = "quota_enforce")
    private Integer quotaEnforce = 0;

    @Column(name = "quota_balance")
    private BigDecimal quotaBalance;

    @Column(name = "quota_balance_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date quotaBalanceDate = null;

    @Column(name = "quota_min_balance")
    private BigDecimal quotaMinBalance;

    @Column(name = "quota_alert_type")
    private Integer quotaAlertType = null;

    @Column(name = "quota_alert_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date quotaAlertDate = null;

    @Column(name = "last_statement_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastStatementDate = null;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "account_state")
    @Enumerated(EnumType.STRING)
    private Account.State accountState;

    @Column(name = "account_removed")
    private Date accountRemoved;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "domain_path")
    private String domainPath;

    @Column(name = "domain_removed")
    private Date domainRemoved;

    @Column(name = "project_uuid")
    private String projectUuid;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "project_removed")
    private Date projectRemoved;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getQuotaEnforce() {
        return quotaEnforce;
    }

    public void setQuotaEnforce(Integer quotaEnforce) {
        this.quotaEnforce = quotaEnforce;
    }

    public BigDecimal getQuotaBalance() {
        return quotaBalance;
    }

    public void setQuotaBalance(BigDecimal quotaBalance) {
        this.quotaBalance = quotaBalance;
    }

    public Date getQuotaBalanceDate() {
        return quotaBalanceDate;
    }

    public void setQuotaBalanceDate(Date quotaBalanceDate) {
        this.quotaBalanceDate = quotaBalanceDate;
    }

    public BigDecimal getQuotaMinBalance() {
        return quotaMinBalance;
    }

    public void setQuotaMinBalance(BigDecimal quotaMinBalance) {
        this.quotaMinBalance = quotaMinBalance;
    }

    public Integer getQuotaAlertType() {
        return quotaAlertType;
    }

    public void setQuotaAlertType(Integer quotaAlertType) {
        this.quotaAlertType = quotaAlertType;
    }

    public Date getQuotaAlertDate() {
        return quotaAlertDate;
    }

    public void setQuotaAlertDate(Date quotaAlertDate) {
        this.quotaAlertDate = quotaAlertDate;
    }

    public Date getLastStatementDate() {
        return lastStatementDate;
    }

    public void setLastStatementDate(Date lastStatementDate) {
        this.lastStatementDate = lastStatementDate;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Account.State getAccountState() {
        return accountState;
    }

    public void setAccountState(Account.State accountState) {
        this.accountState = accountState;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Date getAccountRemoved() {
        return accountRemoved;
    }

    public void setAccountRemoved(Date accountRemoved) {
        this.accountRemoved = accountRemoved;
    }

    public Date getDomainRemoved() {
        return domainRemoved;
    }

    public void setDomainRemoved(Date domainRemoved) {
        this.domainRemoved = domainRemoved;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public void setProjectUuid(String projectUuid) {
        this.projectUuid = projectUuid;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Date getProjectRemoved() {
        return projectRemoved;
    }

    public void setProjectRemoved(Date projectRemoved) {
        this.projectRemoved = projectRemoved;
    }
}
