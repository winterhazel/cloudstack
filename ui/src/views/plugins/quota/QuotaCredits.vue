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
      @fetchData="fetchData"/>
    <a-button v-if="dataSource.length > 0" type="dashed" @click="exportDataToCsv" class="w-100" icon="download">
      {{ $t('label.export.data.csv') }}
    </a-button>
    <bar-chart v-if="dataSource.length > 0" :datasets="getChartDatasets()" :options="getChartOptions()" class="chart-margin"/>
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
import BarChart from '@/components/view/chart/BarChart.js'
import * as dateUtils from '@/utils/date'
import * as exportUtils from '@/utils/export'
import FilterQuotaDataByPeriodView from './FilterQuotaDataByPeriodView.vue'
import * as chartUtils from '@/utils/chart'

export default {
  name: 'QuotaCredits',
  components: {
    FilterQuotaDataByPeriodView,
    BarChart
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
      pattern: dateUtils.formats.ISO_DATE_ONLY,
      currency: '',
      dataSource: [],
      startDate: moment().subtract(30, 'days'),
      endDate: moment()
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
          customRender: (text) => dateUtils.formatToExtended(text, dateUtils.formats.DATETIME_EXTENDED_WITHOUT_SECONDS),
          sorter: (a, b) => a.date - b.date,
          defaultSortOrder: 'descend'
        },
        {
          title: this.$t('label.credit'),
          dataIndex: 'credit',
          width: 'calc(100% / 2)',
          scopedSlots: { customRender: 'credit' },
          sorter: (a, b) => a.credit - b.credit
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
      if (this.tab === 'quota.credits') {
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
        const data = await this.getQuotaCreditsList(startDate, endDate)
        this.currency = data[0]?.currency
        this.dataSource = data.map(row => ({
          ...row,
          date: moment.utc(row.creditedon)
        }))
      } finally {
        this.loading = false
      }
    },
    async getQuotaCreditsList (startDate, endDate) {
      const params = {
        domainid: this.$route.query?.domainid,
        accountid: this.$route.query?.accountid,
        startDate: startDate.format(this.pattern),
        endDate: endDate.format(this.pattern)
      }

      return await api('quotaCreditsList', params)
        .then(json => json.quotacreditslistresponse.credit || {})
        .catch(error => { error && this.$notifyInfo({ message: this.$t('message.request.no.data') }) })
    },
    exportDataToCsv () {
      exportUtils.exportDataToCsv({
        data: this.dataSource,
        keys: ['creditorname', 'date', 'credit'],
        fileName: `credits-of-user-${this.$route.params.id}-between-${this.startDate.format(this.pattern)}-and-${this.endDate.format(this.pattern)}`,
        dateFormat: dateUtils.formats.ISO_DATETIME
      })
    },
    getChartDatasets () {
      const datasets = []

      const data = []
      const res = {}
      this.dataSource.map(value => {
        const date = value.date.format(this.pattern)
        if (!res[date]) {
          res[date] = { date, credit: 0 }
          data.push(res[date])
        }

        res[date].credit += value.credit
        return res
      })

      datasets.push({
        label: this.$t('label.credit'),
        data: data.map(row => row.credit),
        ...chartUtils.getChartColorObject()
      })

      return {
        labels: data.map(row => row.date),
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
            },
            offset: true
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
