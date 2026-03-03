<template>
  <div class="system-analytics">
    <!-- Show warning for non-admin users -->
    <el-alert
        v-if="!isAdmin"
        title="Admin Access Required"
        type="warning"
        description="System Analytics is only available for Administrator users. Please contact your system administrator if you need access."
        :closable="false"
        show-icon
        style="margin: 20px;"
    />

    <!-- Admin content -->
    <el-card v-else>
      <template #header>
        <div class="header-content">
          <h2>System Analytics</h2>
          <el-button type="primary" @click="refreshData" :loading="loading" :icon="Refresh">
            Refresh
          </el-button>
        </div>
      </template>

      <!-- System Health Status -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="24" :sm="6">
          <StatCard
              title="System Uptime"
              :value="systemHealth.uptime + '%'"
              label="Last 30 days"
              icon="Checked"
              color="#67C23A"
          />
        </el-col>
        <el-col :xs="24" :sm="6">
          <StatCard
              title="API Response Time"
              :value="systemHealth.responseTime + 'ms'"
              label="Average"
              icon="Calendar"
              color="#409EFF"
          />
        </el-col>
        <el-col :xs="24" :sm="6">
          <StatCard
              title="Error Rate"
              :value="systemHealth.errorRate + '%'"
              label="Last 24 hours"
              icon="Warning"
              color="#E6A23C"
          />
        </el-col>
        <el-col :xs="24" :sm="6">
          <StatCard
              title="Active Users"
              :value="systemHealth.activeUsers"
              label="Currently online"
              icon="User"
              color="#F56C6C"
          />
        </el-col>
      </el-row>

      <!-- Performance Gauges -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :xs="24" :md="8">
          <el-card>
            <template #header>CPU Usage</template>
            <GaugeChart :value="performance.cpu" height="250px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-card>
            <template #header>Memory Usage</template>
            <GaugeChart :value="performance.memory" height="250px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-card>
            <template #header>Storage Usage</template>
            <GaugeChart :value="performance.storage" height="250px" />
          </el-card>
        </el-col>
      </el-row>

      <!-- Request Analytics -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>API Request Volume</template>
            <LineChart :data="requestVolume" height="350px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Request Distribution</template>
            <PieChart :data="requestDistribution" height="350px" />
          </el-card>
        </el-col>
      </el-row>

      <!-- User Activity Heatmap -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :xs="24">
          <el-card>
            <template #header>User Activity Heatmap (Last 7 Days)</template>
            <HeatmapChart
                :data="activityHeatmap.data"
                :xAxisData="activityHeatmap.hours"
                :yAxisData="activityHeatmap.days"
                height="300px"
            />
          </el-card>
        </el-col>
      </el-row>

      <!-- System Logs -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :xs="24">
          <el-card>
            <template #header>Recent System Logs</template>
            <el-table :data="systemLogs" max-height="400" stripe>
              <el-table-column prop="timestamp" label="Time" width="180">
                <template #default="scope">
                  {{ formatDateTime(scope.row.timestamp) }}
                </template>
              </el-table-column>
              <el-table-column prop="level" label="Level" width="100">
                <template #default="scope">
                  <el-tag :type="getLogType(scope.row.level)">
                    {{ scope.row.level }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="component" label="Component" width="150" />
              <el-table-column prop="message" label="Message" show-overflow-tooltip />
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import http from '@/utils/http'
import StatCard from '@/components/charts/StatCard.vue'
import GaugeChart from '@/components/charts/GaugeChart.vue'
import LineChart from '@/components/charts/LineChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import HeatmapChart from '@/components/charts/HeatmapChart.vue'

export default {
  name: 'SystemAnalytics',
  components: {
    StatCard,
    GaugeChart,
    LineChart,
    PieChart,
    HeatmapChart
  },
  setup() {
    const loading = ref(false)
    let refreshInterval = null

    // ✅ CHECK USER ROLE
    const isAdmin = computed(() => {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      return user.role === 'ADMIN'
    })

    const systemHealth = ref({
      uptime: 99.5,
      responseTime: 120,
      errorRate: 0.3,
      activeUsers: 0
    })

    const performance = ref({
      cpu: 45,
      memory: 62,
      storage: 38
    })

    const requestVolume = ref({ dates: [], values: [] })
    const requestDistribution = ref([])
    const activityHeatmap = ref({
      data: [],
      hours: [],
      days: []
    })
    const systemLogs = ref([])

    const formatDateTime = (timestamp) => {
      if (!timestamp) return ''
      return new Date(timestamp).toLocaleString()
    }

    const getLogType = (level) => {
      const types = {
        'ERROR': 'danger',
        'WARN': 'warning',
        'INFO': 'info',
        'DEBUG': 'success'
      }
      return types[level] || 'info'
    }

    const loadData = async () => {
      // ✅ ONLY LOAD IF ADMIN
      if (!isAdmin.value) {
        return
      }

      loading.value = true
      try {
        await Promise.all([
          loadSystemHealth(),
          loadPerformance(),
          loadRequestMetrics(),
          loadActivityHeatmap(),
          loadSystemLogs()
        ])
      } catch (error) {
        console.error('Failed to load system analytics:', error)
        // ✅ BETTER ERROR HANDLING
        if (error.response?.status === 403) {
          ElMessage.warning('System analytics requires administrator access')
        } else {
          ElMessage.error('Failed to load system analytics')
        }
      } finally {
        loading.value = false
      }
    }

    const loadSystemHealth = async () => {
      try {
        const response = await http.get('/analytics/system/health')
        const data = response.data
        systemHealth.value = {
          uptime: data.uptime || 99.5,
          responseTime: data.avgResponseTime || 120,
          errorRate: data.errorRate || 0.3,
          activeUsers: data.activeUsers || 0
        }
      } catch (error) {
        console.error('Failed to load system health:', error)
      }
    }

    const loadPerformance = async () => {
      try {
        const response = await http.get('/analytics/system/performance')
        const data = response.data
        performance.value = {
          cpu: data.cpuUsage || 45,
          memory: data.memoryUsage || 62,
          storage: data.storageUsage || 38
        }
      } catch (error) {
        console.error('Failed to load performance:', error)
      }
    }

    const loadRequestMetrics = async () => {
      try {
        const response = await http.get('/analytics/system/requests')
        const data = response.data

        // Volume trends
        if (data.volume) {
          requestVolume.value = {
            dates: data.volume.map(d => d.date),
            values: data.volume.map(d => d.count)
          }
        }

        // Distribution by endpoint
        if (data.distribution) {
          requestDistribution.value = Object.entries(data.distribution).map(([name, value]) => ({
            name,
            value
          }))
        }
      } catch (error) {
        console.error('Failed to load request metrics:', error)
      }
    }

    const loadActivityHeatmap = async () => {
      try {
        const response = await http.get('/analytics/users/heatmap')
        const data = response.data

        activityHeatmap.value = {
          data: data.data || [],
          hours: data.hours || Array.from({ length: 24 }, (_, i) => `${i}:00`),
          days: data.days || ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
        }
      } catch (error) {
        console.error('Failed to load activity heatmap:', error)
        // Generate sample data
        const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
        const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
        const data = []
        days.forEach((day, dayIdx) => {
          hours.forEach((hour, hourIdx) => {
            const value = Math.floor(Math.random() * 50)
            data.push([hourIdx, dayIdx, value])
          })
        })
        activityHeatmap.value = { data, hours, days }
      }
    }

    const loadSystemLogs = async () => {
      try {
        const response = await http.get('/analytics/system/logs', {
          params: { limit: 50 }
        })
        systemLogs.value = response.data || []
      } catch (error) {
        console.error('Failed to load system logs:', error)
        systemLogs.value = []
      }
    }

    const refreshData = () => {
      loadData()
    }

    onMounted(() => {
      // ✅ ONLY LOAD AND AUTO-REFRESH IF ADMIN
      if (isAdmin.value) {
        loadData()
        // Auto-refresh every 30 seconds
        refreshInterval = setInterval(() => {
          loadData()
        }, 30000)
      }
    })

    onUnmounted(() => {
      if (refreshInterval) {
        clearInterval(refreshInterval)
      }
    })

    return {
      loading,
      isAdmin, // ✅ EXPOSE TO TEMPLATE
      systemHealth,
      performance,
      requestVolume,
      requestDistribution,
      activityHeatmap,
      systemLogs,
      refreshData,
      formatDateTime,
      getLogType,
      Refresh
    }
  }
}
</script>

<style scoped>
.system-analytics {
  padding: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h2 {
  margin: 0;
  font-size: 24px;
}

.stats-row {
  margin-bottom: 20px;
}
</style>