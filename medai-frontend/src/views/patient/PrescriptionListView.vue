<template>
  <div class="prescription-list-view">
    <el-page-header @back="$router.back()" style="margin-bottom: 20px;">
      <template #content>
        <span class="page-title">My Prescriptions</span>
      </template>
    </el-page-header>

    <el-card>
      <!-- Filter Tabs -->
      <el-tabs v-model="activeTab" @tab-click="filterPrescriptions">
        <el-tab-pane name="active">
          <template #label>
            <span>Active <el-badge :value="activePrescriptions.length" type="success" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="all">
          <template #label>
            <span>All <el-badge :value="allPrescriptions.length" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="completed">
          <template #label>
            <span>Completed <el-badge :value="completedPrescriptions.length" type="info" /></span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- Loading State -->
      <div v-if="loading" v-loading="true" style="min-height: 300px;"></div>

      <!-- Empty State -->
      <div v-else-if="displayedPrescriptions.length === 0" class="empty-state">
        <el-empty description="No prescriptions found" />
      </div>

      <!-- Prescriptions Grid -->
      <div v-else class="prescriptions-grid">
        <el-card
            v-for="rx in displayedPrescriptions"
            :key="rx.id"
            class="prescription-card"
            :class="getStatusClass(rx.status)"
            shadow="hover"
        >
          <!-- Medication Header -->
          <div class="rx-header">
            <div class="rx-name">
              <h3>{{ rx.medicationName }}</h3>
              <div class="rx-meta">
                <el-tag :type="getStatusType(rx.status)" size="small">
                  {{ rx.status }}
                </el-tag>
                <span class="rx-number">Rx# {{ rx.prescriptionNumber }}</span>
              </div>
            </div>
          </div>

          <!-- Dosage Info -->
          <el-divider style="margin: 12px 0;" />
          <div class="rx-dosage">
            <div class="dosage-main">
              <el-icon><FirstAidKit /></el-icon>
              <div>
                <div class="dosage-text"><strong>{{ rx.dosage }} {{ rx.dosageForm }}</strong></div>
                <div class="frequency-text">{{ rx.frequency }}</div>
              </div>
            </div>
          </div>

          <!-- Take Schedule -->
          <div class="rx-schedule" v-if="hasSchedule(rx)">
            <div class="schedule-label">Take:</div>
            <div class="schedule-times">
              <el-tag v-if="rx.timingMorning" size="small" effect="plain">☀️ Morning</el-tag>
              <el-tag v-if="rx.timingAfternoon" size="small" effect="plain" type="info">🌤️ Afternoon</el-tag>
              <el-tag v-if="rx.timingEvening" size="small" effect="plain" type="warning">🌙 Evening</el-tag>
              <el-tag v-if="rx.timingBedtime" size="small" effect="plain" type="success">🌜 Bedtime</el-tag>
              <el-tag v-if="rx.timingAsNeeded" size="small" effect="plain" type="danger">⚠️ As Needed</el-tag>
            </div>
          </div>

          <!-- Instructions -->
          <div class="rx-instructions" v-if="rx.patientInstructions || rx.instructions">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ rx.patientInstructions || rx.instructions }}</span>
          </div>

          <!-- Refill Info -->
          <div class="rx-refills" v-if="rx.status === 'active'">
            <el-divider style="margin: 12px 0;" />
            <div class="refill-header">
              <span class="refill-label">Refills:</span>
              <el-tag v-if="needsRefill(rx)" type="danger" size="small" effect="dark">
                <el-icon><Warning /></el-icon> Refill Soon!
              </el-tag>
            </div>
            <el-progress
                :percentage="getRefillPercentage(rx)"
                :color="getRefillColor(rx)"
                :stroke-width="8"
            >
              <span class="refill-count">{{ rx.refillsRemaining }}/{{ rx.refillsAllowed }}</span>
            </el-progress>
          </div>

          <!-- Dates -->
          <el-divider style="margin: 12px 0;" />
          <div class="rx-dates">
            <div class="date-item">
              <el-icon><Calendar /></el-icon>
              <div>
                <div class="date-label">Prescribed</div>
                <div class="date-value">{{ formatDate(rx.prescribedDate) }}</div>
              </div>
            </div>
            <div class="date-item" v-if="rx.endDate && rx.status === 'active'">
              <el-icon><Clock /></el-icon>
              <div>
                <div class="date-label">Ends</div>
                <div class="date-value">{{ formatDate(rx.endDate) }}</div>
              </div>
            </div>
          </div>

          <!-- Doctor Info -->
          <div class="rx-doctor">
            <el-icon><User /></el-icon>
            <span>Dr. {{ rx.doctorName }}</span>
          </div>

          <!-- Warnings -->
          <el-alert
              v-if="hasWarnings(rx)"
              type="warning"
              :closable="false"
              style="margin-top: 10px;"
              effect="dark"
          >
            <template #title>
              <strong>⚠️ {{ getWarningCount(rx) }} Warning(s)</strong>
            </template>
            Click to view details
          </el-alert>

          <!-- Actions -->
          <div class="rx-actions">
            <el-button type="primary" size="small" @click="viewPrescription(rx)">
              <el-icon><View /></el-icon> View Details
            </el-button>
          </div>
        </el-card>
      </div>
    </el-card>

    <!-- Detail Dialog -->
    <PrescriptionDetailDialog
        v-model="detailDialogVisible"
        :prescription="selectedPrescription"
    />
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import {
  FirstAidKit, InfoFilled, User, Calendar, Clock, Warning, View
} from '@element-plus/icons-vue'
import PrescriptionDetailDialog from '../../components/PrescriptionDetailDialog.vue'

export default {
  name: 'PrescriptionListView',
  components: {
    FirstAidKit, InfoFilled, User, Calendar, Clock, Warning, View,
    PrescriptionDetailDialog
  },
  setup() {
    const loading = ref(false)
    const activeTab = ref('active')
    const allPrescriptions = ref([])
    const displayedPrescriptions = ref([])
    const detailDialogVisible = ref(false)
    const selectedPrescription = ref(null)

    const activePrescriptions = computed(() => {
      return allPrescriptions.value.filter(rx => rx.status === 'active')
    })

    const completedPrescriptions = computed(() => {
      return allPrescriptions.value.filter(rx => rx.status === 'completed')
    })

    const loadPrescriptions = async () => {
      loading.value = true
      try {
        const response = await http.get('/prescriptions/my-prescriptions')
        allPrescriptions.value = response.data
        filterPrescriptions()
      } catch (error) {
        console.error('Failed to load prescriptions:', error)
        ElMessage.error('Failed to load prescriptions')
        allPrescriptions.value = []
      } finally {
        loading.value = false
      }
    }

    const filterPrescriptions = () => {
      switch (activeTab.value) {
        case 'active':
          displayedPrescriptions.value = activePrescriptions.value
          break
        case 'completed':
          displayedPrescriptions.value = completedPrescriptions.value
          break
        default:
          displayedPrescriptions.value = allPrescriptions.value
      }
    }

    const viewPrescription = (rx) => {
      selectedPrescription.value = rx
      detailDialogVisible.value = true
    }

    const hasSchedule = (rx) => {
      return rx.timingMorning || rx.timingAfternoon || rx.timingEvening ||
          rx.timingBedtime || rx.timingAsNeeded
    }

    const getStatusClass = (status) => {
      return `status-${status}`
    }

    const getStatusType = (status) => {
      const types = {
        active: 'success',
        completed: 'info',
        cancelled: 'danger',
        expired: 'warning'
      }
      return types[status] || 'info'
    }

    const getRefillPercentage = (rx) => {
      if (!rx.refillsAllowed) return 0
      return (rx.refillsRemaining / rx.refillsAllowed) * 100
    }

    const getRefillColor = (rx) => {
      const percentage = getRefillPercentage(rx)
      if (percentage === 0) return '#f56c6c'
      if (percentage < 50) return '#e6a23c'
      return '#67c23a'
    }

    const needsRefill = (rx) => {
      return rx.status === 'active' && rx.refillsRemaining <= 1
    }

    const hasWarnings = (rx) => {
      return rx.interactionWarnings &&
          rx.interactionWarnings.warnings &&
          rx.interactionWarnings.warnings.length > 0
    }

    const getWarningCount = (rx) => {
      return rx.interactionWarnings?.warnings?.length || 0
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    }

    onMounted(() => {
      loadPrescriptions()
    })

    return {
      loading,
      activeTab,
      allPrescriptions,
      displayedPrescriptions,
      activePrescriptions,
      completedPrescriptions,
      detailDialogVisible,
      selectedPrescription,
      loadPrescriptions,
      filterPrescriptions,
      viewPrescription,
      hasSchedule,
      getStatusClass,
      getStatusType,
      getRefillPercentage,
      getRefillColor,
      needsRefill,
      hasWarnings,
      getWarningCount,
      formatDate
    }
  }
}
</script>

<style scoped>
.prescription-list-view {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.prescriptions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.prescription-card {
  cursor: pointer;
  transition: all 0.3s;
  border-left: 4px solid #409eff;
}

.prescription-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.prescription-card.status-active {
  border-left-color: #67c23a;
}

.prescription-card.status-completed {
  border-left-color: #909399;
  opacity: 0.85;
}

.prescription-card.status-cancelled {
  border-left-color: #f56c6c;
  opacity: 0.75;
}

.prescription-card.status-expired {
  border-left-color: #e6a23c;
  opacity: 0.80;
}

.rx-header {
  margin-bottom: 10px;
}

.rx-name h3 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.rx-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rx-number {
  font-size: 12px;
  color: #909399;
}

.rx-dosage {
  margin: 12px 0;
}

.dosage-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

.dosage-main .el-icon {
  font-size: 24px;
  margin-top: 2px;
}

.dosage-text {
  font-size: 16px;
  margin-bottom: 4px;
}

.frequency-text {
  font-size: 14px;
  opacity: 0.9;
}

.rx-schedule {
  margin: 15px 0;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.schedule-label {
  font-size: 12px;
  color: #606266;
  font-weight: 600;
  margin-bottom: 8px;
}

.schedule-times {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.rx-instructions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  background-color: #ecf5ff;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
  margin: 12px 0;
}

.rx-refills {
  margin: 12px 0;
}

.refill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.refill-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.refill-count {
  font-size: 12px;
  font-weight: 600;
}

.rx-dates {
  display: flex;
  justify-content: space-between;
  gap: 15px;
}

.date-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.date-item .el-icon {
  font-size: 18px;
  color: #909399;
}

.date-label {
  font-size: 11px;
  color: #909399;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.date-value {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.rx-doctor {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  font-size: 13px;
  color: #606266;
}

.rx-actions {
  margin-top: 15px;
  text-align: center;
}

.rx-actions .el-button {
  width: 100%;
}

.empty-state {
  padding: 80px 20px;
  text-align: center;
}
</style>