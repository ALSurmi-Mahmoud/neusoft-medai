<template>
  <v-chart :option="option" :style="{ height }" autoresize />
</template>

<script>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  BarChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
])

export default {
  name: 'BarChart',
  components: { VChart },
  props: {
    data: {
      type: Object,
      default: () => ({ categories: [], values: [] })
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
      default: '#67C23A'
    },
    horizontal: {
      type: Boolean,
      default: false
    }
  },
  setup(props) {
    const option = computed(() => {
      const baseOption = {
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
            type: 'shadow'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        }
      }

      if (props.horizontal) {
        return {
          ...baseOption,
          xAxis: {
            type: 'value'
          },
          yAxis: {
            type: 'category',
            data: props.data.categories || []
          },
          series: [
            {
              data: props.data.values || [],
              type: 'bar',
              itemStyle: {
                color: props.color
              }
            }
          ]
        }
      } else {
        return {
          ...baseOption,
          xAxis: {
            type: 'category',
            data: props.data.categories || []
          },
          yAxis: {
            type: 'value'
          },
          series: [
            {
              data: props.data.values || [],
              type: 'bar',
              itemStyle: {
                color: props.color
              }
            }
          ]
        }
      }
    })

    return {
      option
    }
  }
}
</script>

<style scoped>
/* Chart container styling handled by parent */
</style>