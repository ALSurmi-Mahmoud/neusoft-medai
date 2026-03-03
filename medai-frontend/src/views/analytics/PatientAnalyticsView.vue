<template>
  <div class="patient-analytics">
    <el-card>
      <template #header>
        <div class="header-content">
          <h2>Patient Analytics</h2>
          <el-button type="primary" @click="refreshData" :loading="loading" :icon="Refresh">
            Refresh
          </el-button>
        </div>
      </template>

      <!-- Statistics Cards -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="24" :sm="8">
          <StatCard
              title="Total Patients"
              :value="stats.totalPatients"
              label="Registered"
              icon="User"
              color="#409EFF"
          />
        </el-col>
        <el-col :xs="24" :sm="8">
          <StatCard
              title="New This Month"
              :value="stats.newThisMonth"
              :trend="stats.monthlyTrend"
              label="vs last month"
              icon="User"
              color="#67C23A"
          />
        </el-col>
        <el-col :xs="24" :sm="8">
          <StatCard
              title="Active Patients"
              :value="stats.activePatients"
              label="Last 30 days"
              icon="Checked"
              color="#E6A23C"
          />
        </el-col>
      </el-row>

      <!-- Charts -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Age Distribution</template>
            <BarChart :data="ageDistribution" height="350px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Gender Distribution</template>
            <PieChart :data="genderDistribution" height="350px" />
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Patient Registration Trends</template>
            <LineChart :data="registrationTrends" height="350px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>Common Conditions</template>
            <el-table :data="commonConditions" max-height="350">
              <el-table-column prop="condition" label="Condition" />
              <el-table-column prop="count" label="Cases" width="100" sortable />
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
import BarChart from '@/components/charts/BarChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import LineChart from '@/components/charts/LineChart.vue'

export default {
  name: 'PatientAnalytics',
  components: {
    StatCard,
    BarChart,
    PieChart,
    LineChart
  },
  setup() {
    const loading = ref(false)
    const stats = ref({
      totalPatients: 0,
      newThisMonth: 0,
      monthlyTrend: 0,
      activePatients: 0
    })
    const ageDistribution = ref({ categories: [], values: [] })
    const genderDistribution = ref([])
    const registrationTrends = ref({ dates: [], values: [] })
    const commonConditions = ref([])

    const loadData = async () => {
      loading.value = true
      try {
        await Promise.all([
          loadStatistics(),
          loadDemographics(),
          loadTrends(),
          loadConditions()
        ])
        ElMessage.success('Patient analytics loaded')
      } catch (error) {
        ElMessage.error('Failed to load patient analytics')
        console.error(error)
      } finally {
        loading.value = false
      }
    }

    const loadStatistics = async () => {
      try {
        const response = await http.get('/analytics/patients/statistics')
        const data = response.data
        stats.value = {
          totalPatients: data.total || 0,
          newThisMonth: data.newThisMonth || 0,
          monthlyTrend: data.trend || 0,
          activePatients: data.active || 0
        }
      } catch (error) {
        console.error('Failed to load statistics:', error)
      }
    }

    const loadDemographics = async () => {
      try {
        const response = await http.get('/analytics/patients/demographics')
        const data = response.data

        // Age distribution
        const ageData = data.ageDistribution || {}
        ageDistribution.value = {
          categories: Object.keys(ageData),
          values: Object.values(ageData)
        }

        // Gender distribution
        const genderData = data.genderDistribution || {}
        genderDistribution.value = Object.entries(genderData).map(([name, value]) => ({
          name,
          value
        }))
      } catch (error) {
        console.error('Failed to load demographics:', error)
      }
    }

    const loadTrends = async () => {
      try {
        const endDate = new Date()
        const startDate = new Date()
        startDate.setMonth(startDate.getMonth() - 6)

        const response = await http.get('/analytics/patients/trends', {
          params: {
            startDate: startDate.toISOString().split('T')[0],
            endDate: endDate.toISOString().split('T')[0]
          }
        })
        const data = response.data
        registrationTrends.value = {
          dates: data.map(d => d.date),
          values: data.map(d => d.count)
        }
      } catch (error) {
        console.error('Failed to load trends:', error)
      }
    }

    const loadConditions = async () => {
      try {
        const response = await http.get('/analytics/patients/conditions')
        // Handle both array and object responses
        const data = response.data
        if (Array.isArray(data)) {
          commonConditions.value = data.slice(0, 10)
        } else if (typeof data === 'object') {
          // Convert object to array format
          commonConditions.value = Object.entries(data)
              .map(([condition, count]) => ({ condition, count, percentage: 0 }))
              .slice(0, 10)
        } else {
          commonConditions.value = []
        }
      } catch (error) {
        console.error('Failed to load conditions:', error)
        commonConditions.value = []
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
      stats,
      ageDistribution,
      genderDistribution,
      registrationTrends,
      commonConditions,
      refreshData,
      Refresh
    }
  }
}
</script>

<style scoped>
.patient-analytics {
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