<template>
  <el-dialog
      v-model="visible"
      title="Prescription Details"
      width="700px"
      :close-on-click-modal="false"
  >
    <div v-if="prescription" class="prescription-detail" v-loading="loading">
      <!-- E-Prescription Header -->
      <div class="rx-e-prescription">
        <div class="e-rx-header">
          <div class="clinic-info">
            <h2>🏥 MedAI Health Center</h2>
            <p>Electronic Prescription</p>
          </div>
          <div class="rx-qr">
            <div class="qr-placeholder">
              <el-icon :size="80"><Grid /></el-icon>
              <div class="qr-text">QR Code</div>
            </div>
            <div class="verification-code">
              PIN: {{ prescription.verificationCode || 'N/A' }}
            </div>
          </div>
        </div>

        <el-divider />

        <!-- Prescription Number & Status -->
        <div class="rx-id-status">
          <div class="rx-id">
            <strong>Rx Number:</strong>
            <el-tag type="primary">{{ prescription.prescriptionNumber }}</el-tag>
          </div>
          <div class="rx-status-badge">
            <el-tag :type="getStatusType(prescription.status)" size="large">
              {{ prescription.status.toUpperCase() }}
            </el-tag>
          </div>
        </div>

        <!-- Date -->
        <div class="rx-date">
          <strong>Date:</strong> {{ formatDate(prescription.prescribedDate) }}
        </div>
      </div>

      <el-divider />

      <!-- Patient Information -->
      <div class="section">
        <h3>Patient Information</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Patient Name">
            {{ prescription.patientName }}
          </el-descriptions-item>
          <el-descriptions-item label="Patient ID">
            {{ prescription.patientIdNumber }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- Medication Details -->
      <div class="section">
        <h3>℞ Medication</h3>
        <div class="medication-box">
          <div class="med-name-big">{{ prescription.medicationName }}</div>
          <div class="med-details-grid">
            <div class="detail-item">
              <span class="label">Dosage:</span>
              <span class="value">{{ prescription.dosage }} {{ prescription.dosageForm }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Route:</span>
              <span class="value">{{ prescription.route }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Frequency:</span>
              <span class="value">{{ prescription.frequency }} ({{ prescription.frequencyCode }})</span>
            </div>
            <div class="detail-item">
              <span class="label">Quantity:</span>
              <span class="value">{{ prescription.quantity }}</span>
            </div>
            <div class="detail-item" v-if="prescription.durationDays">
              <span class="label">Duration:</span>
              <span class="value">{{ prescription.durationDays }} days</span>
            </div>
            <div class="detail-item">
              <span class="label">Refills:</span>
              <span class="value">{{ prescription.refillsAllowed }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Take Schedule -->
      <div class="section" v-if="hasSchedule()">
        <h3>Take Schedule</h3>
        <div class="schedule-badges">
          <el-tag v-if="prescription.timingMorning" size="large" effect="plain">
            ☀️ Morning
          </el-tag>
          <el-tag v-if="prescription.timingAfternoon" size="large" effect="plain" type="info">
            🌤️ Afternoon
          </el-tag>
          <el-tag v-if="prescription.timingEvening" size="large" effect="plain" type="warning">
            🌙 Evening
          </el-tag>
          <el-tag v-if="prescription.timingBedtime" size="large" effect="plain" type="success">
            🌜 Bedtime
          </el-tag>
          <el-tag v-if="prescription.timingAsNeeded" size="large" effect="plain" type="danger">
            ⚠️ As Needed
          </el-tag>
        </div>
      </div>

      <!-- Instructions -->
      <div class="section">
        <h3>Instructions</h3>
        <el-alert
            :title="prescription.patientInstructions || prescription.instructions"
            type="info"
            :closable="false"
            show-icon
        />
      </div>

      <!-- Warnings -->
      <div class="section" v-if="hasWarnings()">
        <h3>⚠️ Warnings & Precautions</h3>
        <el-alert
            v-for="(warning, index) in prescription.interactionWarnings.warnings"
            :key="index"
            :type="warning.level === 'CRITICAL' ? 'error' : warning.level === 'WARNING' ? 'warning' : 'info'"
            :title="warning.message"
            style="margin-bottom: 10px;"
            :closable="false"
        >
          <p><strong>Detail:</strong> {{ warning.detail }}</p>
          <p><strong>Action:</strong> {{ warning.action }}</p>
        </el-alert>
      </div>

      <!-- Dates -->
      <div class="section">
        <h3>Important Dates</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Prescribed">
            {{ formatDate(prescription.prescribedDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="Start Date">
            {{ formatDate(prescription.startDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="End Date" v-if="prescription.endDate">
            {{ formatDate(prescription.endDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="Last Refill" v-if="prescription.lastRefillDate">
            {{ formatDate(prescription.lastRefillDate) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- Doctor Information -->
      <div class="section">
        <h3>Prescribing Doctor</h3>
        <div class="doctor-signature">
          <el-icon :size="40"><User /></el-icon>
          <div>
            <div class="doctor-name">Dr. {{ prescription.doctorName }}</div>
            <div class="doctor-sig">Digital Signature: ✓ Verified</div>
          </div>
        </div>
      </div>

      <!-- Pharmacy Information -->
      <div class="section" v-if="prescription.pharmacyName">
        <h3>Pharmacy</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Pharmacy Name">
            {{ prescription.pharmacyName }}
          </el-descriptions-item>
          <el-descriptions-item label="Phone">
            {{ prescription.pharmacyPhone || 'N/A' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- Refill Progress -->
      <div class="section" v-if="prescription.status === 'active'">
        <h3>Refill Status</h3>
        <div class="refill-progress-box">
          <div class="refill-info-row">
            <span>Refills Remaining:</span>
            <strong>{{ prescription.refillsRemaining }} of {{ prescription.refillsAllowed }}</strong>
          </div>
          <el-progress
              :percentage="getRefillPercentage()"
              :color="getRefillColor()"
              :stroke-width="12"
              :show-text="false"
          />
        </div>
      </div>

      <!-- Admin Actions (for doctors) -->
      <div class="section admin-actions" v-if="showAdminActions">
        <el-divider />
        <h3>Actions</h3>
        <div class="action-buttons">
          <el-button type="primary" @click="handlePrint">
            <el-icon><Printer /></el-icon> Print Prescription
          </el-button>
          <el-button type="success" @click="handleRefill" :disabled="!canRefill()">
            <el-icon><RefreshRight /></el-icon> Process Refill
          </el-button>
          <el-button type="danger" @click="handleCancel" :disabled="prescription.status !== 'active'">
            <el-icon><Close /></el-icon> Cancel Prescription
          </el-button>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">Close</el-button>
      <el-button type="primary" @click="handlePrint">
        <el-icon><Printer /></el-icon> Print
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Grid, User, Printer, RefreshRight, Close } from '@element-plus/icons-vue'
import http from '../utils/http'

export default {
  name: 'PrescriptionDetailDialog',
  components: { Grid, User, Printer, RefreshRight, Close },
  props: {
    modelValue: Boolean,
    prescription: {
      type: Object,
      default: null
    },
    showAdminActions: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'refilled', 'cancelled'],
  setup(props, { emit }) {
    const loading = ref(false)

    const visible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    const hasSchedule = () => {
      if (!props.prescription) return false
      return props.prescription.timingMorning ||
          props.prescription.timingAfternoon ||
          props.prescription.timingEvening ||
          props.prescription.timingBedtime ||
          props.prescription.timingAsNeeded
    }

    const hasWarnings = () => {
      return props.prescription?.interactionWarnings?.warnings?.length > 0
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

    const getRefillPercentage = () => {
      if (!props.prescription.refillsAllowed) return 0
      return (props.prescription.refillsRemaining / props.prescription.refillsAllowed) * 100
    }

    const getRefillColor = () => {
      const percentage = getRefillPercentage()
      if (percentage === 0) return '#f56c6c'
      if (percentage < 50) return '#e6a23c'
      return '#67c23a'
    }

    const canRefill = () => {
      return props.prescription?.status === 'active' &&
          props.prescription?.refillsRemaining > 0
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      })
    }

    const handlePrint = () => {
      window.print()
    }

    const handleRefill = async () => {
      try {
        await ElMessageBox.confirm(
            'Process refill for this prescription?',
            'Confirm Refill',
            {
              confirmButtonText: 'Process Refill',
              cancelButtonText: 'Cancel',
              type: 'info'
            }
        )

        loading.value = true
        await http.post(`/prescriptions/${props.prescription.id}/refill`)

        ElMessage.success('Refill processed successfully')
        emit('refilled')
        visible.value = false
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Failed to process refill:', error)
          ElMessage.error(error.response?.data?.message || 'Failed to process refill')
        }
      } finally {
        loading.value = false
      }
    }

    const handleCancel = async () => {
      try {
        const { value } = await ElMessageBox.prompt(
            'Please provide a reason for cancelling this prescription',
            'Cancel Prescription',
            {
              confirmButtonText: 'Cancel Prescription',
              cancelButtonText: 'Keep Active',
              inputPattern: /.+/,
              inputErrorMessage: 'Reason is required',
              type: 'warning'
            }
        )

        loading.value = true
        await http.put(`/prescriptions/${props.prescription.id}/status`, {
          status: 'cancelled',
          reason: value
        })

        ElMessage.success('Prescription cancelled')
        emit('cancelled')
        visible.value = false
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Failed to cancel prescription:', error)
          ElMessage.error('Failed to cancel prescription')
        }
      } finally {
        loading.value = false
      }
    }

    return {
      loading,
      visible,
      hasSchedule,
      hasWarnings,
      getStatusType,
      getRefillPercentage,
      getRefillColor,
      canRefill,
      formatDate,
      handlePrint,
      handleRefill,
      handleCancel
    }
  }
}
</script>

<style scoped>
.prescription-detail {
  font-size: 14px;
}

.rx-e-prescription {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 25px;
  border-radius: 10px;
  margin-bottom: 20px;
}

.e-rx-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.clinic-info h2 {
  margin: 0 0 5px;
  font-size: 24px;
}

.clinic-info p {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

.rx-qr {
  text-align: center;
}

.qr-placeholder {
  background: white;
  color: #667eea;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 8px;
}

.qr-text {
  font-size: 12px;
  margin-top: 5px;
  font-weight: 600;
}

.verification-code {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 2px;
}

.rx-id-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 15px 0 10px;
}

.rx-date {
  font-size: 13px;
  opacity: 0.95;
}

.section {
  margin: 25px 0;
}

.section h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

.medication-box {
  background-color: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.med-name-big {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 15px;
}

.med-details-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.detail-item {
  display: flex;
  flex-direction: column;
}

.detail-item .label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.detail-item .value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.schedule-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.doctor-signature {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.doctor-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.doctor-sig {
  font-size: 13px;
  color: #67c23a;
}

.refill-progress-box {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.refill-info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
}

.admin-actions {
  background-color: #fef0f0;
  padding: 20px;
  border-radius: 8px;
  margin-top: 30px;
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  flex: 1;
  min-width: 150px;
}

@media print {
  .admin-actions,
  .el-dialog__footer {
    display: none !important;
  }
}
</style>