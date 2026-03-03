<template>
  <v-chart :option="option" :style="{ height }" autoresize />
</template>

<script>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
])

export default {
  name: 'LineChart',
  components: { VChart },
  props: {
    data: {
      type: Object,
      default: () => ({ dates: [], values: [] })
    },
    height: {
      type: String,
      default: '400px'
    },
    title: {
      type: String,
      default: ''
    },
    color: {
      type: String,
      default: '#409EFF'
    }
  },
  setup(props) {
    const option = computed(() => ({
      title: {
        text: props.title,
        left: 'center',
        textStyle: {
          fontSize: 14,
          fontWeight: 'normal'
        }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: props.data.dates || []
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          data: props.data.values || [],
          type: 'line',
          smooth: true,
          areaStyle: {
            opacity: 0.3
          },
          itemStyle: {
            color: props.color
          }
        }
      ]
    }))

    return {
      option
    }
  }
}
</script>

<style scoped>
/* Chart container styling handled by parent */
</style>