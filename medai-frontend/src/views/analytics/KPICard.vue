<template>
  <el-card class="kpi-card" :body-style="{ padding: '20px' }">
    <div class="kpi-content">
      <div class="kpi-icon" :style="{ backgroundColor: color + '20', color: color }">
        <el-icon :size="32"><component :is="iconComponent" /></el-icon>
      </div>
      <div class="kpi-data">
        <div class="kpi-title">{{ title }}</div>
        <div class="kpi-value">{{ formattedValue }}</div>
        <div class="kpi-trend" :class="trendClass">
          <el-icon><component :is="trendIcon" /></el-icon>
          {{ Math.abs(trend) }}%
        </div>
      </div>
    </div>
  </el-card>
</template>

<script>
import { computed } from 'vue'
import { User, Document, Files, Checked, ArrowUp, ArrowDown } from '@element-plus/icons-vue'

export default {
  name: 'KPICard',
  components: { User, Document, Files, Checked, ArrowUp, ArrowDown },
  props: {
    title: String,
    value: [Number, String],
    trend: Number,
    icon: String,
    color: String
  },
  setup(props) {
    const iconComponent = computed(() => {
      const icons = { User, Document, Files, Checked }
      return icons[props.icon] || User
    })

    const trendClass = computed(() => {
      return props.trend >= 0 ? 'trend-up' : 'trend-down'
    })

    const trendIcon = computed(() => {
      return props.trend >= 0 ? ArrowUp : ArrowDown
    })

    const formattedValue = computed(() => {
      if (typeof props.value === 'number') {
        return props.value.toLocaleString()
      }
      return props.value
    })

    return {
      iconComponent,
      trendClass,
      trendIcon,
      formattedValue
    }
  }
}
</script>

<style scoped>
.kpi-card {
  height: 100%;
}

.kpi-content {
  display: flex;
  align-items: center;
}

.kpi-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.kpi-data {
  flex: 1;
}

.kpi-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.kpi-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.kpi-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
}

.trend-up {
  color: #67C23A;
}

.trend-down {
  color: #F56C6C;
}
</style>