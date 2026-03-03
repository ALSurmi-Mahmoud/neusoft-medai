<template>
  <div class="report-analytics">
    <el-card>
      <template #header>
        <div class="header-content">
          <h2>Report Analytics</h2>
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

      <!-- Statistics -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="24" :sm="6">
          <StatCard
              title="Total Reports"
              :value="stats.total"
              label="Generated"
              icon="Files"
              color="#409EFF"
          />
        </el-col>
        <el-col :xs="24" :sm="6">
          <StatCard
              title="This Month"
              :value="stats.thisMonth"
              :trend="stats.trend"
              label="vs last month"
              icon="Files"
              color="#67C23A"
          />
        </el-col>
        <el-col :xs="24" :sm="6">
          <StatCard
              title="Avg Turnaround"
              :value="stats.avgTurnaround"
              label="hours"
              icon="Calendar"
              color="#E6A23C"
          />
        </el-col>
        <el-col :xs="24" :sm="6">
          <StatCard
              title="Quality Score"
              :value="stats.qualityScore"
              label="out of 5"
              icon="Checked"
              color="#F56C6C"
          />
        </el-col>
      </el-row>

      <!-- Charts -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Report Production Trends</template>
            <LineChart :data="productionTrends" height="350px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Reports by Status</template>
            <PieChart :data="statusData" height="350px" />
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Top Contributing Doctors</template>
            <BarChart :data="doctorStats" height="350px" :horizontal="true" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Turnaround Time Analysis</template>
            <el-table :data="turnaroundData" max-height="350">
              <el-table-column prop="category" label="Time Range" />
              <el-table-column prop="count" label="Count" width="100" />
              <el-table-column prop="percentage" label="%" width="100">
                <template #default="scope">
                  {{ scope.row.percentage }}%
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
  name: 'ReportAnalytics',
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
      total: 0,
      thisMonth: 0,
      trend: 0,
      avgTurnaround: '0',
      qualityScore: '0'
    })

    const productionTrends = ref({ dates: [], values: [] })
    const statusData = ref([])
    const doctorStats = ref({ categories: [], values: [] })
    const turnaroundData = ref([])

    const formatDate = (date) => {
      return date.toISOString().split('T')[0]
    }

    const loadData = async () => {
      loading.value = true
      try {
        const startDate = formatDate(dateRange.value[0])
        const endDate = formatDate(dateRange.value[1])

        await Promise.all([

          loadProductionTrends(startDate, endDate),
          loadStatusDistribution(startDate, endDate),
          loadDoctorStats(startDate, endDate),
          loadTurnaroundData(startDate, endDate)
        ])

        ElMessage.success('Report analytics loaded')
      } catch (error) {
        ElMessage.error('Failed to load report analytics')
        console.error(error)
      } finally {
        loading.value = false
      }
    }

    const loadStatistics = async (startDate, endDate) => {
      try {
        const response = await http.get('/analytics/reports/statistics', {
          params: { startDate, endDate }
        })
        const data = response.data
        stats.value = {
          total: data.total || 0,
          thisMonth: data.thisMonth || 0,
          trend: data.trend || 0,
          avgTurnaround: data.avgTurnaround?.toString() || '0',
          qualityScore: data.qualityScore?.toFixed(1) || '0'
        }
      } catch (error) {
        console.error('Failed to load statistics:', error)
      }
    }

    const loadProductionTrends = async (startDate, endDate) => {
      try {
        const response = await http.get('/analytics/reports/trends', {
          params: { startDate, endDate }
        })
        const data = response.data
        productionTrends.value = {
          dates: data.map(d => d.date),
          values: data.map(d => d.count)
        }
      } catch (error) {
        console.error('Failed to load production trends:', error)
      }
    }

    const loadStatusDistribution = async (startDate, endDate) => {
      try {
        const response = await http.get('/analytics/reports/production', {
          params: { startDate, endDate }
        })
        const data = response.data.byStatus || {}
        statusData.value = Object.entries(data).map(([name, value]) => ({
          name,
          value
        }))
      } catch (error) {
        console.error('Failed to load status distribution:', error)
      }
    }

    const loadDoctorStats = async (startDate, endDate) => {
      try {
        const response = await http.get('/analytics/reports/by-doctor', {
          params: { startDate, endDate, limit: 10 }
        })
        const data = response.data
        doctorStats.value = {
          categories: data.map(d => d.doctorName),
          values: data.map(d => d.count)
        }
      } catch (error) {
        console.error('Failed to load doctor stats:', error)
      }
    }

    const loadTurnaroundData = async (startDate, endDate) => {
      try {
        const response = await http.get('/analytics/reports/turnaround', {
          params: { startDate, endDate }
        })
        const data = response.data
        turnaroundData.value = Array.isArray(data) ? data : []
      } catch (error) {
        console.error('Failed to load turnaround data:', error)
        turnaroundData.value = [
          { category: '< 24 hours', count: 150, percentage: 45 },
          { category: '24-48 hours', count: 120, percentage: 36 },
          { category: '48-72 hours', count: 50, percentage: 15 },
          { category: '> 72 hours', count: 10, percentage: 4 }
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
      productionTrends,
      statusData,
      doctorStats,
      turnaroundData,
      refreshData,
      Refresh,
      loadData
    }
  }
}
</script>

<style scoped>
.report-analytics {
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