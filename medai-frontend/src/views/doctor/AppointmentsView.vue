<template>
  <div class="appointments-view">
    <el-row :gutter="20">
      <!-- Calendar View -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="calendar-header">
              <div class="nav-buttons">
                <el-button @click="previousWeek" circle><el-icon><ArrowLeft /></el-icon></el-button>
                <el-button @click="goToToday">Today</el-button>
                <el-button @click="nextWeek" circle><el-icon><ArrowRight /></el-icon></el-button>
              </div>
              <h3>{{ currentWeekLabel }}</h3>
              <el-button type="primary" @click="showNewAppointmentDialog">
                <el-icon><Plus /></el-icon> New Appointment
              </el-button>
            </div>
          </template>

          <div v-loading="loading" class="week-calendar">
            <div class="calendar-grid">
              <div v-for="day in weekDays" :key="day.date" class="day-column" :class="{ today: day.isToday }">
                <div class="day-header">
                  <div class="day-name">{{ day.dayName }}</div>
                  <div class="day-date">{{ day.dateLabel }}</div>
                </div>
                <div class="day-appointments">
                  <div
                      v-for="apt in getAppointmentsForDay(day.date)"
                      :key="apt.id"
                      class="appointment-slot"
                      :class="apt.status"
                      @click="viewAppointment(apt)"
                  >
                    <div class="apt-time">{{ formatTime(apt.time) }}</div>
                    <div class="apt-patient">{{ apt.patientName || 'Unknown' }}</div>
                    <div class="apt-type">{{ apt.type }}</div>
                  </div>
                  <div v-if="getAppointmentsForDay(day.date).length === 0" class="no-appointments">
                    No appointments
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Today's Summary -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>Today's Appointments</span>
          </template>
          <div v-if="todayAppointments.length === 0">
            <el-empty description="No appointments today" :image-size="60" />
          </div>
          <div v-else class="today-list">
            <div
                v-for="apt in todayAppointments"
                :key="apt.id"
                class="today-item"
                :class="apt.status"
                @click="viewAppointment(apt)"
            >
              <div class="today-time">{{ formatTime(apt.time) }}</div>
              <div class="today-info">
                <div class="today-patient">{{ apt.patientName }}</div>
                <div class="today-type">{{ apt.type }}</div>
              </div>
              <el-tag :type="getStatusType(apt.status)" size="small">{{ apt.status }}</el-tag>
            </div>
          </div>
        </el-card>

        <!-- Quick Stats -->
        <el-card style="margin-top: 20px;">
          <template #header>
            <span>Statistics</span>
          </template>
          <el-row :gutter="10">
            <el-col :span="12">
              <el-statistic title="Today" :value="stats.todayCount" />
            </el-col>
            <el-col :span="12">
              <el-statistic title="This Week" :value="weekAppointments.length" />
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- New Appointment Dialog -->
    <el-dialog v-model="newDialogVisible" title="New Appointment" width="500px">
      <el-form :model="newForm" label-width="120px">
        <el-form-item label="Patient" required>
          <el-select v-model="newForm.patientId" placeholder="Select patient" style="width: 100%;" filterable>
            <el-option v-for="p in patients" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Date" required>
          <el-date-picker v-model="newForm.date" type="date" style="width: 100%;" :disabled-date="disabledDate" />
        </el-form-item>
        <el-form-item label="Time" required>
          <el-time-select v-model="newForm.time" start="08:00" step="00:30" end="18:00" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="Type" required>
          <el-select v-model="newForm.type" placeholder="Select type" style="width: 100%;">
            <el-option label="CT Scan" value="CT Scan" />
            <el-option label="MRI" value="MRI" />
            <el-option label="X-Ray" value="X-Ray" />
            <el-option label="Ultrasound" value="Ultrasound" />
            <el-option label="Consultation" value="Consultation" />
            <el-option label="Follow-up" value="Follow-up" />
          </el-select>
        </el-form-item>
        <el-form-item label="Duration">
          <el-select v-model="newForm.duration" style="width: 100%;">
            <el-option label="15 minutes" :value="15" />
            <el-option label="30 minutes" :value="30" />
            <el-option label="45 minutes" :value="45" />
            <el-option label="60 minutes" :value="60" />
          </el-select>
        </el-form-item>
        <el-form-item label="Location">
          <el-input v-model="newForm.location" placeholder="Room / Location" />
        </el-form-item>
        <el-form-item label="Notes">
          <el-input v-model="newForm.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="createAppointment" :loading="saving">Create</el-button>
      </template>
    </el-dialog>

    <!-- View Appointment Dialog -->
    <el-dialog v-model="viewDialogVisible" title="Appointment Details" width="500px">
      <div v-if="selectedAppointment">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Patient">{{ selectedAppointment.patientName }}</el-descriptions-item>
          <el-descriptions-item label="Date">{{ selectedAppointment.date }}</el-descriptions-item>
          <el-descriptions-item label="Time">{{ formatTime(selectedAppointment.time) }}</el-descriptions-item>
          <el-descriptions-item label="Type">{{ selectedAppointment.type }}</el-descriptions-item>
          <el-descriptions-item label="Location">{{ selectedAppointment.location || 'N/A' }}</el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="getStatusType(selectedAppointment.status)">{{ selectedAppointment.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Notes">{{ selectedAppointment.notes || 'N/A' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">Close</el-button>

        <!-- NEW: Create Report for this patient -->
        <el-button
            type="primary"
            @click="createReportForPatient"
        >
          <el-icon><Document /></el-icon>
          Create Report
        </el-button>

        <el-button
            v-if="selectedAppointment?.status === 'scheduled'"
            type="success"
            @click="completeAppointment"
        >
          Mark Complete
        </el-button>
        <el-button
            v-if="selectedAppointment?.status === 'scheduled'"
            type="danger"
            @click="cancelAppointment"
        >
          Cancel
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Plus } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

export default {
  name: 'AppointmentsView',
  components: { ArrowLeft, ArrowRight, Plus },
  setup() {
    const loading = ref(false)
    const saving = ref(false)
    const newDialogVisible = ref(false)
    const viewDialogVisible = ref(false)
    const selectedAppointment = ref(null)
    const weekAppointments = ref([])
    const patients = ref([])
    const currentWeekStart = ref(getMonday(new Date()))
    const router = useRouter()

    const stats = reactive({
      todayCount: 0,
      scheduledCount: 0
    })

    const newForm = reactive({
      patientId: null,
      date: null,
      time: '09:00',
      type: '',
      duration: 30,
      location: '',
      notes: ''
    })

    function getMonday(date) {
      const d = new Date(date)
      const day = d.getDay()
      const diff = d.getDate() - day + (day === 0 ? -6 : 1)
      return new Date(d.setDate(diff))
    }

    const weekDays = computed(() => {
      const days = []
      const today = new Date().toISOString().split('T')[0]

      for (let i = 0; i < 7; i++) {
        const date = new Date(currentWeekStart.value)
        date.setDate(date.getDate() + i)
        const dateStr = date.toISOString().split('T')[0]

        days.push({
          date: dateStr,
          dayName: date.toLocaleDateString('en', { weekday: 'short' }),
          dateLabel: date.toLocaleDateString('en', { month: 'short', day: 'numeric' }),
          isToday: dateStr === today
        })
      }
      return days
    })

    const currentWeekLabel = computed(() => {
      const start = new Date(currentWeekStart.value)
      const end = new Date(start)
      end.setDate(end.getDate() + 6)
      return `${start.toLocaleDateString('en', { month: 'short', day: 'numeric' })} - ${end.toLocaleDateString('en', { month: 'short', day: 'numeric', year: 'numeric' })}`
    })

    const todayAppointments = computed(() => {
      const today = new Date().toISOString().split('T')[0]
      return weekAppointments.value.filter(a => a.date === today)
    })

    const loadAppointments = async () => {
      loading.value = true
      try {
        const startDate = currentWeekStart.value.toISOString().split('T')[0]
        const endDate = new Date(currentWeekStart.value)
        endDate.setDate(endDate.getDate() + 6)

        const response = await http.get('/appointments/doctor/week', {
          params: { startDate, endDate: endDate.toISOString().split('T')[0] }
        })
        weekAppointments.value = response.data || []
      } catch (error) {
        console.error('Failed to load appointments:', error)
        weekAppointments.value = []
      } finally {
        loading.value = false
      }
    }

    const loadPatients = async () => {
      try {
        // Try to get all patients - backend should have /api/patients endpoint
        const response = await http.get('/patient/all', { params: { page: 0, size: 100 } })

        // Handle different response formats
        if (response.data.content) {
          // Paginated response
          patients.value = response.data.content.map(p => ({
            id: p.id,
            name: p.name || p.fullName || `Patient ${p.patientId}`,
            patientId: p.patientId
          }))
        } else if (Array.isArray(response.data)) {
          // Array response
          patients.value = response.data.map(p => ({
            id: p.id,
            name: p.name || p.fullName || `Patient ${p.patientId}`,
            patientId: p.patientId
          }))
        } else {
          patients.value = []
        }
      } catch (error) {
        console.error('Failed to load patients:', error)
        // Fallback: create mock patient for testing
        patients.value = [
          { id: 1, name: 'Patient Bob Wilson', patientId: 'P001' },
          { id: 2, name: 'Patient Jane Doe', patientId: 'P002' }
        ]
      }
    }

    const loadStats = async () => {
      try {
        const response = await http.get('/appointments/stats')
        stats.todayCount = response.data.todayCount || 0
        stats.scheduledCount = response.data.scheduledCount || 0
      } catch (error) {
        console.error('Failed to load stats:', error)
        // Set defaults instead of showing error
        stats.todayCount = 0
        stats.scheduledCount = 0
        // Don't show error notification to user
      }
    }

    const getAppointmentsForDay = (dateStr) => {
      return weekAppointments.value.filter(a => a.date === dateStr)
    }

    const previousWeek = () => {
      const newDate = new Date(currentWeekStart.value)
      newDate.setDate(newDate.getDate() - 7)
      currentWeekStart.value = newDate
      loadAppointments()
    }

    const nextWeek = () => {
      const newDate = new Date(currentWeekStart.value)
      newDate.setDate(newDate.getDate() + 7)
      currentWeekStart.value = newDate
      loadAppointments()
    }

    const goToToday = () => {
      currentWeekStart.value = getMonday(new Date())
      loadAppointments()
    }

    const showNewAppointmentDialog = () => {
      newForm.patientId = null
      newForm.date = null
      newForm.time = '09:00'
      newForm.type = ''
      newForm.duration = 30
      newForm.location = ''
      newForm.notes = ''
      newDialogVisible.value = true
    }

    const createAppointment = async () => {
      if (!newForm.patientId || !newForm.date || !newForm.type) {
        ElMessage.warning('Please fill in required fields')
        return
      }

      saving.value = true
      try {
        const user = JSON.parse(localStorage.getItem('user') || '{}')

        // Fix timezone issue - get local date string
        const selectedDate = new Date(newForm.date)
        const localDateStr = `${selectedDate.getFullYear()}-${String(selectedDate.getMonth() + 1).padStart(2, '0')}-${String(selectedDate.getDate()).padStart(2, '0')}`

        await http.post('/appointments', {
          patientId: newForm.patientId,
          doctorId: user.id,
          date: localDateStr,  // Use local date string instead of ISO
          time: newForm.time,
          type: newForm.type,
          duration: newForm.duration,
          location: newForm.location,
          notes: newForm.notes
        })

        ElMessage.success('Appointment created')
        newDialogVisible.value = false
        loadAppointments()
        loadStats()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || 'Failed to create appointment')
      } finally {
        saving.value = false
      }
    }

    const viewAppointment = (apt) => {
      selectedAppointment.value = apt
      viewDialogVisible.value = true
    }

    const completeAppointment = async () => {
      try {
        await http.put(`/appointments/${selectedAppointment.value.id}/status`, { status: 'completed' })
        ElMessage.success('Appointment marked as completed')
        viewDialogVisible.value = false
        loadAppointments()
      } catch (error) {
        ElMessage.error('Failed to update appointment')
      }
    }

    const cancelAppointment = async () => {
      try {
        await http.put(`/appointments/${selectedAppointment.value.id}/status`, { status: 'cancelled' })
        ElMessage.success('Appointment cancelled')
        viewDialogVisible.value = false
        loadAppointments()
      } catch (error) {
        ElMessage.error('Failed to cancel appointment')
      }
    }

    const createReportForPatient = () => {
      // Navigate to create report for this patient
      router.push(`/reports/new?patientId=${selectedAppointment.value.patientId}&patientName=${selectedAppointment.value.patientName}`)
    }

    const formatTime = (timeStr) => {
      if (!timeStr) return ''
      return timeStr.substring(0, 5)
    }

    const getStatusType = (status) => {
      const types = { scheduled: 'warning', completed: 'success', cancelled: 'danger' }
      return types[status] || 'info'
    }

    const disabledDate = (date) => {
      return date < new Date(new Date().setHours(0, 0, 0, 0))
    }

    onMounted(() => {
      loadAppointments()
      loadPatients()
      loadStats()
    })

    return {
      loading, saving, newDialogVisible, viewDialogVisible, selectedAppointment,
      weekAppointments, patients, weekDays, currentWeekLabel, todayAppointments, stats,
      newForm, loadAppointments, getAppointmentsForDay, previousWeek, nextWeek, goToToday,
      showNewAppointmentDialog, createAppointment, viewAppointment, completeAppointment,
      cancelAppointment, formatTime, getStatusType, disabledDate,createReportForPatient
    }
  }
}
</script>

<style scoped>
.appointments-view { padding: 20px; }

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.calendar-header h3 { margin: 0; }

.nav-buttons { display: flex; gap: 10px; align-items: center; }

.week-calendar { min-height: 400px; }

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
}

.day-column {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  min-height: 350px;
}

.day-column.today { border-color: #409eff; background-color: #ecf5ff; }

.day-header {
  padding: 10px;
  text-align: center;
  border-bottom: 1px solid #ebeef5;
  background-color: #f5f7fa;
  border-radius: 8px 8px 0 0;
}

.day-column.today .day-header { background-color: #409eff; color: white; }

.day-name { font-weight: 600; }
.day-date { font-size: 12px; color: #909399; }
.day-column.today .day-date { color: rgba(255,255,255,0.8); }

.day-appointments { padding: 10px; }

.appointment-slot {
  padding: 8px;
  margin-bottom: 8px;
  border-radius: 4px;
  background-color: #e1f3d8;
  cursor: pointer;
  transition: all 0.2s;
}

.appointment-slot:hover { transform: translateY(-2px); box-shadow: 0 2px 8px rgba(0,0,0,0.1); }

.appointment-slot.completed { background-color: #d9ecff; }
.appointment-slot.cancelled { background-color: #fde2e2; text-decoration: line-through; }

.apt-time { font-weight: 600; color: #409eff; font-size: 12px; }
.apt-patient { font-weight: 500; font-size: 13px; }
.apt-type { font-size: 11px; color: #909399; }

.no-appointments { text-align: center; color: #c0c4cc; font-size: 12px; padding: 20px; }

.today-list { display: flex; flex-direction: column; gap: 10px; }

.today-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background-color: #f5f7fa;
  cursor: pointer;
  gap: 15px;
}

.today-item:hover { background-color: #ecf5ff; }

.today-time { font-size: 16px; font-weight: 600; color: #409eff; min-width: 50px; }
.today-info { flex: 1; }
.today-patient { font-weight: 500; }
.today-type { font-size: 12px; color: #909399; }
</style>