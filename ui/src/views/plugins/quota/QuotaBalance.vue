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
    <filter-quota-data-by-period-view
      :start-date="startDate"
      :end-date="endDate"
      :preset-ranges="presetRanges"
      :date-limit="dateLimit"
      @fetchData="fetchData"/>
    <a-button v-if="dataSource.length > 0" type="dashed" @click="exportDataToCsv" class="w-100" icon="download">
      {{ $t('label.export.data.csv') }}
    </a-button>
    <line-chart v-if="dataSource.length > 0" :datasets="getChartDatasets()" :options="getChartOptions()" class="chart-margin" />
    <a-table
      size="small"
      :loading="loading"
      :columns="columns"
      :dataSource="dataSource"
      :rowKey="record => record.name"
      :pagination="false"
      :scroll="{ y: '55vh' }"
    >
      <template slot="title" v-if="dataSource.length > 0">
        {{ $t('label.currency') }}: <b>{{ currency }}</b>
      </template>
    </a-table>
  </div>
</template>

<script>
import { api } from '@/api'
import moment from 'moment'
import LineChart from '@/components/view/chart/LineChart.js'
import * as dateUtils from '@/utils/date'
import * as exportUtils from '@/utils/export'
import FilterQuotaDataByPeriodView from './FilterQuotaDataByPeriodView.vue'
import * as chartUtils from '@/utils/chart'

export default {
  name: 'QuotaBalance',
  components: {
    FilterQuotaDataByPeriodView,
    LineChart
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    tab: {
      type: String,
      default: () => ''
    }
  },
  data () {
    return {
      loading: false,
      pattern: dateUtils.formats.ISO_DATE_ONLY,
      currency: '',
      dataSource: [],
      startDate: moment().subtract(30, 'days'),
      endDate: moment().subtract(1, 'days'),
      presetRanges: {
        [this.$t('label.quota.filter.preset.thismonth')]: [moment().startOf('month'), moment().subtract(1, 'days')],
        [this.$t('label.quota.filter.preset.thisyear')]: [moment().startOf('year'), moment().subtract(1, 'days')]
      },
      dateLimit: moment().subtract(1, 'days').toDate()
    }
  },
  computed: {
    columns () {
      return [
        {
          title: this.$t('label.date'),
          dataIndex: 'date',
          width: 'calc(100% / 2)',
          scopedSlots: { customRender: 'date' },
          customRender: (text) => dateUtils.formatDateToExtended(moment(text)),
          sorter: (a, b) => a.date - b.date,
          defaultSortOrder: 'descend'
        },
        {
          title: this.$t('label.balance'),
          dataIndex: 'balance',
          width: 'calc(100% / 2)',
          scopedSlots: { customRender: 'balance' },
          sorter: (a, b) => a.balance - b.balance
        }
      ]
    }
  },
  watch: {
    tab (newTab, oldTab) {
      this.tab = newTab
      this.fetchDataIfInTab()
    },
    resource () {
      this.fetchDataIfInTab()
    }
  },
  created () {
    this.fetchData(this.startDate, this.endDate)
  },
  methods: {
    fetchDataIfInTab () {
      if (this.tab === 'quota.statement.balance') {
        this.fetchData(this.startDate, this.endDate)
      }
    },
    async fetchData (startDate, endDate) {
      if (this.loading) return

      this.startDate = startDate
      this.endDate = endDate
      this.dataSource = []
      this.loading = true

      try {
        const data = await this.getQuotaBalance(startDate, endDate)
        this.currency = data.currency
        this.dataSource = data.dailybalances.map(balance => ({
          ...balance,
          date: moment.utc(balance.date)
        }))
      } finally {
        this.loading = false
      }
    },
    async getQuotaBalance (startDate, endDate) {
      const params = {
        domainid: this.$route.query?.domainid,
        account: this.$route.query?.account,
        startDate: startDate.format(this.pattern),
        endDate: endDate.format(this.pattern)
      }

      return await api('quotaBalance', params)
        .then(json => json.quotabalanceresponse.balance || {})
        .catch(error => { error && this.$notifyInfo({ message: this.$t('message.request.no.data') }) })
    },
    exportDataToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSource,
        keys: ['date', 'balance'],
        fileName: `daily-quota-balance-of-user-${this.$route.params.id}-between-${this.startDate.format(this.pattern)}-and-${this.endDate.format(this.pattern)}`,
        dateFormat: this.pattern
      })
    },
    getChartDatasets () {
      const datasets = []

      datasets.push({
        label: this.$t('label.balance'),
        data: this.dataSource.map(row => row.balance),
        ...chartUtils.getChartColorObject()
      })

      return {
        labels: this.dataSource.map(row => row.date.format(this.pattern)),
        datasets
      }
    },
    getChartOptions () {
      return {
        scales: {
          xAxes: [{
            type: 'time',
            time: {
              unit: chartUtils.getUnitToTimeCartesianAxis('day', this.dataSource.length)
            }
          }]
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/common/common.scss';
@import '@/style/objects/chart.scss';
</style>
