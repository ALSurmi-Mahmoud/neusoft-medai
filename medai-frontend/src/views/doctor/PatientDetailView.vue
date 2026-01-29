<template>
  <div class="patient-detail-view" v-loading="loading">
    <!-- Patient Header -->
    <el-card class="patient-header-card">
      <div class="patient-header">
        <div class="patient-main-info">
          <el-avatar :size="80" class="patient-avatar">
            {{ getInitials(patient.name) }}
          </el-avatar>
          <div class="patient-identity">
            <h2>{{ patient.name }}</h2>
            <div class="patient-badges">
              <el-tag>{{ patient.patientId }}</el-tag>
              <el-tag :type="patient.status === 'active' ? 'success' : 'info'" size="small">
                {{ patient.status }}
              </el-tag>
              <el-tag v-if="patient.age" type="info" size="small">
                {{ patient.age }} years old
              </el-tag>
            </div>
          </div>
        </div>

        <div class="patient-quick-stats">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-statistic title="Appointments" :value="patient.appointmentCount || 0">
                <template #prefix><el-icon><Calendar /></el-icon></template>
              </el-statistic>
            </el-col>
            <el-col :span="6">
              <el-statistic title="Studies" :value="patient.studyCount || 0">
                <template #prefix><el-icon><FolderOpened /></el-icon></template>
              </el-statistic>
            </el-col>
            <el-col :span="6">
              <el-statistic title="Reports" :value="patient.reportCount || 0">
                <template #prefix><el-icon><Document /></el-icon></template>
              </el-statistic>
            </el-col>
            <el-col :span="6">
              <div class="last-visit-stat">
                <div class="stat-title">Last Visit</div>
                <div class="stat-value">{{ formatDate(patient.lastVisit) }}</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Main Content -->
      <el-col :span="18">
        <el-card>
          <el-tabs v-model="activeTab" type="border-card">
            <!-- Overview Tab -->
            <el-tab-pane label="Overview" name="overview">
              <div class="overview-content">
                <!-- Demographics -->
                <h3>Demographics</h3>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="Date of Birth">
                    {{ formatDate(patient.birthDate) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="Gender">
                    {{ getGenderLabel(patient.sex) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="Blood Type">
                    {{ patient.bloodType || 'N/A' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="Email">
                    {{ patient.email }}
                  </el-descriptions-item>
                  <el-descriptions-item label="Phone">
                    {{ patient.phone || 'N/A' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="Address">
                    {{ getFullAddress() }}
                  </el-descriptions-item>
                </el-descriptions>

                <!-- Emergency Contact -->
                <h3 style="margin-top: 20px;">Emergency Contact</h3>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="Name">
                    {{ patient.emergencyContactName || 'N/A' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="Phone">
                    {{ patient.emergencyContactPhone || 'N/A' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="Relationship" :span="2">
                    {{ patient.emergencyContactRelationship || 'N/A' }}
                  </el-descriptions-item>
                </el-descriptions>

                <!-- Medical Information -->
                <h3 style="margin-top: 20px;">Medical Information</h3>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="Allergies">
                    <el-text v-if="patient.allergies">{{ patient.allergies }}</el-text>
                    <el-text v-else type="info">No known allergies</el-text>
                  </el-descriptions-item>
                  <el-descriptions-item label="Medical Conditions">
                    <el-text v-if="patient.medicalConditions">{{ patient.medicalConditions }}</el-text>
                    <el-text v-else type="info">No known conditions</el-text>
                  </el-descriptions-item>
                  <el-descriptions-item label="Current Medications">
                    <el-text v-if="patient.currentMedications">{{ patient.currentMedications }}</el-text>
                    <el-text v-else type="info">No current medications</el-text>
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </el-tab-pane>

            <!-- Appointments Tab -->
            <el-tab-pane name="appointments">
              <template #label>
                <span>Appointments <el-badge :value="appointments.length" class="tab-badge" /></span>
              </template>
              <el-table :data="appointments" style="width: 100%">
                <el-table-column prop="appointmentDate" label="Date" width="130" sortable>
                  <template #default="{ row }">
                    {{ formatDate(row.appointmentDate) }}
                  </template>
                </el-table-column>
                <el-table-column prop="appointmentTime" label="Time" width="100">
                  <template #default="{ row }">
                    {{ formatTime(row.appointmentTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="appointmentType" label="Type" width="150" />
                <el-table-column prop="status" label="Status" width="120">
                  <template #default="{ row }">
                    <el-tag :type="getStatusType(row.status)" size="small">
                      {{ row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="location" label="Location" width="150" />
                <el-table-column prop="notes" label="Notes" show-overflow-tooltip />
              </el-table>
              <div v-if="appointments.length === 0" class="empty-state">
                <el-empty description="No appointments found" />
              </div>
            </el-tab-pane>

            <!-- Reports Tab -->
            <el-tab-pane name="reports">
              <template #label>
                <span>Reports <el-badge :value="reports.length" class="tab-badge" /></span>
              </template>
              <el-table :data="reports" style="width: 100%">
                <el-table-column prop="createdAt" label="Date" width="150" sortable>
                  <template #default="{ row }">
                    {{ formatDateTime(row.createdAt) }}
                  </template>
                </el-table-column>
                <el-table-column prop="title" label="Title" min-width="250" />
                <el-table-column prop="status" label="Status" width="120">
                  <template #default="{ row }">
                    <el-tag :type="row.finalized ? 'success' : 'warning'" size="small">
                      {{ row.finalized ? 'Final' : 'Draft' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="authorName" label="Author" width="150" />
                <el-table-column label="Actions" width="120" fixed="right">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" @click="viewReport(row)">
                      <el-icon><View /></el-icon> View
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="reports.length === 0" class="empty-state">
                <el-empty description="No reports found" />
              </div>
            </el-tab-pane>

            <!-- Studies Tab -->
            <el-tab-pane name="studies">
              <template #label>
                <span>Studies <el-badge :value="studies.length" class="tab-badge" /></span>
              </template>
              <el-table :data="studies" style="width: 100%">
                <el-table-column prop="studyDate" label="Date" width="180" sortable>
                  <template #default="{ row }">
                    {{ formatDateTime(row.studyDate) }}
                  </template>
                </el-table-column>
                <el-table-column prop="modality" label="Modality" width="100">
                  <template #default="{ row }">
                    <el-tag size="small">{{ row.modality }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="Description" min-width="250" />
                <el-table-column prop="accessionNumber" label="Accession #" width="150" />
                <el-table-column prop="status" label="Status" width="120">
                  <template #default="{ row }">
                    <el-tag :type="getStudyStatusType(row.status)" size="small">
                      {{ row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="Actions" width="120" fixed="right">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" @click="viewStudy(row)">
                      <el-icon><View /></el-icon> View
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="studies.length === 0" class="empty-state">
                <el-empty description="No studies found" />
              </div>
            </el-tab-pane>
            <!-- Prescriptions Tab -->
            <el-tab-pane name="prescriptions">
              <template #label>
                <span>Prescriptions <el-badge :value="prescriptions.length" class="tab-badge" /></span>
              </template>
              <PrescriptionsTab
                  v-if="patient.id"
                  :patient-id="patient.id"
                  @prescription-added="loadPatientDetail"
              />
            </el-tab-pane>

            <!-- Timeline Tab -->
            <el-tab-pane label="Timeline" name="timeline">
              <div class="timeline-content">
                <el-timeline>
                  <el-timeline-item
                      v-for="event in timelineEvents"
                      :key="event.id"
                      :timestamp="formatDateTime(event.date)"
                      placement="top"
                      :type="event.type"
                  >
                    <el-card>
                      <div class="timeline-event">
                        <div class="event-icon">
                          <el-icon v-if="event.category === 'appointment'"><Calendar /></el-icon>
                          <el-icon v-else-if="event.category === 'report'"><Document /></el-icon>
                          <el-icon v-else-if="event.category === 'study'"><FolderOpened /></el-icon>
                        </div>
                        <div class="event-content">
                          <h4>{{ event.title }}</h4>
                          <p>{{ event.description }}</p>
                        </div>
                      </div>
                    </el-card>
                  </el-timeline-item>
                </el-timeline>
                <div v-if="timelineEvents.length === 0" class="empty-state">
                  <el-empty description="No timeline events" />
                </div>
              </div>
            </el-tab-pane>
            <!--Clinical Notes tab-->
            <el-tab-pane label="Clinical Notes" name="clinical-notes">
              <ClinicalNotesTab :patient-id="patient.id" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <!-- Quick Actions Sidebar -->
      <el-col :span="6">
        <el-card class="quick-actions-card">
          <template #header>
            <span>Quick Actions</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="scheduleAppointment" style="width: 100%; margin-bottom: 10px;">
              <el-icon><Calendar /></el-icon> Schedule Appointment
            </el-button>
            <el-button type="success" @click="createReport" style="width: 100%; margin-bottom: 10px;">
              <el-icon><Document /></el-icon> Create Report
            </el-button>
            <el-button @click="sendMessage" style="width: 100%; margin-bottom: 10px;">
              <el-icon><ChatDotRound /></el-icon> Send Message
            </el-button>
            <el-button @click="editPatient" style="width: 100%;">
              <el-icon><Edit /></el-icon> Edit Patient Info
            </el-button>
          </div>
        </el-card>

        <!-- Recent Activity -->
        <el-card style="margin-top: 20px;">
          <template #header>
            <span>Recent Activity</span>
          </template>
          <el-timeline>
            <el-timeline-item
                v-for="activity in recentActivity"
                :key="activity.id"
                :timestamp="formatRelativeTime(activity.date)"
                size="small"
            >
              {{ activity.description }}
            </el-timeline-item>
          </el-timeline>
          <div v-if="recentActivity.length === 0">
            <el-text type="info">No recent activity</el-text>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import PrescriptionsTab from './PrescriptionsTab.vue'
import ClinicalNotesTab from '../clinical-notes/ClinicalNotesTab.vue'
import {
  Calendar, FolderOpened, Document, View, ChatDotRound, Edit
}
from '@element-plus/icons-vue'

export default {
  name: 'PatientDetailView',
  components: {
    Calendar, FolderOpened, Document, View, ChatDotRound, Edit, PrescriptionsTab, ClinicalNotesTab
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const loading = ref(false)
    const activeTab = ref('overview')
    const prescriptions = ref([]); // ✅ Declare prescriptions here


    const patient = reactive({
      id: null,
      patientId: '',
      name: '',
      email: '',
      phone: '',
      birthDate: null,
      sex: '',
      age: null,
      bloodType: '',
      address: '',
      city: '',
      state: '',
      zipCode: '',
      emergencyContactName: '',
      emergencyContactPhone: '',
      emergencyContactRelationship: '',
      allergies: '',
      medicalConditions: '',
      currentMedications: '',
      status: '',
      lastVisit: null,
      appointmentCount: 0,
      studyCount: 0,
      reportCount: 0
    })

    const appointments = ref([])
    const reports = ref([])
    const studies = ref([])

    const timelineEvents = computed(() => {
      const events = []

      // Add appointments
      appointments.value.forEach(apt => {
        events.push({
          id: `apt-${apt.id}`,
          category: 'appointment',
          type: apt.status === 'completed' ? 'success' : 'primary',
          date: apt.appointmentDate,
          title: apt.appointmentType,
          description: `${apt.status} - ${apt.location || 'N/A'}`
        })
      })

      // Add reports
      reports.value.forEach(report => {
        events.push({
          id: `report-${report.id}`,
          category: 'report',
          type: 'success',
          date: report.createdAt,
          title: report.title,
          description: `Report by ${report.authorName}`
        })
      })

      // Add studies
      studies.value.forEach(study => {
        events.push({
          id: `study-${study.id}`,
          category: 'study',
          type: 'info',
          date: study.studyDate,
          title: `${study.modality} Study`,
          description: study.description || 'No description'
        })
      })

      // Sort by date descending
      return events.sort((a, b) => new Date(b.date) - new Date(a.date))
    })

    const recentActivity = computed(() => {
      return timelineEvents.value.slice(0, 5)
    })

    const loadPatientDetail = async () => {
      loading.value = true
      try {
        const patientId = route.params.id
        // Use the ENHANCED complete endpoint for Phase 4.2
        const response = await http.get(`/patient/doctor/patients/${patientId}/complete`)
        const prescriptions = ref([])

        // Assign all patient data
        Object.assign(patient, response.data)

        // Load ALL appointments, reports, and studies (not just recent)
        appointments.value = response.data.recentAppointments || []
        reports.value = response.data.recentReports || []
        studies.value = response.data.recentStudies || []


        if (response.data.prescriptions) {
          prescriptions.value = response.data.prescriptions;
        }


      } catch (error) {
        console.error('Failed to load patient:', error)
        ElMessage.error('Failed to load patient details')
        router.push('/my-patients')
      } finally {
        loading.value = false
      }
    }

    const getInitials = (name) => {
      if (!name) return '??'
      return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
    }

    const getGenderLabel = (sex) => {
      const labels = { M: 'Male', F: 'Female', O: 'Other', U: 'Unknown' }
      return labels[sex] || 'N/A'
    }

    const getFullAddress = () => {
      const parts = [
        patient.address,
        patient.city,
        patient.state,
        patient.zipCode
      ].filter(Boolean)
      return parts.length > 0 ? parts.join(', ') : 'N/A'
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    }

    const formatTime = (timeStr) => {
      if (!timeStr) return ''
      return timeStr.substring(0, 5)
    }

    const formatDateTime = (dateTimeStr) => {
      if (!dateTimeStr) return 'N/A'
      return new Date(dateTimeStr).toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    }

    const formatRelativeTime = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      const now = new Date()
      const diffMs = now - date
      const diffMins = Math.floor(diffMs / 60000)
      const diffHours = Math.floor(diffMins / 60)
      const diffDays = Math.floor(diffHours / 24)

      if (diffMins < 60) return `${diffMins}m ago`
      if (diffHours < 24) return `${diffHours}h ago`
      if (diffDays < 7) return `${diffDays}d ago`
      return formatDate(dateStr)
    }

    const getStatusType = (status) => {
      const types = {
        scheduled: 'warning',
        confirmed: 'primary',
        completed: 'success',
        cancelled: 'danger'
      }
      return types[status] || 'info'
    }

    const getStudyStatusType = (status) => {
      const types = {
        uploaded: 'warning',
        processed: 'success',
        archived: 'info'
      }
      return types[status] || 'info'
    }

    const scheduleAppointment = () => {
      router.push(`/appointments?patientId=${patient.id}`)
    }

    const createReport = () => {
      router.push(`/reports/new?patientId=${patient.id}&patientName=${patient.name}`)
    }

    const sendMessage = () => {
      ElMessage.info('Messaging feature coming in Phase 4.6')
    }

    const editPatient = () => {
      ElMessage.info('Edit patient feature coming soon')
    }

    const viewReport = (report) => {
      router.push(`/reports/${report.id}`)
    }

    const viewStudy = (study) => {
      router.push(`/studies/${study.id}`)
    }


    onMounted(() => {
      loadPatientDetail()
    })

    return {
      loading,
      activeTab,
      patient,
      appointments,
      reports,
      studies,
      timelineEvents,
      recentActivity,
      getInitials,
      getGenderLabel,
      getFullAddress,
      formatDate,
      formatTime,
      formatDateTime,
      formatRelativeTime,
      getStatusType,
      getStudyStatusType,
      scheduleAppointment,
      createReport,
      sendMessage,
      editPatient,
      viewReport,
      viewStudy,
      prescriptions
    }
  }
}
</script>

<style scoped>
.patient-detail-view {
  padding: 20px;
}

.patient-header-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.patient-header-card :deep(.el-card__body) {
  padding: 30px;
}

.patient-header {
  color: white;
}

.patient-main-info {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.patient-avatar {
  background: rgba(255, 255, 255, 0.3);
  color: white;
  font-weight: bold;
  font-size: 32px;
}

.patient-identity h2 {
  margin: 0 0 10px;
  color: white;
  font-size: 28px;
}

.patient-badges {
  display: flex;
  gap: 10px;
}

.patient-quick-stats {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 20px;
}

.patient-quick-stats :deep(.el-statistic__head) {
  color: rgba(255, 255, 255, 0.8);
}

.patient-quick-stats :deep(.el-statistic__content) {
  color: white;
}

.last-visit-stat {
  text-align: center;
}

.stat-title {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: white;
}

.overview-content h3 {
  margin: 20px 0 10px;
  font-size: 18px;
  color: #333;
}

.overview-content h3:first-child {
  margin-top: 0;
}

.tab-badge {
  margin-left: 8px;
}

.empty-state {
  padding: 40px;
  text-align: center;
}

.timeline-content {
  padding: 20px 0;
}

.timeline-event {
  display: flex;
  align-items: flex-start;
  gap: 15px;
}

.event-icon {
  font-size: 24px;
  color: #409eff;
  margin-top: 5px;
}

.event-content h4 {
  margin: 0 0 5px;
  font-size: 16px;
  color: #333;
}

.event-content p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.quick-actions-card {
  position: sticky;
  top: 20px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
}
</style>