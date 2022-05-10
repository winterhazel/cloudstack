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
      <a-form-item v-if="'listDomains' in $store.getters.apis">
        <tooltip-label slot="label" :title="$t('label.domain')" :tooltip="apiParams.domainid.description"/>
        <a-select
          v-decorator="['domainid', {
            rules: [{ required: true, message: $t('message.action.quota.credit.add.error.domainidrequired') }]
          }]"
          showSearch
          :loading="domainLoading"
          :placeholder="this.$t('label.domainid')"
          @change="val => { this.handleDomainChange(val) }">
          <a-select-option v-for="domain in this.domainList" :value="`${domain.id}|${domain.path}`" :key="domain.id">
            {{ domain.path || domain.name || domain.description }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item v-if="'listDomains' in $store.getters.apis">
        <tooltip-label slot="label" :title="$t('label.account')" :tooltip="apiParams.account.description"/>
        <a-select
          v-decorator="['account', {
            rules: [{ required: true, message: $t('message.action.quota.credit.add.error.accountrequired') }]
          }]"
          showSearch
          :placeholder="this.$t('label.account')">
          <a-select-option v-for="account in accountList" :value="account.name" :key="account.id">
            {{ account.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.value')" :tooltip="apiParams.value.description"/>
        <a-input-number
          v-decorator="['value', {
            rules: [{ required: true, message: $t('message.action.quota.credit.add.error.valuerequired') }]
          }]"
          :placeholder="$t('placeholder.quota.credit.add.value')" />
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.min_balance')" :tooltip="apiParams.min_balance.description"/>
        <a-input-number
          v-decorator="['min_balance', {
            rules: [{ required: true, message: $t('message.action.quota.credit.add.error.minbalancerequired') }]
          }]"
          :placeholder="$t('placeholder.quota.credit.add.min_balance')" />
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.quota.enforce')" :tooltip="apiParams.quota_enforce.description"/>
        <a-switch v-decorator="['quota_enforce', {}]" />
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
import DedicateDomain from '@/components/view/DedicateDomain'

export default {
  name: 'CreateQuotaTariff',
  components: {
    ResourceIcon,
    TooltipLabel,
    DedicateDomain
  },
  data () {
    return {
      loading: false,
      domainList: [],
      accountList: [],
      domainId: null,
      domainLoading: false,
      domainError: false
    }
  },
  inject: ['parentFetchData'],
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('quotaCredits')
  },
  created () {
    this.fetchData()
  },
  methods: {
    handleSubmit (e) {
      if (this.loading) return

      this.form.validateFields((err, values) => {
        if (err) {
          return
        }

        values.domainid = this.domainId

        this.loading = true
        api('quotaCredits', values).then(response => {
          this.$message.success(this.$t('message.action.quota.credit.add.success', { credit: values.value, account: values.account }))
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
    handleDomainChange (domain) {
      this.domainId = domain?.split('|')[0]
      if ('listAccounts' in this.$store.getters.apis) {
        this.fetchAccounts()
      }
    },
    fetchData () {
      if ('listDomains' in this.$store.getters.apis) {
        this.fetchDomains()
      }
    },
    fetchDomains () {
      this.domainLoading = true
      api('listDomains', {
        listAll: true,
        details: 'min'
      }).then(response => {
        this.domainList = response.listdomainsresponse.domain

        if (this.domainList[0]) {
          this.handleDomainChange(null)
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.domainLoading = false
      })
    },
    fetchAccounts () {
      api('listAccounts', {
        domainid: this.domainId
      }).then(response => {
        this.accountList = response.listaccountsresponse.account || []
      }).catch(error => {
        this.$notifyError(error)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/objects/form.scss';
</style>
