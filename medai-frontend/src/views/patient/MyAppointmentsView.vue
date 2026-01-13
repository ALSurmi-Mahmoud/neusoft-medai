<template>
  <div class="my-appointments">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>My Appointments</span>
          <el-button type="primary" @click="showBookDialog">
            <el-icon><Plus /></el-icon> Book Appointment
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- Upcoming -->
        <el-tab-pane label="Upcoming" name="upcoming">
          <div v-if="loading" class="loading-container">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="upcomingAppointments.length === 0">
            <el-empty description="No upcoming appointments">
              <el-button type="primary" @click="showBookDialog">Book Now</el-button>
            </el-empty>
          </div>
          <div v-else class="appointments-grid">
            <el-card v-for="apt in upcomingAppointments" :key="apt.id" class="appointment-card" shadow="hover">
              <div class="apt-header">
                <div class="apt-date-box">
                  <div class="date-day">{{ getDay(apt.date) }}</div>
                  <div class="date-month">{{ getMonth(apt.date) }}</div>
                </div>
                <el-tag :type="getStatusType(apt.status)">{{ apt.status }}</el-tag>
              </div>
              <div class="apt-body">
                <h4>{{ apt.type || 'Medical Appointment' }}</h4>
                <p><el-icon><User /></el-icon> {{ apt.doctorName || 'Doctor TBD' }}</p>
                <p><el-icon><Clock /></el-icon> {{ formatTime(apt.time) }}</p>
                <p v-if="apt.location"><el-icon><Location /></el-icon> {{ apt.location }}</p>
              </div>
              <div class="apt-actions" v-if="apt.status === 'scheduled' || apt.status === 'pending'">
                <el-button size="small" type="danger" @click="cancelAppointment(apt)">Cancel</el-button>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <!-- Past -->
        <el-tab-pane label="Past" name="past">
          <el-table :data="pastAppointments" style="width: 100%">
            <el-table-column prop="date" label="Date" width="120" />
            <el-table-column prop="time" label="Time" width="100">
              <template #default="{ row }">
                {{ formatTime(row.time) }}
              </template>
            </el-table-column>
            <el-table-column prop="type" label="Type" width="150" />
            <el-table-column prop="doctorName" label="Doctor" width="150" />
            <el-table-column prop="status" label="Status" width="120">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="notes" label="Notes" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Book Appointment Dialog -->
    <el-dialog v-model="bookDialogVisible" title="Book New Appointment" width="550px">
      <el-form :model="bookForm" label-width="140px" :rules="bookRules" ref="bookFormRef">
        <el-form-item label="Select Doctor" prop="doctorId">
          <el-select v-model="bookForm.doctorId" placeholder="Choose a doctor" style="width: 100%;" filterable>
            <el-option
                v-for="doc in doctors"
                :key="doc.id"
                :label="`${doc.name} - ${doc.department || 'General'}`"
                :value="doc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Appointment Type" prop="type">
          <el-select v-model="bookForm.type" placeholder="Select type" style="width: 100%;">
            <el-option label="General Consultation" value="Consultation" />
            <el-option label="CT Scan" value="CT Scan" />
            <el-option label="MRI" value="MRI" />
            <el-option label="X-Ray" value="X-Ray" />
            <el-option label="Ultrasound" value="Ultrasound" />
            <el-option label="Follow-up" value="Follow-up" />
          </el-select>
        </el-form-item>
        <el-form-item label="Preferred Date" prop="date">
          <el-date-picker
              v-model="bookForm.date"
              type="date"
              style="width: 100%;"
              :disabled-date="disabledDate"
              placeholder="Select date"
          />
        </el-form-item>
        <el-form-item label="Preferred Time" prop="time">
          <el-time-select
              v-model="bookForm.time"
              start="08:00"
              step="00:30"
              end="17:00"
              style="width: 100%;"
              placeholder="Select time"
          />
        </el-form-item>
        <el-form-item label="Reason for Visit">
          <el-input
              v-model="bookForm.reason"
              type="textarea"
              :rows="3"
              placeholder="Please describe the reason for your appointment"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="submitBooking" :loading="submitting">
          Book Appointment
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import http from '../../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, User, Clock, Location } from '@element-plus/icons-vue'

export default {
  name: 'MyAppointmentsView',
  components: { Plus, User, Clock, Location },
  setup() {
    const loading = ref(true)
    const submitting = ref(false)
    const activeTab = ref('upcoming')
    const bookDialogVisible = ref(false)
    const bookFormRef = ref(null)
    const appointments = ref([])
    const doctors = ref([])

    const bookForm = reactive({
      doctorId: null,
      type: '',
      date: null,
      time: '',
      reason: ''
    })

    const bookRules = {
      doctorId: [{ required: true, message: 'Please select a doctor', trigger: 'change' }],
      type: [{ required: true, message: 'Please select appointment type', trigger: 'change' }],
      date: [{ required: true, message: 'Please select a date', trigger: 'change' }],
      time: [{ required: true, message: 'Please select a time', trigger: 'change' }]
    }

    const upcomingAppointments = computed(() => {
      const today = new Date().toISOString().split('T')[0]
      return appointments.value.filter(a => a.date >= today && a.status !== 'cancelled' && a.status !== 'completed')
    })

    const pastAppointments = computed(() => {
      const today = new Date().toISOString().split('T')[0]
      return appointments.value.filter(a => a.date < today || a.status === 'completed' || a.status === 'cancelled')
    })

    const loadAppointments = async () => {
      loading.value = true
      try {
        const response = await http.get('/appointments/patient')
        appointments.value = response.data || []
      } catch (error) {
        console.error('Failed to load appointments:', error)
        appointments.value = []
      } finally {
        loading.value = false
      }
    }

    const loadDoctors = async () => {
      try {
        const response = await http.get('/appointments/doctors')
        doctors.value = response.data || []
      } catch (error) {
        console.error('Failed to load doctors:', error)
        // Fallback: try to get from users
        try {
          const usersRes = await http.get('/admin/users', { params: { role: 'DOCTOR', size: 100 } })
          doctors.value = (usersRes.data.content || []).map(u => ({
            id: u.id,
            name: u.fullName,
            department: u.department
          }))
        } catch (e) {
          doctors.value = []
        }
      }
    }

    const showBookDialog = () => {
      bookForm.doctorId = null
      bookForm.type = ''
      bookForm.date = null
      bookForm.time = ''
      bookForm.reason = ''
      bookDialogVisible.value = true
    }

    const disabledDate = (date) => {
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      return date < today
    }

    const submitBooking = async () => {
      if (!bookFormRef.value) return

      try {
        await bookFormRef.value.validate()
      } catch (e) {
        return
      }

      if (!bookForm.doctorId || !bookForm.date || !bookForm.time || !bookForm.type) {
        ElMessage.warning('Please fill in all required fields')
        return
      }

      submitting.value = true
      try {
        // Create appointment - UNCOMMENTED
        const response = await http.post('/appointments', {
          doctorId: bookForm.doctorId,
          date: new Date(bookForm.date).toISOString().split('T')[0],
          time: bookForm.time,
          type: bookForm.type,
          reason: bookForm.reason,
          notes: bookForm.reason
        })

        ElMessage.success('Appointment booked successfully!')
        bookDialogVisible.value = false
        loadAppointments()

        ElMessage.success('Appointment booked successfully!')
        bookDialogVisible.value = false
        loadAppointments()
      } catch (error) {
        console.error('Booking failed:', error)
        ElMessage.error(error.response?.data?.message || 'Failed to book appointment')
      } finally {
        submitting.value = false
      }
    }

    const cancelAppointment = (apt) => {
      ElMessageBox.confirm(
          'Are you sure you want to cancel this appointment?',
          'Cancel Appointment',
          { confirmButtonText: 'Yes, Cancel', cancelButtonText: 'No', type: 'warning' }
      ).then(async () => {
        try {
          await http.put(`/appointments/${apt.id}/status`, { status: 'cancelled' })
          ElMessage.success('Appointment cancelled')
          loadAppointments()
        } catch (error) {
          ElMessage.error('Failed to cancel appointment')
        }
      }).catch(() => {})
    }

    const getDay = (dateStr) => {
      if (!dateStr) return '--'
      return new Date(dateStr).getDate()
    }

    const getMonth = (dateStr) => {
      if (!dateStr) return ''
      return new Date(dateStr).toLocaleString('en', { month: 'short' })
    }

    const formatTime = (timeStr) => {
      if (!timeStr) return ''
      return timeStr.substring(0, 5)
    }

    const getStatusType = (status) => {
      const types = {
        'pending': 'warning',
        'scheduled': 'primary',
        'confirmed': 'success',
        'completed': 'success',
        'cancelled': 'danger'
      }
      return types[status] || 'info'
    }

    onMounted(() => {
      loadAppointments()
      loadDoctors()
    })

    return {
      loading, submitting, activeTab, bookDialogVisible, bookFormRef,
      appointments, doctors, bookForm, bookRules,
      upcomingAppointments, pastAppointments,
      showBookDialog, disabledDate, submitBooking, cancelAppointment,
      getDay, getMonth, formatTime, getStatusType
    }
  }
}
</script>

<style scoped>
.my-appointments { padding: 20px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-container { padding: 20px; }

.appointments-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.appointment-card { border-radius: 12px; }

.apt-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.apt-date-box {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  color: white;
  padding: 12px 18px;
  border-radius: 10px;
  text-align: center;
  min-width: 60px;
}

.date-day { font-size: 28px; font-weight: bold; line-height: 1; }
.date-month { font-size: 12px; text-transform: uppercase; margin-top: 4px; }

.apt-body h4 { margin: 0 0 12px; color: #333; font-size: 16px; }

.apt-body p {
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.apt-actions {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}
</style>