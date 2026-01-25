<template>
  <div class="nurse-dashboard">
    <h2>Nurse Dashboard - {{ userName }}</h2>

    <!-- Quick Stats -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card uploads" shadow="hover">
          <div class="stat-content">
            <el-icon :size="48"><Upload /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayUploads }}</div>
              <div class="stat-label">Today's Uploads</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card appointments" shadow="hover">
          <div class="stat-content">
            <el-icon :size="48"><Calendar /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayAppointments }}</div>
              <div class="stat-label">Today's Appointments</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card patients" shadow="hover">
          <div class="stat-content">
            <el-icon :size="48"><User /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalPatients }}</div>
              <div class="stat-label">Total Patients</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card studies" shadow="hover">
          <div class="stat-content">
            <el-icon :size="48"><FolderOpened /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalStudies }}</div>
              <div class="stat-label">Total Studies</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Quick Actions -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>Quick Actions</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" size="large" @click="$router.push('/upload')">
              <el-icon><Upload /></el-icon> Upload Images
            </el-button>
            <el-button type="success" size="large" @click="$router.push('/nurse/schedule')">
              <el-icon><Calendar /></el-icon> View Schedule
            </el-button>
            <el-button type="info" size="large" @click="$router.push('/studies')">
              <el-icon><FolderOpened /></el-icon> View Studies
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- Today's Schedule -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Today's Schedule ({{ today }})</span>
              <el-button text type="primary" @click="$router.push('/nurse/schedule')">View Full Schedule</el-button>
            </div>
          </template>
          <div v-loading="loading">
            <el-table v-if="todaySchedule.length > 0" :data="todaySchedule" style="width: 100%">
              <el-table-column prop="time" label="Time" width="100">
                <template #default="{ row }">
                  {{ formatTime(row.time) }}
                </template>
              </el-table-column>
              <el-table-column prop="patientName" label="Patient" width="150" />
              <el-table-column prop="type" label="Exam Type" width="130" />
              <el-table-column prop="doctorName" label="Doctor" width="150" />
              <el-table-column prop="status" label="Status" width="120">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="No appointments scheduled for today" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Recent Studies -->
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Recent Studies</span>
              <el-button text type="primary" @click="$router.push('/studies')">View All Studies</el-button>
            </div>
          </template>
          <div v-loading="loadingStudies">
            <el-table v-if="recentStudies.length > 0" :data="recentStudies" style="width: 100%">
              <el-table-column prop="studyDate" label="Date" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.studyDate) }}
                </template>
              </el-table-column>
              <el-table-column prop="patientName" label="Patient" width="150" />
              <el-table-column prop="modality" label="Modality" width="100">
                <template #default="{ row }">
                  <el-tag>{{ row.modality }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="Description" />
              <el-table-column prop="status" label="Status" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'uploaded' ? 'success' : 'warning'" size="small">
                    {{ row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="Actions" width="100">
                <template #default="{ row }">
                  <el-button size="small" @click="viewStudy(row)">View</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="No studies found" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { Upload, Calendar, User, FolderOpened } from '@element-plus/icons-vue'

export default {
  name: 'NurseDashboard',
  components: { Upload, Calendar, User, FolderOpened },
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const loadingStudies = ref(false)

    const userName = computed(() => {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      return user.fullName || user.username || 'Nurse'
    })

    const today = computed(() => {
      return new Date().toLocaleDateString('en', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
    })

    const stats = reactive({
      todayUploads: 0,
      todayAppointments: 0,
      totalPatients: 0,
      totalStudies: 0
    })

    const todaySchedule = ref([])
    const recentStudies = ref([])

    const loadTodaySchedule = async () => {
      loading.value = true
      try {
        const response = await http.get('/appointments/today')
        todaySchedule.value = response.data || []
        stats.todayAppointments = todaySchedule.value.length
      } catch (error) {
        console.error('Failed to load schedule:', error)
        todaySchedule.value = []
      } finally {
        loading.value = false
      }
    }

    const loadRecentStudies = async () => {
      loadingStudies.value = true
      try {
        const response = await http.get('/studies', { params: { page: 0, size: 10 } })
        recentStudies.value = response.data.content || []
        stats.totalStudies = response.data.totalElements || 0
      } catch (error) {
        console.error('Failed to load studies:', error)
        recentStudies.value = []
      } finally {
        loadingStudies.value = false
      }
    }

    const loadStats = async () => {
      try {
        // Appointment stats are loaded separately by loadTodaySchedule()
        // Nurses don't have access to /api/patients endpoint, so skip patient stats
        stats.totalPatients = 0  // Placeholder - nurses don't manage patients directly
      } catch (error) {
        console.error('Failed to load stats:', error)
      }
    }

    const viewStudy = (study) => {
      router.push(`/studies/${study.id}`)
    }

    const formatTime = (timeStr) => {
      if (!timeStr) return '-'
      return timeStr.substring(0, 5)
    }

    const formatDateTime = (dateStr) => {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleString()
    }

    const getStatusType = (status) => {
      const types = {
        'scheduled': 'warning',
        'completed': 'success',
        'cancelled': 'danger',
        'in-progress': 'primary'
      }
      return types[status] || 'info'
    }

    onMounted(() => {
      loadTodaySchedule()
      loadRecentStudies()
      loadStats()
    })

    return {
      loading, loadingStudies, userName, today, stats,
      todaySchedule, recentStudies, viewStudy, formatTime, formatDateTime, getStatusType
    }
  }
}
</script>

<style scoped>
.nurse-dashboard { padding: 20px; }
.nurse-dashboard h2 { margin: 0 0 20px; color: #333; }

.stat-card { border-radius: 12px; }
.stat-card.uploads { border-top: 4px solid #409eff; }
.stat-card.appointments { border-top: 4px solid #67c23a; }
.stat-card.patients { border-top: 4px solid #e6a23c; }
.stat-card.studies { border-top: 4px solid #909399; }

.stat-content { display: flex; align-items: center; gap: 20px; }
.stat-value { font-size: 32px; font-weight: bold; color: #333; }
.stat-label { font-size: 14px; color: #909399; }

.card-header { display: flex; justify-content: space-between; align-items: center; }

.quick-actions { display: flex; flex-direction: column; gap: 15px; }
.quick-actions .el-button { height: 60px; }
</style>