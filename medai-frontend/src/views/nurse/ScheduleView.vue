<template>
  <div class="nurse-schedule">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Today's Schedule</span>
          <span class="date-display">{{ todayDate }}</span>
        </div>
      </template>

      <div v-loading="loading">
        <!-- Stats Summary -->
        <el-row :gutter="20" style="margin-bottom: 20px;">
          <el-col :span="8">
            <el-statistic title="Total Appointments" :value="appointments.length">
              <template #prefix>
                <el-icon><Calendar /></el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="8">
            <el-statistic title="Completed" :value="completedCount">
              <template #prefix>
                <el-icon style="color: #67c23a"><CircleCheck /></el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="8">
            <el-statistic title="Pending" :value="pendingCount">
              <template #prefix>
                <el-icon style="color: #e6a23c"><Clock /></el-icon>
              </template>
            </el-statistic>
          </el-col>
        </el-row>

        <!-- Appointments Table -->
        <el-table
            :data="appointments"
            style="width: 100%"
            :default-sort="{ prop: 'time', order: 'ascending' }"
        >
          <el-table-column prop="time" label="Time" width="100" sortable>
            <template #default="{ row }">
              <strong>{{ formatTime(row.time) }}</strong>
            </template>
          </el-table-column>

          <el-table-column prop="patientName" label="Patient" width="180">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 8px;">
                <el-icon><User /></el-icon>
                {{ row.patientName }}
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="type" label="Exam Type" width="150">
            <template #default="{ row }">
              <el-tag size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="doctorName" label="Doctor" width="180" />

          <el-table-column prop="location" label="Location" width="120" />

          <el-table-column prop="status" label="Status" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Actions" width="150" fixed="right">
            <template #default="{ row }">
              <el-button
                  v-if="row.status === 'scheduled'"
                  size="small"
                  type="primary"
                  @click="markAsArrived(row)"
              >
                Check In
              </el-button>
              <el-button
                  size="small"
                  @click="viewDetails(row)"
              >
                View
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Empty State -->
        <el-empty
            v-if="!loading && appointments.length === 0"
            description="No appointments scheduled for today"
        />
      </div>
    </el-card>

    <!-- Appointment Details Dialog -->
    <el-dialog v-model="detailsVisible" title="Appointment Details" width="500px">
      <div v-if="selectedAppointment">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Patient">
            {{ selectedAppointment.patientName }}
          </el-descriptions-item>
          <el-descriptions-item label="Time">
            {{ formatTime(selectedAppointment.time) }}
          </el-descriptions-item>
          <el-descriptions-item label="Type">
            {{ selectedAppointment.type }}
          </el-descriptions-item>
          <el-descriptions-item label="Doctor">
            {{ selectedAppointment.doctorName }}
          </el-descriptions-item>
          <el-descriptions-item label="Location">
            {{ selectedAppointment.location || 'Not specified' }}
          </el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="getStatusType(selectedAppointment.status)">
              {{ selectedAppointment.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Notes">
            {{ selectedAppointment.notes || 'No notes' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailsVisible = false">Close</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { Calendar, Clock, User, CircleCheck } from '@element-plus/icons-vue'

export default {
  name: 'NurseScheduleView',
  components: { Calendar, Clock, User, CircleCheck },
  setup() {
    const loading = ref(true)
    const appointments = ref([])
    const detailsVisible = ref(false)
    const selectedAppointment = ref(null)

    const todayDate = computed(() => {
      return new Date().toLocaleDateString('en-US', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      })
    })

    const completedCount = computed(() => {
      return appointments.value.filter(a => a.status === 'completed').length
    })

    const pendingCount = computed(() => {
      return appointments.value.filter(a =>
          a.status === 'scheduled' || a.status === 'confirmed'
      ).length
    })

    const loadAppointments = async () => {
      loading.value = true
      try {
        const response = await http.get('/appointments/today')
        appointments.value = response.data || []
      } catch (error) {
        console.error('Failed to load appointments:', error)
        appointments.value = []
      } finally {
        loading.value = false
      }
    }

    const markAsArrived = async (appointment) => {
      try {
        await http.put(`/appointments/${appointment.id}/status`, {
          status: 'confirmed'
        })
        ElMessage.success('Patient checked in')
        loadAppointments()
      } catch (error) {
        ElMessage.error('Failed to update status')
      }
    }

    const viewDetails = (appointment) => {
      selectedAppointment.value = appointment
      detailsVisible.value = true
    }

    const formatTime = (timeStr) => {
      if (!timeStr) return ''
      return timeStr.substring(0, 5)
    }

    const getStatusType = (status) => {
      const types = {
        'scheduled': 'warning',
        'confirmed': 'primary',
        'completed': 'success',
        'cancelled': 'danger'
      }
      return types[status] || 'info'
    }

    onMounted(() => {
      loadAppointments()
    })

    return {
      loading,
      appointments,
      detailsVisible,
      selectedAppointment,
      todayDate,
      completedCount,
      pendingCount,
      loadAppointments,
      markAsArrived,
      viewDetails,
      formatTime,
      getStatusType
    }
  }
}
</script>

<style scoped>
.nurse-schedule {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.date-display {
  font-size: 14px;
  color: #909399;
}
</style>