<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <!-- Statistics Cards -->
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon patients">
              <el-icon :size="40"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.patients }}</div>
              <div class="stat-label">Patients</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon studies">
              <el-icon :size="40"><FolderOpened /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.studies }}</div>
              <div class="stat-label">Studies</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon reports">
              <el-icon :size="40"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.reports }}</div>
              <div class="stat-label">Reports</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon pacs">
              <el-icon :size="40"><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ pacsStatus }}</div>
              <div class="stat-label">PACS Status</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Recent Studies -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Recent Studies</span>
              <el-button type="primary" text @click="$router.push('/studies')">
                View All
              </el-button>
            </div>
          </template>

          <el-table :data="recentStudies" style="width: 100%">
            <el-table-column prop="patientId" label="Patient ID" width="150" />
            <el-table-column prop="modality" label="Modality" width="100" />
            <el-table-column prop="description" label="Description" />
            <el-table-column prop="studyDate" label="Date" width="180">
              <template #default="{ row }">
                {{ formatDate(row.studyDate) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="Status" width="120">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- Quick Actions -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>Quick Actions</span>
          </template>

          <div class="quick-actions">
            <el-button type="primary" size="large" @click="$router.push('/upload')">
              <el-icon><Upload /></el-icon>
              Upload Images
            </el-button>

            <el-button type="success" size="large" @click="$router.push('/studies')">
              <el-icon><FolderOpened /></el-icon>
              Browse Studies
            </el-button>

            <el-button type="warning" size="large" @click="$router.push('/reports')">
              <el-icon><Document /></el-icon>
              View Reports
            </el-button>

            <el-button type="info" size="large" @click="$router.push('/pacs')">
              <el-icon><Connection /></el-icon>
              PACS Operations
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import http from '../utils/http'
import { User, FolderOpened, Document, Connection, Upload } from '@element-plus/icons-vue'

export default {
  name: 'DashboardView',
  components: { User, FolderOpened, Document, Connection, Upload },
  setup() {
    const stats = ref({
      patients: 0,
      studies: 0,
      reports: 0
    })

    const pacsStatus = ref('Offline')
    const recentStudies = ref([])

    const loadDashboardData = async () => {
      try {
        // Load studies
        const studiesRes = await http.get('/studies', { params: { size: 5 } })
        recentStudies.value = studiesRes.data.content || []
        stats.value.studies = studiesRes.data.totalElements || 0

        // Count unique patients
        const uniquePatients = new Set(recentStudies.value.map(s => s.patientId))
        stats.value.patients = uniquePatients.size

        // Load reports count
        const reportsRes = await http.get('/reports', { params: { size: 1 } })
        stats.value.reports = reportsRes.data.totalElements || 0

        // Check PACS status
        const pacsRes = await http.get('/pacs/status')
        pacsStatus.value = pacsRes.data.available ? 'Online' : 'Offline'

      } catch (error) {
        console.error('Failed to load dashboard data:', error)
      }
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleString()
    }

    const getStatusType = (status) => {
      const types = {
        'uploaded': 'info',
        'processed': 'success',
        'archived': 'warning'
      }
      return types[status] || 'info'
    }

    onMounted(() => {
      loadDashboardData()
    })

    return {
      stats,
      pacsStatus,
      recentStudies,
      formatDate,
      getStatusType
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  width: 80px;
  height: 80px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  color: white;
}

.stat-icon.patients { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-icon.studies { background: linear-gradient(135deg, #f093fb, #f5576c); }
.stat-icon.reports { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.stat-icon.pacs { background: linear-gradient(135deg, #43e97b, #38f9d7); }

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 5px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.quick-actions .el-button {
  width: 100%;
  justify-content: flex-start;
  padding-left: 20px;
}
</style>