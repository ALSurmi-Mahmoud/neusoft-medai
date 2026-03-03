<template>
  <v-chart :option="option" :style="{ height }" autoresize />
</template>

<script>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { HeatmapChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  VisualMapComponent,
  TitleComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  HeatmapChart,
  GridComponent,
  TooltipComponent,
  VisualMapComponent,
  TitleComponent
])

export default {
  name: 'HeatmapChart',
  components: { VChart },
  props: {
    data: {
      type: Array,
      default: () => []
    },
    xAxisData: {
      type: Array,
      default: () => []
    },
    yAxisData: {
      type: Array,
      default: () => []
    },
    height: {
      type: String,
      default: '400px'
    },
    title: {
      type: String,
      default: ''
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
        position: 'top',
        formatter: function (params) {
          return (
              props.yAxisData[params.data[1]] +
              ' - ' +
              props.xAxisData[params.data[0]] +
              ': ' +
              params.data[2]
          )
        }
      },
      grid: {
        left: '10%',
        right: '10%',
        top: '15%',
        bottom: '10%'
      },
      xAxis: {
        type: 'category',
        data: props.xAxisData,
        splitArea: {
          show: true
        }
      },
      yAxis: {
        type: 'category',
        data: props.yAxisData,
        splitArea: {
          show: true
        }
      },
      visualMap: {
        min: 0,
        max: Math.max(...props.data.map(d => d[2]), 10),
        calculable: true,
        orient: 'horizontal',
        left: 'center',
        bottom: '0%',
        inRange: {
          color: ['#ebedf0', '#c6e48b', '#7bc96f', '#239a3b', '#196127']
        }
      },
      series: [
        {
          name: 'Activity',
          type: 'heatmap',
          data: props.data,
          label: {
            show: false
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
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