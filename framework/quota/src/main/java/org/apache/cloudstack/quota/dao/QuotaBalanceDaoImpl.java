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
package org.apache.cloudstack.quota.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;

@Component
public class QuotaBalanceDaoImpl extends GenericDaoBase<QuotaBalanceVO, Long> implements QuotaBalanceDao {
    private static final Logger s_logger = Logger.getLogger(QuotaBalanceDaoImpl.class.getName());

    @Override
    public QuotaBalanceVO getLastQuotaBalanceEntry(final Long accountId, final Long domainId, final Date beforeThis) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaBalanceVO>() {
            @Override
            public QuotaBalanceVO doInTransaction(final TransactionStatus status) {
                Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", false, 0L, 1L);

                QueryBuilder<QuotaBalanceVO> qb = QueryBuilder.create(QuotaBalanceVO.class);
                qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                qb.and(qb.entity().getCreditsId(), SearchCriteria.Op.EQ, 0);

                if (beforeThis != null) {
                    qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.LT, beforeThis);
                }

                List<QuotaBalanceVO> quotaBalanceEntries = search(qb.create(), filter);
                return !quotaBalanceEntries.isEmpty() ? quotaBalanceEntries.get(0) : null;
            }
        });
    }

    @Override
    public QuotaBalanceVO findLaterBalanceEntry(final Long accountId, final Long domainId, final Date afterThis) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaBalanceVO>() {
            @Override
            public QuotaBalanceVO doInTransaction(final TransactionStatus status) {
                List<QuotaBalanceVO> quotaBalanceEntries = new ArrayList<>();
                Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true, 0L, 1L);
                QueryBuilder<QuotaBalanceVO> qb = QueryBuilder.create(QuotaBalanceVO.class);
                qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                qb.and(qb.entity().getCreditsId(), SearchCriteria.Op.EQ, 0);
                qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.GT, afterThis);
                quotaBalanceEntries = search(qb.create(), filter);
                return quotaBalanceEntries.size() > 0 ? quotaBalanceEntries.get(0) : null;
            }
        });
    }

    @Override
    public QuotaBalanceVO saveQuotaBalance(final QuotaBalanceVO qb) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaBalanceVO>() {
            @Override
            public QuotaBalanceVO doInTransaction(final TransactionStatus status) {
                return persist(qb);
            }
        });
    }

    @Override
    public List<QuotaBalanceVO> findCreditBalances(final Long accountId, final Long domainId, final Date startDate, final Date endDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<List<QuotaBalanceVO>>() {
            @Override
            public List<QuotaBalanceVO> doInTransaction(final TransactionStatus status) {
                if (startDate == null || endDate == null || startDate.after(endDate)) {
                    return new ArrayList<>();
                }

                Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true, 0L, Long.MAX_VALUE);
                QueryBuilder<QuotaBalanceVO> qb = QueryBuilder.create(QuotaBalanceVO.class);
                qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                qb.and(qb.entity().getCreditsId(), SearchCriteria.Op.GT, 0);
                qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.BETWEEN, startDate, endDate);

                return search(qb.create(), filter);
            }
        });
    }

    @Override
    public List<QuotaBalanceVO> listQuotaBalances(Long accountId, Long domainId, Date startDate, Date endDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<List<QuotaBalanceVO>>() {
            @Override
            public List<QuotaBalanceVO> doInTransaction(final TransactionStatus status) {
                QueryBuilder<QuotaBalanceVO> qb = QueryBuilder.create(QuotaBalanceVO.class);

                qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                qb.and(qb.entity().getCreditsId(), SearchCriteria.Op.EQ, 0);
                qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.BETWEEN, startDate, endDate);

                Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true);
                return listBy(qb.create(), filter);
            }
        });

    }

    @Override
    public BigDecimal getLastQuotaBalance(Long accountId, Long domainId) {
        QuotaBalanceVO quotaBalance = getLastQuotaBalanceEntry(accountId, domainId, null);

        BigDecimal finalBalance = BigDecimal.ZERO;
        Date startDate = DateUtils.addDays(new Date(), -1);
        if (quotaBalance == null) {
            s_logger.info(String.format("There are no balance entries for account [%s] and domain [%s]. Considering only new added credits.", accountId, domainId));
        } else {
            finalBalance = quotaBalance.getCreditBalance();
            startDate = quotaBalance.getUpdatedOn();
        }

        List<QuotaBalanceVO> credits = findCreditBalances(accountId, domainId, startDate, new Date());

        for (QuotaBalanceVO credit : credits) {
            finalBalance = finalBalance.add(credit.getCreditBalance());
        }

        return finalBalance;
    }

}
