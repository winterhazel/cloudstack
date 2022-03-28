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
  <a-form class="form" :form="form" layout="vertical" @submit="submitTariff">
    <a-form-item :label="$t('label.description')">
      <a-input auto-focus v-decorator="['description', { initialValue: resource.description }]" max-length="65535" />
    </a-form-item>
    <a-form-item :label="$t('label.quota.tariff.value')">
      <a-input-number v-decorator="['value', { initialValue: resource.tariffValue }]" />
    </a-form-item>
    <a-form-item :label="$t('label.quota.tariff.activationrule')">
      <a-textarea v-decorator="['activationRule', { initialValue: resource.activationRule }]" auto-size max-length="65535" />
    </a-form-item>
    <a-form-item :label="$t('label.quota.enddate')">
      <a-date-picker :disabled-date="disabledDate" v-decorator="['endDate', { initialValue: resource.endDate }]" />
    </a-form-item>
    <div :span="24" class="action-button">
      <a-button @click="onClose">{{ $t('label.cancel') }}</a-button>
      <a-button type="primary" ref="submit" @click="submitTariff">{{ $t('label.ok') }}</a-button>
    </div>
  </a-form>
</template>

<script>
import { api } from '@/api'
import moment from 'moment'
import { formats } from '@/utils/date'

export default {
  name: 'EditTariffValueWizard',
  props: {
    showAction: {
      type: Boolean,
      default: () => false
    },
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      pattern: formats.ISO_DATE_ONLY
    }
  },
  inject: ['parentFetchData'],
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    onClose () {
      this.$emit('close-action')
    },
    submitTariff (e) {
      e.preventDefault()
      if (this.loading) return
      this.form.validateFields((error, values) => {
        if (error) return

        const params = {
          name: this.resource.name
        }

        if (this.form.isFieldTouched('description')) {
          params.description = values.description
        }

        if (this.form.isFieldTouched('value')) {
          params.value = values.value
        }

        if (this.form.isFieldTouched('activationRule')) {
          params.activationRule = values.activationRule
        }

        if (this.form.isFieldTouched('endDate')) {
          params.enddate = values.endDate.format(formats.ISO_DATE_ONLY)
        }

        if (!this.form.isFieldsTouched()) {
          this.onClose()
          return
        }

        this.loading = true

        api('quotaTariffUpdate', {}, 'POST', params).then(json => {
          const tariffResponse = json.quotatariffupdateresponse.quotatariff || {}

          if (tariffResponse.uuid && this.$route.params.id) {
            this.$router.push(`/quotatariff/${tariffResponse.uuid}`)
          } else if (Object.keys(tariffResponse).length > 0) {
            this.parentFetchData()
          }

          this.$message.success(this.$t('message.quota.tariff.updated', { tariff: this.resource.name }))
          this.onClose()
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }).finally(() => {
          this.loading = false
        })
      })
    },
    disabledDate (current) {
      return current < moment().startOf('day')
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/objects/form.scss';
</style>
