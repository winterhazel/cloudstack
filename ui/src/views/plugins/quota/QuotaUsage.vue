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

<template>
  <div>
    <filter-quota-data-by-period-view :start-date="startDate" :end-date="endDate" @fetchData="fetchData"/>
    <a-button v-if="dataSource.length > 0" type="dashed" @click="exportDataToCsv" class="w-100" icon="download">
      {{ $t('label.export.data.csv') }}
    </a-button>
    <bar-chart v-if="dataSource.length > 0" :datasets="getChartDatasets()" class="chart-margin"/>
    <a-table
      size="small"
      :loading="loading"
      :columns="columns"
      :dataSource="dataSource"
      :rowKey="record => record.name"
      :pagination="false"
      :scroll="{ y: '55vh' }">
      <template slot="title" v-if="dataSource.length > 0">
        {{ $t('label.currency') }}: <b>{{ currency }}</b>
      </template>
      <template slot="footer" v-if="dataSource.length > 0">
        <div style="text-align: right;">
          {{ $t('label.quota.totalconsumption') }}: <b>{{ totalQuota }}</b>
        </div>
      </template>
    </a-table>

    <hr class="chart-margin" id="detailed-resource" />
    <template>
      <div>
        <div>
          <strong><tooltip-label :title="$t('label.quota.usage.detailed.resource')" :tooltip="$t('message.quota.usage.detailed.resource.redirect')"/></strong>
        </div>
        <div>
          <a-select
            style="width: 100%; margin: 5px 0 10px 0px"
            show-search
            v-model="selectedResource"
            @change="handleSelectedResourceChange">
            <a-select-option
              v-for="quotaType of getQuotaTypes()"
              :value="`${quotaType.id}-${quotaType.type}`"
              :key="quotaType.id">
              {{ $t(quotaType.type) }}
            </a-select-option>
          </a-select>
          <a-button v-if="dataSourceDetailed.length > 0" type="dashed" @click="exportDetailsToCsv" class="w-100 mb-10" icon="download">
            {{ $t('label.export.details.csv') }}
          </a-button>
          <a-table
            size="small"
            :loading="loadingDetails"
            :columns="detailedColumns"
            :dataSource="dataSourceDetailed"
            :rowKey="record => `${record.displayname}-${record.startdate}`"
            :pagination="false"
            :scroll="{ y: '55vh' }">
            <template slot="title" v-if="dataSourceDetailed.length > 0">
              <div>{{ $t('label.currency') }}: <b>{{ currency }}</b></div>
            </template>
          </a-table>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { api } from '@/api'
import moment from 'moment'
import FilterQuotaDataByPeriodView from './FilterQuotaDataByPeriodView.vue'
import BarChart from '@/components/view/chart/BarChart.js'
import * as dateUtils from '@/utils/date'
import * as exportUtils from '@/utils/export'
import { getChartColorObject } from '@/utils/chart'
import { getByQuotaTypeByType, getIsImplementedQuotaTypes } from '@/utils/quota'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'QuotaUsage',
  components: {
    FilterQuotaDataByPeriodView,
    BarChart,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      default: () => {}
    },
    tab: {
      type: String,
      default: () => ''
    }
  },
  data () {
    return {
      loading: false,
      loadingDetails: false,
      dataSource: [],
      dataSourceDetailed: [],
      pattern: dateUtils.formats.ISO_DATE_ONLY,
      currency: '',
      totalQuota: 0,
      startDate: moment().subtract(30, 'days'),
      endDate: moment(),
      selectedResource: ''
    }
  },
  created () {
    this.fetchData(this.startDate, this.endDate)
    this.fetchDetailedData(this.startDate, this.endDate)
  },
  computed: {
    columns () {
      return [
        {
          title: this.$t('label.quota.type.name'),
          dataIndex: 'name',
          width: 'calc(100% / 3)',
          sorter: (a, b) => a.name.localeCompare(b.name),
          scopedSlots: { customRender: 'name' },
          customRender: (text, record) => {
            return <a onclick={() => this.setSelectedResource(`${record.type}-${record.name}`)}>{ this.$t(text) }</a>
          }
        },
        {
          title: this.$t('label.quota.type.unit'),
          dataIndex: 'unit',
          width: 'calc(100% / 3)',
          sorter: (a, b) => a.unit.localeCompare(b.unit),
          scopedSlots: { customRender: 'unit' },
          customRender: (text) => {
            return <div>{ this.$t(text) }</div>
          }
        },
        {
          title: this.$t('label.quota.usage'),
          dataIndex: 'quota',
          width: 'calc(100% / 3)',
          sorter: (a, b) => a.quota - b.quota,
          defaultSortOrder: 'descend',
          scopedSlots: { customRender: 'quota' }
        }
      ]
    },
    detailedColumns () {
      return [
        {
          title: this.$t('label.resource'),
          dataIndex: 'resource.displayname',
          width: '25%',
          sorter: (a, b) => a.resource.displayname.localeCompare(b.resource.displayname),
          scopedSlots: { customRender: 'displayname' },
          customRender: (text, record) => {
            if (!text) return '-'

            const quotaType = getByQuotaTypeByType(this.selectedResource.split('-')[1])
            if (record.resource.removed || !quotaType.isImplemented) {
              return text
            }

            return <a href={ `#/${quotaType.componentUrl}/${record.resource.id}`} target="_blank">{{ text }}</a>
          }
        },
        {
          title: this.$t('label.quota.startdate'),
          dataIndex: 'startDate',
          width: '25%',
          scopedSlots: { customRender: 'startDate' },
          customRender: (text) => dateUtils.formatToExtended(text, dateUtils.formats.DATETIME_EXTENDED_WITHOUT_SECONDS),
          sorter: (a, b) => a.startDate - b.startDate,
          defaultSortOrder: 'descend'
        },
        {
          title: this.$t('label.quota.enddate'),
          dataIndex: 'endDate',
          width: '25%',
          scopedSlots: { customRender: 'endDate' },
          customRender: (text) => dateUtils.formatToExtended(text, dateUtils.formats.DATETIME_EXTENDED_WITHOUT_SECONDS),
          sorter: (a, b) => a.endDate - b.endDate
        },
        {
          title: this.$t('label.quota.usage.quota.consumed'),
          dataIndex: 'quotaConsumed',
          width: '25%',
          sorter: (a, b) => a.quotaConsumed - b.quotaConsumed,
          scopedSlots: { customRender: 'quotaConsumed' }
        }
      ]
    }
  },
  watch: {
    tab (newTab, oldTab) {
      this.tab = newTab
      this.fetchDataIfInTab()
    }
  },
  methods: {
    fetchDataIfInTab () {
      if (this.tab === 'quota.statement.quota') {
        this.fetchData(this.startDate, this.endDate)
        this.fetchDetailedData(this.startDate, this.endDate)
      }
    },
    exportDataToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSource,
        keys: ['type', 'name', 'unit', 'quota'],
        fileName: `quota-usage-of-user-${this.$route.params.id}-between-${this.startDate.format(this.pattern)}-and-${this.endDate.format(this.pattern)}`
      })
    },
    exportDetailsToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSourceDetailed.map(row => ({ ...row, resource: row.resource.displayname })),
        keys: ['resource', 'quotaconsumed', 'startDate', 'endDate'],
        fileName: `detailed-quota-usage-of-type-${this.selectedResource}-of-user-${this.$route.params.id}-between-${this.startDate.format(this.pattern)}-and-${this.endDate.format(this.pattern)}`,
        dateFormat: dateUtils.formats.ISO_DATETIME
      })
    },
    getChartDatasets () {
      const datasets = []

      for (const row of this.dataSource) {
        if (row.quota > 0) {
          datasets.push({
            label: this.$t(row.name),
            data: [row.quota],
            ...getChartColorObject(getByQuotaTypeByType(row.name).chartColor)
          })
        }
      }

      return {
        labels: [this.$t('label.resource')],
        datasets
      }
    },
    async fetchData (startDate, endDate) {
      if (this.loading) return

      this.startDate = startDate
      this.endDate = endDate
      this.loading = true
      this.dataSource = []

      try {
        const quotaStatement = await this.getQuotaStatement({
          startDate: startDate.format(this.pattern),
          endDate: endDate.format(this.pattern)
        })
        this.dataSource = quotaStatement.quotausage.filter(item => item.quota !== 0)
        this.dataSource.forEach(item => { item.unit = this.$t(item.unit) })
        this.currency = quotaStatement.currency
        this.totalQuota = quotaStatement.totalquota
      } finally {
        this.loading = false
      }
    },
    async fetchDetailedData (startDate, endDate) {
      if (this.selectedResource === '' || this.loadingDetails) return

      this.dataSourceDetailed = []
      this.loadingDetails = true

      try {
        const quotaStatement = await this.getQuotaStatement({
          startDate: startDate.format(this.pattern),
          endDate: endDate.format(this.pattern),
          showDetails: true,
          type: this.selectedResource.split('-')[0]
        })

        this.dataSourceDetailed = quotaStatement.quotausage[0].details
        this.dataSourceDetailed = this.dataSourceDetailed.map(detail => ({
          ...detail,
          startDate: moment.utc(detail.startdate),
          endDate: moment.utc(detail.enddate),
          quotaConsumed: parseFloat(detail.quotaconsumed)
        }))
      } finally {
        this.loadingDetails = false
      }
    },
    async getQuotaStatement (apiParams) {
      const params = {
        domainid: this.$route.query?.domainid,
        account: this.$route.query?.account,
        ...apiParams
      }

      return await api('quotaStatement', params)
        .then(json => json.quotastatementresponse.statement || {})
        .catch(error => { error && this.$notifyInfo({ message: this.$t('message.request.no.data') }) })
    },
    getQuotaTypes () {
      return getIsImplementedQuotaTypes()
    },
    async setSelectedResource (resource) {
      await this.handleSelectedResourceChange(resource)
    },
    async handleSelectedResourceChange (value) {
      this.selectedResource = value
      document.getElementById('detailed-resource').scrollIntoView({ behavior: 'smooth' })
      await this.fetchDetailedData(this.startDate, this.endDate)
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/common/common.scss';
@import '@/style/objects/chart.scss';
</style>
