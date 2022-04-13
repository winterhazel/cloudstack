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

import { Line } from 'vue-chartjs'

export default {
  extends: Line,
  name: 'MultiLineChart',
  props: {
    chartLabels: {
      type: Array,
      default: () => []
    },
    chartData: {
      type: Array,
      default: null
    },
    options: {
      type: Object,
      default: null
    }
  },
  mounted () {
    this.renderChart(this.prepareData(), this.options)
  },
  methods: {
    prepareData () {
      const datasetList = []
      for (const element of this.chartData) {
        datasetList.push(
          {
            backgroundColor: element.backgroundColor,
            borderColor: element.borderColor,
            borderWidth: 3,
            label: element.label,
            data: element.data.map(d => d.stat),
            hidden: this.hideLine(element.data.map(d => d.stat)),
            pointRadius: element.pointRadius
          }
        )
      }
      return {
        labels: this.chartLabels,
        datasets: datasetList
      }
    },
    hideLine (data) {
      for (const d of data) {
        if (d < 0) {
          return true
        }
      }
      return false
    }
  },
  watch: {
    chartData: function () {
      this._data._chart.destroy()
      this.renderChart(this.prepareData(), this.options)
    }
  }
}
