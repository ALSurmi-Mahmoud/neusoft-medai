<template>
  <el-card class="stat-card" :body-style="{ padding: '15px' }">
    <div class="stat-content">
      <div class="stat-header">
        <span class="stat-title">{{ title }}</span>
        <el-icon :size="20" :color="color">
          <component :is="iconComponent" />
        </el-icon>
      </div>
      <div class="stat-value">{{ formattedValue }}</div>
      <div class="stat-footer">
        <span class="stat-label">{{ label }}</span>
        <span v-if="trend !== undefined" class="stat-trend" :class="trendClass">
          <el-icon :size="12">
            <component :is="trendIcon" />
          </el-icon>
          {{ Math.abs(trend) }}%
        </span>
      </div>
    </div>
  </el-card>
</template>

<script>
import { computed } from 'vue'
import {
  User,
  Document,
  Files,
  Checked,
  Warning,
  Calendar,
  ArrowUp,
  ArrowDown,
  Minus
} from '@element-plus/icons-vue'

export default {
  name: 'StatCard',
  components: {
    User,
    Document,
    Files,
    Checked,
    Warning,
    Calendar,
    ArrowUp,
    ArrowDown,
    Minus
  },
  props: {
    title: {
      type: String,
      required: true
    },
    value: {
      type: [Number, String],
      required: true
    },
    label: {
      type: String,
      default: ''
    },
    trend: {
      type: Number,
      default: undefined
    },
    icon: {
      type: String,
      default: 'Document'
    },
    color: {
      type: String,
      default: '#409EFF'
    }
  },
  setup(props) {
    const iconComponent = computed(() => {
      const icons = {
        User,
        Document,
        Files,
        Checked,
        Warning,
        Calendar
      }
      return icons[props.icon] || Document
    })

    const trendClass = computed(() => {
      if (props.trend === undefined || props.trend === 0) return 'trend-neutral'
      return props.trend > 0 ? 'trend-up' : 'trend-down'
    })

    const trendIcon = computed(() => {
      if (props.trend === undefined || props.trend === 0) return Minus
      return props.trend > 0 ? ArrowUp : ArrowDown
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
.stat-card {
  height: 100%;
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin: 10px 0;
}

.stat-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.stat-label {
  color: #909399;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-weight: 600;
}

.trend-up {
  color: #67c23a;
}

.trend-down {
  color: #f56c6c;
}

.trend-neutral {
  color: #909399;
}
</style>