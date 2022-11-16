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
  <a-spin :spinning="loading">
    <a-form
      class="form"
      :form="form"
      @submit="handleSubmit"
      v-ctrl-enter="handleSubmit"
      layout="vertical">
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        <a-input
          v-decorator="['name', {
            rules: [{ required: true, message: $t('message.action.quota.tariff.create.error.namerequired') }]
          }]"
          :placeholder="$t('placeholder.quota.tariff.name')"
          auto-focus
          max-length="65535" />
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.description')" :tooltip="apiParams.description.description"/>
        <a-textarea v-decorator="['description', {}]" :placeholder="$t('placeholder.quota.tariff.description')" auto-size max-length="65535" />
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.quota.type.name')" :tooltip="apiParams.usagetype.description"/>
        <a-select
          show-search
          v-decorator="['usageType', {
            rules: [{ required: true, message: $t('message.action.quota.tariff.create.error.usagetyperequired') }]
          }]"
          :placeholder="$t('placeholder.quota.tariff.usagetype')"
        >
          <a-select-option v-for="quotaType of getQuotaTypes()" :value="`${quotaType.id}-${quotaType.type}`" :key="quotaType.id">
            {{ $t(quotaType.type) }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.quota.tariff.value')" :tooltip="apiParams.value.description"/>
        <a-input-number
          v-decorator="['value', {
            rules: [{ required: true, message: $t('message.action.quota.tariff.create.error.valuerequired') }]
          }]"
          :placeholder="$t('placeholder.quota.tariff.value')" />
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.quota.tariff.activationrule')" :tooltip="apiParams.activationrule.description"/>
        <a-textarea v-decorator="['activationRule', {}]" :placeholder="$t('placeholder.quota.tariff.activationrule')" auto-size max-length="65535" />
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.quota.startdate')" :tooltip="apiParams.startdate.description"/>
        <a-date-picker :disabled-date="disabledStartDate" v-decorator="['startDate', {}]" :placeholder="$t('placeholder.quota.tariff.startdate')" />
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.quota.enddate')" :tooltip="apiParams.enddate.description"/>
        <a-date-picker :disabled-date="disabledEndDate" v-decorator="['endDate', {}]" :placeholder="$t('placeholder.quota.tariff.enddate')" />
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { getIsImplementedQuotaTypes } from '@/utils/quota'
import { formats, moment } from '@/utils/date'

export default {
  name: 'CreateQuotaTariff',
  components: {
    ResourceIcon,
    TooltipLabel
  },
  data () {
    return {
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('quotaTariffCreate')
  },
  mounted () {
    this.form.setFieldsValue({ value: 0 })
  },
  inject: ['parentFetchData'],
  methods: {
    handleSubmit (e) {
      if (this.loading) return
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }

        values.usageType = values.usageType.split('-')[0]

        if (values.startDate) {
          values.startDate = values.startDate.format(formats.ISO_DATE_ONLY)
        }

        if (values.endDate) {
          values.endDate = values.endDate.format(formats.ISO_DATE_ONLY)
        }

        this.loading = true
        api('quotaTariffCreate', values).then(response => {
          this.$message.success(this.$t('message.quota.tariff.create.success', { quotaTariff: values.name }))
          this.parentFetchData()
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    getQuotaTypes () {
      return getIsImplementedQuotaTypes()
    },
    disabledStartDate (current) {
      return current < moment().startOf('day')
    },
    disabledEndDate (current) {
      return current < (this.form.getFieldValue('startDate') || moment().startOf('day'))
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/objects/form.scss';
</style>
