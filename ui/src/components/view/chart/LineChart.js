import { Line } from 'vue-chartjs'
import * as chartUtils from '@/utils/chart'

export default {
  extends: Line,
  props: {
    datasets: {
      type: Object,
      default () {
        return {}
      }
    },
    options: {
      type: Object,
      default () {
        return {}
      }
    }
  },

  mounted () {
    this.renderChart(
      this.datasets,
      {
        ...chartUtils.defaultChartOptions,
        ...this.options
      }
    )
  }
}
