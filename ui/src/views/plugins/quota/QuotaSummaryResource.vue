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
  <resource-view
    :loading="loading"
    :resource="quotaResource"
    :tabs="tabs"
    :historyTab="activeTab"
    @onTabChange="(tab) => { this.activeTab = tab }"/>
</template>

<script>
import { api } from '@/api'

import ResourceView from '@/components/view/ResourceView'

export default {
  name: 'QuotaSummaryResource',
  components: {
    ResourceView
  },
  props: {
    resource: {
      type: Object,
      default: () => {}
    },
    tabs: {
      type: Array,
      default: () => []
    }
  },
  data () {
    return {
      loading: false,
      quotaResource: {},
      networkService: null,
      pattern: 'YYYY-MM-DD',
      activeTab: ''
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource () {
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      const params = {}
      if (Object.keys(this.$route.query).length > 0) {
        Object.assign(params, this.$route.query)
      }

      params.listAll = true
      this.loading = true

      api('quotaSummary', params).then(json => {
        const quotaSummary = json.quotasummaryresponse.summary[0] || {}
        if (Object.keys(quotaSummary).length > 0) {
          quotaSummary.currentbalance = quotaSummary.balance
          quotaSummary.account = this.$route.params.id ? this.$route.params.id : null
          quotaSummary.domainid = this.$route.query.domainid ? this.$route.query.domainid : null
        }
        this.quotaResource = Object.assign({}, this.quotaResource, quotaSummary)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
</style>
