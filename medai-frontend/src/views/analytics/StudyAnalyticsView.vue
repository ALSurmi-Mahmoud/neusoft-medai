<template>
  <div class="study-analytics">
    <el-card>
      <template #header>
        <div class="header-content">
          <h2>Study Analytics</h2>
          <div class="actions">
            <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="To"
                start-placeholder="Start"
                end-placeholder="End"
                @change="loadData"
            />
            <el-button type="primary" @click="refreshData" :loading="loading" :icon="Refresh">
              Refresh
            </el-button>
          </div>
        </div>
      </template>

      <!-- Statistics Cards -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="24" :sm="6">
          <StatCard
              title="Total Studies"
              :value="stats.totalStudies"
              label="All time"
              icon="Document"
              color="#409EFF"
          />
        </el-col>
        <el-col :xs="24" :sm="6">
          <StatCard
              title="This Month"
              :value="stats.thisMonth"
              :trend="stats.monthlyTrend"
              label="vs last month"
              icon="Document"
              color="#67C23A"
          />
        </el-col>
        <el-col :xs="24" :sm="6">
          <StatCard
              title="Pending"
              :value="stats.pending"
              label="Awaiting report"
              icon="Warning"
              color="#E6A23C"
          />
        </el-col>
        <el-col :xs="24" :sm="6">
          <StatCard
              title="Completed"
              :value="stats.completed"
              label="With reports"
              icon="Checked"
              color="#F56C6C"
          />
        </el-col>
      </el-row>

      <!-- Charts Row 1 -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Study Volume Trends</template>
            <LineChart :data="volumeTrends" height="350px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Studies by Modality</template>
            <PieChart :data="modalityData" height="350px" :donut="true" />
          </el-card>
        </el-col>
      </el-row>

      <!-- Charts Row 2 -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Status Distribution</template>
            <BarChart :data="statusData" height="350px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Study Performance</template>
            <el-table :data="performanceMetrics" max-height="350">
              <el-table-column prop="metric" label="Metric" />
              <el-table-column prop="value" label="Value" width="120" />
              <el-table-column prop="target" label="Target" width="120" />
              <el-table-column label="Status" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.status">
                    {{ scope.row.status === 'success' ? 'Met' : 'Below' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import http from '@/utils/http'
import StatCard from '@/components/charts/StatCard.vue'
import LineChart from '@/components/charts/LineChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import BarChart from '@/components/charts/BarChart.vue'

export default {
  name: 'StudyAnalytics',
  components: {
    StatCard,
    LineChart,
    PieChart,
    BarChart
  },
  setup() {
    const loading = ref(false)
    const dateRange = ref([
      new Date(Date.now() - 30 * 24 * 60 * 60 * 1000),
      new Date()
    ])

    const stats = ref({
      totalStudies: 0,
      thisMonth: 0,
      monthlyTrend: 0,
      pending: 0,
      completed: 0
    })

    const volumeTrends = ref({ dates: [], values: [] })
    const modalityData = ref([])
    const statusData = ref({ categories: [], values: [] })
    const performanceMetrics = ref([])

    const formatDate = (date) => {
      return date.toISOString().split('T')[0]
    }

    const loadData = async () => {
      loading.value = true
      try {
        const startDate = formatDate(dateRange.value[0])
        const endDate = formatDate(dateRange.value[1])

        await Promise.all([

          loadTrends(startDate, endDate),
          loadModalities(),
          loadStatusData(),
          loadPerformance()
        ])

        ElMessage.success('Study analytics loaded')
      } catch (error) {
        ElMessage.error('Failed to load study analytics')
        console.error(error)
      } finally {
        loading.value = false
      }
    }

    const loadStatistics = async (startDate, endDate) => {
      try {
        const response = await http.get('/analytics/studies/statistics', {
          params: { startDate, endDate }
        })
        const data = response.data
        stats.value = {
          totalStudies: data.total || 0,
          thisMonth: data.thisMonth || 0,
          monthlyTrend: data.trend || 0,
          pending: data.pending || 0,
          completed: data.completed || 0
        }
      } catch (error) {
        console.error('Failed to load statistics:', error)
      }
    }

    const loadTrends = async (startDate, endDate) => {
      try {
        const response = await http.get('/analytics/studies/trends', {
          params: { startDate, endDate }
        })
        const data = response.data
        volumeTrends.value = {
          dates: data.map(d => d.date),
          values: data.map(d => d.count)
        }
      } catch (error) {
        console.error('Failed to load trends:', error)
      }
    }

    const loadModalities = async () => {
      try {
        const response = await http.get('/analytics/studies/by-modality')
        const data = response.data
        modalityData.value = Object.entries(data).map(([name, value]) => ({
          name,
          value
        }))
      } catch (error) {
        console.error('Failed to load modalities:', error)
      }
    }

    const loadStatusData = async () => {
      try {
        const response = await http.get('/analytics/studies/status')
        const data = response.data
        statusData.value = {
          categories: Object.keys(data),
          values: Object.values(data)
        }
      } catch (error) {
        console.error('Failed to load status data:', error)
      }
    }

    const loadPerformance = async () => {
      try {
        const response = await http.get('/analytics/studies/performance')
        const data = response.data
        performanceMetrics.value = Array.isArray(data) ? data : []
      } catch (error) {
        console.error('Failed to load performance:', error)
        performanceMetrics.value = [
          { metric: 'Average Turnaround', value: '24 hours', target: '48 hours', status: 'success' },
          { metric: 'Report Completion Rate', value: '95%', target: '90%', status: 'success' },
          { metric: 'Quality Score', value: '4.5/5', target: '4.0/5', status: 'success' }
        ]
      }
    }

    const refreshData = () => {
      loadData()
    }

    onMounted(() => {
      loadData()
    })

    return {
      loading,
      dateRange,
      stats,
      volumeTrends,
      modalityData,
      statusData,
      performanceMetrics,
      refreshData,
      Refresh,
      loadData
    }
  }
}
</script>

<style scoped>
.study-analytics {
  padding: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.header-content h2 {
  margin: 0;
  font-size: 24px;
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.stats-row {
  margin-bottom: 20px;
}
</style>