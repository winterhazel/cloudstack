// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

<template>
  <a-form :form="form" class="form-layout" @submit="handleSubmit">
    <div v-show="!(!showStartDate || !showEndDate) || (!showStartDate && !showEndDate)">
      <a-form-item :label="$t('label.all.available.data')">
        <a-switch @change="onToggleAllData"/>
      </a-form-item>
      <div v-show="showAllDataAlert">
        <a-alert :message="$t('message.alert.show.all.stats.data')" banner />
      </div>
    </div>
    <div v-show="showStartDate">
      <a-form-item :label="$t('label.only.start.date.and.time')">
        <a-switch @change="onToggleStartDate"/>
      </a-form-item>
      <a-form-item :label="$t('label.start.date.and.time')">
        <a-date-picker
          v-decorator="['startDate', { rules: [{ required: showStartDate, message: `${this.$t('message.error.start.date.and.time')}` }] }]"
          show-time
          :placeholder="$t('message.select.start.date.and.time')"/>
      </a-form-item>
    </div>
    <div v-show="showEndDate">
      <a-form-item :label="$t('label.only.end.date.and.time')">
        <a-switch @change="onToggleEndDate"/>
      </a-form-item>
      <a-form-item :label="$t('label.end.date.and.time')">
        <a-date-picker
          v-decorator="['endDate', { rules: [{ required: showEndDate, message: `${this.$t('message.error.end.date.and.time')}` }] }]"
          show-time
          :placeholder="$t('message.select.end.date.and.time')"/>
      </a-form-item>
    </div>
    <div :span="24" class="action-button">
      <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
      <a-button ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
    </div>
  </a-form>
</template>

<script>
export default {
  name: 'FilterStats',
  data () {
    return {
      formLayout: 'vertical',
      form: this.$form.createForm(this),
      startDate: null,
      endDate: null,
      showAllDataAlert: false,
      showAllData: true,
      showStartDate: true,
      showEndDate: true
    }
  },
  methods: {
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.$emit('onSubmit', values)
      })
    },
    closeAction () {
      this.$emit('closeAction')
    },
    onToggleAllData () {
      this.showAllDataAlert = !this.showAllDataAlert
      if (this.showAllDataAlert) {
        this.showStartDate = false
        this.showEndDate = false
        this.form.setFieldsValue({
          startDate: null,
          endDate: null
        })
      } else {
        this.showStartDate = true
        this.showEndDate = true
        this.form.resetFields()
      }
    },
    onToggleStartDate () {
      this.showEndDate = !this.showEndDate
      if (this.showEndDate === false) {
        this.form.setFieldsValue({ endDate: null })
      } else {
        this.form.resetFields()
      }
    },
    onToggleEndDate () {
      this.showStartDate = !this.showStartDate
      if (this.showStartDate === false) {
        this.form.setFieldsValue({ startDate: null })
      } else {
        this.form.resetFields()
      }
    }
  }
}
</script>
