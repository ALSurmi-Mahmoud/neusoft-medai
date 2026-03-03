<template>
  <v-chart :option="option" :style="{ height }" autoresize />
</template>

<script>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GaugeChart } from 'echarts/charts'
import {
  TooltipComponent,
  TitleComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  GaugeChart,
  TooltipComponent,
  TitleComponent
])

export default {
  name: 'GaugeChart',
  components: { VChart },
  props: {
    value: {
      type: Number,
      default: 0
    },
    height: {
      type: String,
      default: '300px'
    },
    title: {
      type: String,
      default: ''
    },
    max: {
      type: Number,
      default: 100
    },
    unit: {
      type: String,
      default: '%'
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
        formatter: '{a} <br/>{b} : {c}' + props.unit
      },
      series: [
        {
          name: props.title,
          type: 'gauge',
          max: props.max,
          progress: {
            show: true,
            width: 18
          },
          axisLine: {
            lineStyle: {
              width: 18
            }
          },
          axisTick: {
            show: false
          },
          splitLine: {
            length: 15,
            lineStyle: {
              width: 2,
              color: '#999'
            }
          },
          axisLabel: {
            distance: 25,
            color: '#999',
            fontSize: 12
          },
          anchor: {
            show: true,
            showAbove: true,
            size: 18,
            itemStyle: {
              borderWidth: 8
            }
          },
          detail: {
            valueAnimation: true,
            fontSize: 24,
            offsetCenter: [0, '70%'],
            formatter: '{value}' + props.unit
          },
          data: [
            {
              value: props.value
            }
          ]
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