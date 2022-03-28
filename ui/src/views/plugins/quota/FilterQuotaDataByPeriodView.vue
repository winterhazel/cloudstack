<template>
  <div style="margin-bottom: 12px;">
    <a-modal
      v-model="showFilterQuotaDataByPeriodModal"
      :title="$t('label.quota.selectperiod')"
      :maskClosable="false"
      :footer="null">
      <template>
        <a-form :form="form" class="form-layout" @submit="handleSubmit">
          <a-form-item>
            <a-range-picker
              v-decorator="['dates']"
              :disabled-date="getDisabledDates"
              :ranges="presetDateRanges"
              :default-value="presetDateRanges[this.$t('label.quota.filter.preset.thismonth')]"
            />
          </a-form-item>
          <div :span="24" class="action-button">
            <a-button @click="closeFilterQuotaDataByPeriodModal">{{ this.$t('label.cancel') }}</a-button>
            <a-button ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
          </div>
        </a-form>
      </template>
    </a-modal>
    <a-row>
      <a-col>
        <a-button @click="openFilterQuotaDataByPeriodModal">
          <a-icon type="filter"/>
          <span v-html="getPeriodToString()"></span>
        </a-button>
      </a-col>
    </a-row>
  </div>
</template>

<script>

import * as dateUtils from '@/utils/date'
import moment from 'moment'

export default {
  name: 'QuotaUsage',
  props: {
    startDate: {
      type: Object,
      default: () => moment().startOf('month')
    },
    endDate: {
      type: Object,
      default: () => moment()
    },
    presetRanges: {
      type: Object,
      default: () => {}
    },
    dateLimit: {
      type: Date,
      default: () => new Date()
    }
  },
  data () {
    return {
      pattern: dateUtils.formats.ISO_DATE_ONLY,
      showFilterQuotaDataByPeriodModal: false,
      form: this.$form.createForm(this),
      dates: [],
      presetDateRanges: {
        [this.$t('label.quota.filter.preset.thismonth')]: [moment().startOf('month'), moment()],
        [this.$t('label.quota.filter.preset.lastmonth')]: [moment().subtract(1, 'months').startOf('month'), moment().subtract(1, 'months').endOf('month')],
        [this.$t('label.quota.filter.preset.thisyear')]: [moment().startOf('year'), moment()],
        [this.$t('label.quota.filter.preset.lastyear')]: [moment().subtract(1, 'years').startOf('year'), moment().subtract(1, 'years').endOf('year')],
        ...this.presetRanges
      }
    }
  },
  methods: {
    openFilterQuotaDataByPeriodModal () {
      this.showFilterQuotaDataByPeriodModal = true
    },
    closeFilterQuotaDataByPeriodModal () {
      this.showFilterQuotaDataByPeriodModal = false
    },
    getPeriodToString () {
      return this.$t('label.quota.filter.period', { startDate: dateUtils.formatDateToExtended(this.startDate), endDate: dateUtils.formatDateToExtended(this.endDate) })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }

        if (values.dates) {
          this.$emit('fetchData', values.dates[0], values.dates[1])
        }

        this.closeFilterQuotaDataByPeriodModal()
      })
    },
    getDisabledDates (current) {
      return current > this.dateLimit
    }
  }
}
</script>
