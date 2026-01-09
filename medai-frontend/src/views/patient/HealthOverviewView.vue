<template>
  <div class="health-overview">
    <el-row :gutter="20">
      <!-- Welcome Card -->
      <el-col :span="24">
        <el-card class="welcome-card">
          <div class="welcome-content">
            <div class="welcome-text">
              <h2>Welcome back, {{ patientInfo.name }}!</h2>
              <p>Here's your health overview and recent activity.</p>
            </div>
            <div class="welcome-actions">
              <el-button type="primary" @click="$router.push('/my-appointments')">
                <el-icon><Calendar /></el-icon>
                Book Appointment
              </el-button>
              <el-button @click="$router.push('/my-reports')">
                <el-icon><Document /></el-icon>
                View Reports
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Health Summary -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>Health Summary</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="Patient ID">{{ patientInfo.patientId }}</el-descriptions-item>
            <el-descriptions-item label="Date of Birth">{{ patientInfo.birthDate }}</el-descriptions-item>
            <el-descriptions-item label="Gender">{{ patientInfo.gender }}</el-descriptions-item>
            <el-descriptions-item label="Blood Type">{{ patientInfo.bloodType }}</el-descriptions-item>
            <el-descriptions-item label="Primary Doctor">{{ patientInfo.primaryDoctor }}</el-descriptions-item>
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
          <div v-if="upcomingAppointments.length === 0" class="no-data">
            <el-empty description="No upcoming appointments" :image-size="80" />
          </div>
          <div v-else class="appointment-list">
            <div v-for="apt in upcomingAppointments" :key="apt.id" class="appointment-item">
              <div class="apt-date">
                <div class="apt-day">{{ apt.day }}</div>
                <div class="apt-month">{{ apt.month }}</div>
              </div>
              <div class="apt-info">
                <div class="apt-type">{{ apt.type }}</div>
                <div class="apt-doctor">Dr. {{ apt.doctorName }}</div>
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
          <div v-if="recentReports.length === 0" class="no-data">
            <el-empty description="No reports yet" :image-size="80" />
          </div>
          <div v-else class="report-list">
            <div v-for="report in recentReports" :key="report.id" class="report-item" @click="viewReport(report)">
              <el-icon :size="24" class="report-icon"><Document /></el-icon>
              <div class="report-info">
                <div class="report-title">{{ report.title }}</div>
                <div class="report-date">{{ report.date }}</div>
              </div>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
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
            <span>Recent Imaging Studies</span>
          </template>
          <el-table :data="recentStudies" style="width: 100%">
            <el-table-column prop="date" label="Date" width="120" />
            <el-table-column prop="type" label="Type" width="100">
              <template #default="{ row }">
                <el-tag>{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="Description" />
            <el-table-column prop="status" label="Status" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === 'Completed' ? 'success' : 'warning'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="doctor" label="Doctor" width="150" />
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
            <el-button size="large" @click="$router.push('/my-appointments')">
              <el-icon><Calendar /></el-icon>
              Schedule Appointment
            </el-button>
            <el-button size="large" @click="$router.push('/my-reports')">
              <el-icon><Document /></el-icon>
              Download Reports
            </el-button>
            <el-button size="large" @click="contactDoctor">
              <el-icon><ChatDotRound /></el-icon>
              Contact Doctor
            </el-button>
            <el-button size="large" @click="updateProfile">
              <el-icon><User /></el-icon>
              Update Profile
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Document, ArrowRight, ChatDotRound, User } from '@element-plus/icons-vue'

export default {
  name: 'HealthOverviewView',
  components: { Calendar, Document, ArrowRight, ChatDotRound, User },
  setup() {
    const router = useRouter()

    const patientInfo = ref({
      name: 'John Doe',
      patientId: 'P-12345',
      birthDate: '1985-03-15',
      gender: 'Male',
      bloodType: 'A+',
      primaryDoctor: 'Dr. Smith'
    })

    const upcomingAppointments = ref([
      { id: 1, day: '15', month: 'Jan', type: 'CT Scan Follow-up', doctorName: 'Smith', time: '10:00 AM' },
      { id: 2, day: '22', month: 'Jan', type: 'General Checkup', doctorName: 'Johnson', time: '2:30 PM' }
    ])

    const recentReports = ref([
      { id: 1, title: 'CT Chest Report', date: 'Jan 05, 2026' },
      { id: 2, title: 'Blood Test Results', date: 'Dec 28, 2025' },
      { id: 3, title: 'MRI Brain Report', date: 'Dec 15, 2025' }
    ])

    const recentStudies = ref([
      { date: 'Jan 05', type: 'CT', description: 'Chest CT Scan', status: 'Completed', doctor: 'Dr. Smith' },
      { date: 'Dec 15', type: 'MRI', description: 'Brain MRI', status: 'Completed', doctor: 'Dr. Johnson' },
      { date: 'Dec 01', type: 'X-Ray', description: 'Chest X-Ray', status: 'Completed', doctor: 'Dr. Smith' }
    ])

    const viewReport = () => {
      router.push('/my-reports')
    }

    const contactDoctor = () => {
      ElMessage.info('Messaging feature coming soon')
    }

    const updateProfile = () => {
      ElMessage.info('Profile update feature coming soon')
    }

    return {
      patientInfo,
      upcomingAppointments,
      recentReports,
      recentStudies,
      viewReport,
      contactDoctor,
      updateProfile
    }
  }
}
</script>

<style scoped>
.health-overview {
  padding: 20px;
}

.welcome-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.welcome-card :deep(.el-card__body) {
  padding: 30px;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-text h2 {
  margin: 0 0 10px;
  font-size: 24px;
}

.welcome-text p {
  margin: 0;
  opacity: 0.9;
}

.welcome-actions {
  display: flex;
  gap: 10px;
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
  gap: 15px;
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
  min-width: 50px;
}

.apt-day {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.apt-month {
  font-size: 12px;
  color: #909399;
  text-transform: uppercase;
}

.apt-info {
  flex: 1;
}

.apt-type {
  font-weight: 600;
  margin-bottom: 3px;
}

.apt-doctor {
  font-size: 13px;
  color: #606266;
}

.apt-time {
  font-size: 12px;
  color: #909399;
}

.report-list {
  display: flex;
  flex-direction: column;
}

.report-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  border-bottom: 1px solid #ebeef5;
  transition: all 0.2s;
}

.report-item:last-child {
  border-bottom: none;
}

.report-item:hover {
  background-color: #f5f7fa;
}

.report-icon {
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

.arrow-icon {
  color: #c0c4cc;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-actions .el-button {
  justify-content: flex-start;
  padding-left: 20px;
}
</style>