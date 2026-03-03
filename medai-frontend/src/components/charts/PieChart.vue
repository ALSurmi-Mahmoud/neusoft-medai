<template>
  <v-chart :option="option" :style="{ height }" autoresize />
</template>

<script>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import {
  TooltipComponent,
  LegendComponent,
  TitleComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  PieChart,
  TooltipComponent,
  LegendComponent,
  TitleComponent
])

export default {
  name: 'PieChart',
  components: { VChart },
  props: {
    data: {
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
    },
    donut: {
      type: Boolean,
      default: false
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
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'middle'
      },
      series: [
        {
          name: props.title,
          type: 'pie',
          radius: props.donut ? ['40%', '70%'] : '70%',
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            formatter: '{b}: {d}%'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '16',
              fontWeight: 'bold'
            }
          },
          data: props.data || []
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