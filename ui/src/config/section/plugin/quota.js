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

import { i18n } from '@/locales'
import { formats, moment } from '@/utils/date'

export default {
  name: 'quota',
  title: 'label.quota',
  icon: 'pie-chart',
  docHelp: 'plugins/quota.html',
  permission: ['quotaSummary'],
  children: [
    {
      name: 'quotasummary',
      title: 'label.quota.summary',
      icon: 'bars',
      permission: ['quotaSummary'],
      columns: ['account',
        {
          state: (record) => record.state.toLowerCase()
        },
        {
          quotastate: (record) => record.quotaenabled ? 'Enabled' : 'Disabled'
        }, 'domain', 'currency', 'balance'
      ],
      columnNames: ['account', 'accountstate', 'quotastate', 'domain', 'currency', 'currentbalance'],
      details: ['currency', 'currentbalance'],
      component: () => import('@/views/plugins/quota/QuotaSummary.vue'),
      filters: ['all', 'activeaccounts', 'removedaccounts'],
      tabs: [
        {
          name: 'quota.statement.quota',
          component: () => import('@/views/plugins/quota/QuotaUsage.vue')
        },
        {
          name: 'quota.statement.balance',
          component: () => import('@/views/plugins/quota/QuotaBalance.vue')
        },
        {
          name: 'quota.credits',
          component: () => import('@/views/plugins/quota/QuotaCredits.vue')
        }
      ],
      actions: [
        {
          api: 'quotaCredits',
          icon: 'plus',
          docHelp: 'plugins/quota.html#quota-credits',
          label: 'label.quota.add.credits',
          listView: true,
          popup: true,
          component: () => import('@/views/plugins/quota/AddCredit.vue')
        }
      ]
    },
    {
      name: 'quotatariff',
      title: 'label.quota.tariff',
      icon: 'credit-card',
      docHelp: 'plugins/quota.html#quota-tariff',
      permission: ['quotaTariffList'],
      columns: ['name', 'usageName', 'usageUnit', 'tariffValue',
        {
          hasActivationRule: (record) => record.activationRule ? i18n.t('label.yes') : i18n.t('label.no')
        },
        {
          effectiveDate: (record) => moment.utc(record.effectiveDate).format(formats.ISO_DATE_ONLY)
        },
        {
          endDate: (record) => record.endDate ? moment.utc(record.endDate).format(formats.ISO_DATE_ONLY) : undefined
        },
        {
          removed: (record) => record.removed ? moment.utc(record.removed).format(formats.ISO_DATETIME) : undefined
        }],
      columnNames: ['name', 'usageName', 'usageUnit', 'quota.tariff.value', 'quota.tariff.hasactivationrule', 'quota.startdate', 'quota.enddate', 'removed'],
      details: ['uuid', 'name', 'description', 'usageName', 'usageUnit', 'tariffValue', 'effectiveDate', 'endDate', 'removed', 'activationRule'],
      detailLabels: { tariffValue: 'quota.tariff.value', effectiveDate: 'quota.startdate', endDate: 'quota.enddate', activationRule: 'quota.tariff.activationrule' },
      filters: ['all', 'active', 'removed'],
      searchFilters: ['name'],
      actions: [
        {
          api: 'quotaTariffCreate',
          icon: 'plus',
          label: 'label.action.quota.tariff.create',
          listView: true,
          popup: true,
          component: () => import('@/views/plugins/quota/CreateQuotaTariff.vue')
        },
        {
          api: 'quotaTariffUpdate',
          icon: 'edit',
          label: 'label.quota.tariff.edit',
          dataView: true,
          popup: true,
          show: (record) => !record.removed,
          component: () => import('@/views/plugins/quota/EditQuotaTariff.vue')
        },
        {
          api: 'quotaTariffDelete',
          icon: 'delete',
          label: 'label.action.quota.tariff.remove',
          message: 'message.action.quota.tariff.remove',
          params: (record) => ({ uuid: record.uuid }),
          dataView: true,
          show: (record) => !record.removed
        }
      ]
    },
    {
      name: 'quotaemailtemplate',
      title: 'label.emailtemplate',
      icon: 'mail',
      permission: ['quotaEmailTemplateList'],
      columns: ['templatetype', 'templatesubject', 'templatebody'],
      details: ['templatetype', 'templatesubject', 'templatebody'],
      tabs: [{
        name: 'details',
        component: () => import('@/views/plugins/quota/EmailTemplateDetails.vue')
      }]
    },
    {
      name: 'quotausage',
      title: 'quota.usage',
      icon: 'bars',
      permission: ['quotaStatement'],
      hidden: true,
      columns: ['usageName']
    }
  ]
}
