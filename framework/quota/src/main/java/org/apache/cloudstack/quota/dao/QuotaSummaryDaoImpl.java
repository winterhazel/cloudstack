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

package org.apache.cloudstack.quota.dao;

import java.util.List;

import org.apache.cloudstack.quota.vo.QuotaSummaryVO;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;

public class QuotaSummaryDaoImpl extends GenericDaoBase<QuotaSummaryVO, Long> implements QuotaSummaryDao {

    @Override
    public Pair<List<QuotaSummaryVO>, Integer> listQuotaSummariesForAccountAndOrDomain(Long accountId, Long domainId, String domainPath, Long startIndex, Long pageSize) {
        SearchCriteria<QuotaSummaryVO> searchCriteria = createListQuotaSummariesSearchCriteria(accountId, domainId, domainPath);
        Filter sorter = new Filter(QuotaSummaryVO.class, "accountName", true, startIndex, pageSize);

        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<Pair<List<QuotaSummaryVO>, Integer>>() {
            @Override
            public Pair<List<QuotaSummaryVO>, Integer> doInTransaction(final TransactionStatus status) {
                return searchAndCount(searchCriteria, sorter);
            }
        });
    }

    protected SearchCriteria<QuotaSummaryVO> createListQuotaSummariesSearchCriteria(Long accountId, Long domainId, String domainPath) {
        SearchCriteria<QuotaSummaryVO> searchCriteria = createListQuotaSummariesSearchBuilder(accountId, domainId, domainPath).create();

        searchCriteria.setParametersIfNotNull("account_id", accountId);
        searchCriteria.setParametersIfNotNull("domain_id", domainId);

        if (domainPath != null) {
            searchCriteria.setParameters("domain_path", domainPath + "%");
        }

        return searchCriteria;
    }

    protected SearchBuilder<QuotaSummaryVO> createListQuotaSummariesSearchBuilder(Long accountId, Long domainId, String domainPath) {
        SearchBuilder<QuotaSummaryVO> searchBuilder = createSearchBuilder();

        if (accountId != null) {
            searchBuilder.and("account_id", searchBuilder.entity().getAccountId(), SearchCriteria.Op.EQ);
        }

        if (domainId != null) {
            searchBuilder.and("domain_id", searchBuilder.entity().getDomainId(), SearchCriteria.Op.EQ);
        }

        if (domainPath != null) {
            searchBuilder.and("domain_path", searchBuilder.entity().getDomainPath(), SearchCriteria.Op.LIKE);
        }

        return searchBuilder;
    }

}
