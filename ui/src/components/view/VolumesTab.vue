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
  <a-table
    class="table"
    size="small"
    :columns="volumeColumns"
    :dataSource="volumes"
    :rowKey="item => item.id"
    :pagination="false"
  >
    <template slot="name" slot-scope="text, item">
      <a-icon type="hdd" />
      <router-link :to="{ path: '/volume/' + item.id }">
        {{ text }}
      </router-link>
      <a-tag v-if="item.provisioningtype">
        {{ item.provisioningtype }}
      </a-tag>
    </template>
    <template slot="state" slot-scope="text">
      <status :text="text ? text : ''" />{{ text }}
    </template>
    <template slot="size" slot-scope="text, item">
      {{ parseFloat(item.size / (1024.0 * 1024.0 * 1024.0)).toFixed(2) }} GB
    </template>
  </a-table>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'VolumesTab',
  components: {
    Status
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    items: {
      type: Array,
      default: () => []
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      vm: {},
      volumes: [],
      volumeColumns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name',
          scopedSlots: { customRender: 'name' }
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state',
          scopedSlots: { customRender: 'state' }
        },
        {
          title: this.$t('label.type'),
          dataIndex: 'type'
        },
        {
          title: this.$t('label.size'),
          dataIndex: 'size',
          scopedSlots: { customRender: 'size' }
        }
      ]
    }
  },
  created () {
    this.vm = this.resource
    this.fetchData()
  },
  watch: {
    resource: function (newItem) {
      this.vm = newItem
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      this.volumes = []
      if (!this.vm?.id) {
        return
      }
      if (this.items.length) {
        this.volumes = this.items
      } else {
        this.getVolumes()
      }
    },
    getVolumes () {
      api('listVolumes', { listall: true, virtualmachineid: this.vm.id }).then(json => {
        this.volumes = json.listvolumesresponse.volume
        if (this.volumes) {
          this.volumes.sort((a, b) => { return a.deviceid - b.deviceid })
        }
        this.$set(this.resource, 'volumes', this.volumes)
      })
    }
  }
}
</script>
