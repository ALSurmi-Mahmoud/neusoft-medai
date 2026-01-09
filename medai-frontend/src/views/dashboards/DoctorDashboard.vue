<template>
  <div class="doctor-dashboard">
    <h2>Welcome, Dr. {{ userName }}</h2>

    <!-- Quick Stats -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card pending" shadow="hover" @click="$router.push('/worklist')">
          <div class="stat-content">
            <el-icon :size="48"><Tickets /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingCases }}</div>
              <div class="stat-label">Pending Cases</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card today" shadow="hover" @click="$router.push('/appointments')">
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
        <el-card class="stat-card reports" shadow="hover" @click="$router.push('/reports')">
          <div class="stat-content">
            <el-icon :size="48"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.draftReports }}</div>
              <div class="stat-label">Draft Reports</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card completed" shadow="hover">
          <div class="stat-content">
            <el-icon :size="48"><CircleCheck /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.completedToday }}</div>
              <div class="stat-label">Completed Today</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Today's Worklist -->
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>My Worklist</span>
              <el-button text type="primary" @click="$router.push('/worklist')">View All</el-button>
            </div>
          </template>
          <el-table :data="worklist" style="width: 100%" max-height="350">
            <el-table-column prop="priority" label="Priority" width="90">
              <template #default="{ row }">
                <el-tag :type="getPriorityType(row.priority)" size="small" effect="dark">
                  {{ row.priority }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="patientName" label="Patient" width="130" />
            <el-table-column prop="modality" label="Modality" width="80" />
            <el-table-column prop="description" label="Description" show-overflow-tooltip />
            <el-table-column label="Actions" width="120">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openStudy(row)">
                  <el-icon><View /></el-icon> View
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- Today's Appointments -->
      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Today's Appointments</span>
              <el-button text type="primary" @click="$router.push('/appointments')">View All</el-button>
            </div>
          </template>
          <div v-if="appointments.length === 0" class="no-data">
            <el-empty description="No appointments today" :image-size="60" />
          </div>
          <div v-else class="appointment-list">
            <div v-for="apt in appointments" :key="apt.id" class="appointment-item" :class="apt.status">
              <div class="apt-time">{{ apt.time }}</div>
              <div class="apt-info">
                <div class="apt-patient">{{ apt.patientName }}</div>
                <div class="apt-type">{{ apt.type }}</div>
              </div>
              <el-tag :type="getStatusType(apt.status)" size="small">{{ apt.status }}</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Quick Actions -->
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>Quick Actions</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" size="large" @click="$router.push('/worklist')">
              <el-icon><Tickets /></el-icon> Open Worklist
            </el-button>
            <el-button type="success" size="large" @click="$router.push('/studies')">
              <el-icon><FolderOpened /></el-icon> Browse Studies
            </el-button>
            <el-button type="warning" size="large" @click="$router.push('/reports')">
              <el-icon><Document /></el-icon> My Reports
            </el-button>
            <el-button type="info" size="large" @click="$router.push('/pacs')">
              <el-icon><Connection /></el-icon> PACS Access
            </el-button>
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
import { Tickets, Calendar, Document, CircleCheck, View, FolderOpened, Connection } from '@element-plus/icons-vue'

export default {
  name: 'DoctorDashboard',
  components: { Tickets, Calendar, Document, CircleCheck, View, FolderOpened, Connection },
  setup() {
    const router = useRouter()

    const userName = computed(() => {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      return user.fullName?.replace('Dr. ', '') || user.username || 'Doctor'
    })

    const stats = reactive({
      pendingCases: 5,
      todayAppointments: 3,
      draftReports: 2,
      completedToday: 8
    })

    const worklist = ref([
      { id: 1, patientName: 'John Doe', modality: 'CT', description: 'Chest CT - possible nodules', priority: 'high', studyId: 1 },
      { id: 2, patientName: 'Jane Smith', modality: 'MR', description: 'Brain MRI follow-up', priority: 'urgent', studyId: 2 },
      { id: 3, patientName: 'Bob Wilson', modality: 'XR', description: 'Routine chest X-ray', priority: 'normal', studyId: 3 }
    ])

    const appointments = ref([
      { id: 1, time: '09:00', patientName: 'John Doe', type: 'CT Review', status: 'scheduled' },
      { id: 2, time: '10:30', patientName: 'Jane Smith', type: 'Consultation', status: 'completed' },
      { id: 3, time: '14:00', patientName: 'Alice Brown', type: 'Follow-up', status: 'scheduled' }
    ])

    const loadData = async () => {
      // Load appointments
      try {
        const response = await http.get('/appointments/today')
        if (response.data && response.data.length > 0) {
          appointments.value = response.data.map(apt => ({
            id: apt.id,
            time: apt.appointmentTime?.substring(0, 5) || '09:00',
            patientName: apt.patientName || 'Unknown',
            type: apt.appointmentType || 'Consultation',
            status: apt.status || 'scheduled'
          }))
          stats.todayAppointments = appointments.value.length
        }
      } catch (e) {
        console.log('Using mock appointments data')
      }
    }

    const openStudy = (row) => {
      router.push(`/viewer/${row.studyId}`)
    }

    const getPriorityType = (priority) => {
      const types = { urgent: 'danger', high: 'warning', normal: 'info', low: '' }
      return types[priority] || 'info'
    }

    const getStatusType = (status) => {
      const types = { scheduled: 'warning', completed: 'success', cancelled: 'danger' }
      return types[status] || 'info'
    }

    onMounted(() => {
      loadData()
    })

    return {
      userName,
      stats,
      worklist,
      appointments,
      openStudy,
      getPriorityType,
      getStatusType
    }
  }
}
</script>

<style scoped>
.doctor-dashboard {
  padding: 20px;
}

.doctor-dashboard h2 {
  margin: 0 0 20px;
  color: #333;
}

.stat-card {
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-card.pending { border-top: 4px solid #e6a23c; }
.stat-card.today { border-top: 4px solid #409eff; }
.stat-card.reports { border-top: 4px solid #67c23a; }
.stat-card.completed { border-top: 4px solid #909399; }

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.no-data {
  padding: 20px;
}

.appointment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.appointment-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background-color: #f5f7fa;
  gap: 15px;
}

.appointment-item.completed {
  background-color: #f0f9eb;
}

.apt-time {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
  min-width: 60px;
}

.apt-info {
  flex: 1;
}

.apt-patient {
  font-weight: 500;
}

.apt-type {
  font-size: 12px;
  color: #909399;
}

.quick-actions {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.quick-actions .el-button {
  flex: 1;
  min-width: 200px;
  height: 60px;
}
</style>