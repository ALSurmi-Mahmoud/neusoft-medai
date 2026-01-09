<template>
  <div class="patient-dashboard">
    <!-- Welcome Banner -->
    <el-card class="welcome-banner">
      <div class="welcome-content">
        <div class="welcome-text">
          <h2>Welcome back, {{ userName }}!</h2>
          <p>Here's your health overview and upcoming appointments.</p>
        </div>
        <div class="welcome-actions">
          <el-button type="primary" @click="$router.push('/my-appointments')">
            <el-icon><Calendar /></el-icon> Book Appointment
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Health Summary -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>My Health Summary</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="Patient ID">{{ healthInfo.patientId }}</el-descriptions-item>
            <el-descriptions-item label="Blood Type">{{ healthInfo.bloodType }}</el-descriptions-item>
            <el-descriptions-item label="Primary Doctor">{{ healthInfo.primaryDoctor }}</el-descriptions-item>
            <el-descriptions-item label="Last Visit">{{ healthInfo.lastVisit }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- Upcoming Appointments -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Upcoming Appointments</span>
              <el-button text type="primary" @click="$router.push('/my-appointments')">View All</el-button>
            </div>
          </template>
          <div v-if="appointments.length === 0">
            <el-empty description="No upcoming appointments" :image-size="60" />
          </div>
          <div v-else class="appointment-list">
            <div v-for="apt in appointments" :key="apt.id" class="appointment-item">
              <div class="apt-date">
                <div class="date-day">{{ apt.day }}</div>
                <div class="date-month">{{ apt.month }}</div>
              </div>
              <div class="apt-info">
                <div class="apt-type">{{ apt.type }}</div>
                <div class="apt-doctor">{{ apt.doctor }}</div>
                <div class="apt-time">{{ apt.time }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Recent Reports -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Recent Reports</span>
              <el-button text type="primary" @click="$router.push('/my-reports')">View All</el-button>
            </div>
          </template>
          <div v-if="reports.length === 0">
            <el-empty description="No reports yet" :image-size="60" />
          </div>
          <div v-else class="report-list">
            <div v-for="report in reports" :key="report.id" class="report-item" @click="viewReport(report)">
              <el-icon :size="24"><Document /></el-icon>
              <div class="report-info">
                <div class="report-title">{{ report.title }}</div>
                <div class="report-date">{{ report.date }}</div>
              </div>
              <el-icon><ArrowRight /></el-icon>
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
            <el-button size="large" @click="$router.push('/my-appointments')">
              <el-icon><Calendar /></el-icon> My Appointments
            </el-button>
            <el-button size="large" @click="$router.push('/my-reports')">
              <el-icon><Document /></el-icon> My Reports
            </el-button>
            <el-button size="large" @click="contactDoctor">
              <el-icon><ChatDotRound /></el-icon> Contact Doctor
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Document, ArrowRight, ChatDotRound } from '@element-plus/icons-vue'

export default {
  name: 'PatientDashboard',
  components: { Calendar, Document, ArrowRight, ChatDotRound },
  setup() {
    const router = useRouter()

    const userName = computed(() => {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      return user.fullName || user.username || 'Patient'
    })

    const healthInfo = reactive({
      patientId: 'P-12345',
      bloodType: 'A+',
      primaryDoctor: 'Dr. Smith',
      lastVisit: 'Jan 5, 2026'
    })

    const appointments = ref([
      { id: 1, day: '15', month: 'Jan', type: 'CT Scan Follow-up', doctor: 'Dr. Smith', time: '10:00 AM' },
      { id: 2, day: '22', month: 'Jan', type: 'General Checkup', doctor: 'Dr. Johnson', time: '2:30 PM' }
    ])

    const reports = ref([
      { id: 1, title: 'CT Chest Report', date: 'Jan 5, 2026' },
      { id: 2, title: 'Blood Test Results', date: 'Dec 28, 2025' }
    ])

    const viewReport = () => {
      router.push('/my-reports')
    }

    const contactDoctor = () => {
      ElMessage.info('Messaging feature coming soon')
    }

    return {
      userName,
      healthInfo,
      appointments,
      reports,
      viewReport,
      contactDoctor
    }
  }
}
</script>

<style scoped>
.patient-dashboard {
  padding: 20px;
}

.welcome-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.welcome-banner :deep(.el-card__body) {
  padding: 30px;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
}

.welcome-text h2 {
  margin: 0 0 10px;
  font-size: 24px;
}

.welcome-text p {
  margin: 0;
  opacity: 0.9;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.appointment-list, .report-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.appointment-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.apt-date {
  text-align: center;
  min-width: 45px;
}

.date-day {
  font-size: 22px;
  font-weight: bold;
  color: #409eff;
}

.date-month {
  font-size: 11px;
  color: #909399;
  text-transform: uppercase;
}

.apt-info {
  flex: 1;
}

.apt-type {
  font-weight: 500;
}

.apt-doctor, .apt-time {
  font-size: 12px;
  color: #909399;
}

.report-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}

.report-item:hover {
  background-color: #f5f7fa;
}

.report-item .el-icon:first-child {
  color: #409eff;
  margin-right: 12px;
}

.report-info {
  flex: 1;
}

.report-title {
  font-weight: 500;
}

.report-date {
  font-size: 12px;
  color: #909399;
}

.quick-actions {
  display: flex;
  gap: 20px;
}

.quick-actions .el-button {
  flex: 1;
  height: 60px;
}
</style>