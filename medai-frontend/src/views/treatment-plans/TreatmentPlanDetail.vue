<template>
  <el-dialog
      v-model="dialogVisible"
      width="900px"
      @closed="handleClose"
  >
    <template #header>
      <div class="card-header">
        <span>Treatment Plan Details</span>
        <div class="header-actions">
          <QuickExportButton
              v-if="plan && plan.id"
              :entity-id="plan.id"
              entity-type="treatment-plan"
          />

          <el-dropdown @command="handleExport" style="margin-left: 10px;">
            <el-button type="success">
              <el-icon><Download /></el-icon>
              Export Plan
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="pdf">Export as PDF</el-dropdown-item>
                <el-dropdown-item command="docx">Export as DOCX</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </template>

    <div v-loading="loading" class="plan-detail">
      <div v-if="plan" class="plan-content">
        <div class="plan-header">
          <div class="header-row">
            <div class="info-item">
              <label>Plan ID:</label>
              <span>{{ plan.planUid }}</span>
            </div>
            <div class="info-item">
              <label>Status:</label>
              <el-tag :type="getStatusType(plan.status)">{{ plan.status }}</el-tag>
            </div>
            <div class="info-item">
              <label>Priority:</label>
              <el-tag :type="getPriorityType(plan.priority)">{{ plan.priority }}</el-tag>
            </div>
          </div>
          <div class="header-row">
            <div class="info-item">
              <label>Patient:</label>
              <span>{{ plan.patientName }}</span>
            </div>
            <div class="info-item">
              <label>Doctor:</label>
              <span>{{ plan.doctorName }}</span>
            </div>
            <div class="info-item">
              <label>Category:</label>
              <el-tag>{{ formatCategory(plan.category) }}</el-tag>
            </div>
          </div>
        </div>

        <el-divider />

        <h3>{{ plan.title }}</h3>

        <div v-if="plan.diagnosis" class="detail-section">
          <label>Diagnosis:</label>
          <p>{{ plan.diagnosis }}</p>
        </div>

        <div v-if="plan.goals" class="detail-section">
          <label>Goals:</label>
          <p>{{ plan.goals }}</p>
        </div>

        <div v-if="plan.description" class="detail-section">
          <label>Description:</label>
          <p>{{ plan.description }}</p>
        </div>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <div class="date-box">
              <label>Start Date:</label>
              <span>{{ formatDate(plan.startDate) }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="date-box">
              <label>Expected Completion:</label>
              <span>{{ formatDate(plan.expectedCompletionDate) }}</span>
            </div>
          </el-col>
        </el-row>

        <div class="progress-section">
          <label>Overall Progress:</label>
          <el-progress
              :percentage="plan.progressPercentage"
              :color="getProgressColor(plan.progressPercentage)"
              :stroke-width="20"
          />
        </div>

        <el-divider content-position="left">Treatment Steps</el-divider>

        <div v-if="steps.length === 0">
          <el-empty description="No steps defined" />
        </div>

        <el-timeline v-else>
          <el-timeline-item
              v-for="step in steps"
              :key="step.id"
              :timestamp="formatDate(step.dueDate)"
              placement="top"
              :type="getStepType(step.status)"
              :icon="getStepIcon(step.status)"
          >
            <el-card>
              <div class="step-title">
                <h4>{{ step.stepOrder }}. {{ step.title }}</h4>
                <el-tag :type="getStepStatusType(step.status)" size="small">
                  {{ step.status }}
                </el-tag>
              </div>

              <p v-if="step.description">{{ step.description }}</p>

              <div v-if="step.instructions" class="instructions">
                <label>Instructions:</label>
                <p>{{ step.instructions }}</p>
              </div>

              <div v-if="step.completedAt" class="completion-info">
                <small>Completed: {{ formatDateTime(step.completedAt) }}</small>
              </div>

              <div v-if="step.completionNotes" class="completion-notes">
                <small>Notes: {{ step.completionNotes }}</small>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>

        <div v-if="plan.notes" class="notes-section">
          <el-divider />
          <label>Additional Notes:</label>
          <p>{{ plan.notes }}</p>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">Close</el-button>
      <el-button
          v-if="plan && plan.status === 'active'"
          type="primary"
          @click="editPlan"
      >
        <el-icon><Edit /></el-icon>
        Edit
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { Edit, Check, Close, Clock, Back, Download, ArrowDown } from '@element-plus/icons-vue'
import QuickExportButton from '../../components/QuickExportButton.vue'

export default {
  name: 'TreatmentPlanDetail',
  components: { Edit, Check, Close, Clock, QuickExportButton, Back, Download, ArrowDown },
  props: {
    modelValue: Boolean,
    planId: Number
  },
  emits: ['update:modelValue', 'closed'],
  setup(props, { emit }) {
    const router = useRouter()
    const loading = ref(false)
    const plan = ref(null)
    const steps = ref([])

    const dialogVisible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    const loadPlan = async () => {
      if (!props.planId) return
      loading.value = true
      try {
        const res = await http.get(`/treatment-plans/${props.planId}`)
        plan.value = res.data
        steps.value = res.data.steps || []
      } catch (e) {
        ElMessage.error('Failed to load plan')
        handleClose()
      } finally {
        loading.value = false
      }
    }

    const handleExport = async (format) => {
      try {
        if (!plan.value?.id) {
          ElMessage.warning('No plan loaded')
          return
        }

        ElMessage.info(`Exporting treatment plan as ${format.toUpperCase()}...`)

        const response = await http.post(
            `/treatment-plans/${plan.value.id}/export`,
            null,
            {
              params: { format },
              responseType: 'blob'
            }
        )

        const blob = new Blob([response.data])
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url

        const planTitle = plan.value?.title || 'TreatmentPlan'
        const sanitizedTitle = planTitle.replace(/[^a-z0-9]/gi, '_')
        const timestamp = new Date().toISOString()
            .replace(/[:.]/g, '-')
            .slice(0, 19)
            .replace('T', '_')
        const fileName = `TreatmentPlan_${sanitizedTitle}_${timestamp}.${format}`

        link.setAttribute('download', fileName)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        ElMessage.success(`Treatment plan exported as ${format.toUpperCase()}`)

      } catch (error) {
        console.error('Export error:', error)
        ElMessage.error(error.response?.data?.message || 'Export failed. Please try again.')
      }
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'Not set'
      return new Date(dateStr).toLocaleDateString()
    }

    const formatDateTime = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleString()
    }

    const formatCategory = (cat) => {
      const map = {
        chronic_disease: 'Chronic Disease',
        post_operative: 'Post-Operative',
        rehabilitation: 'Rehabilitation',
        preventive: 'Preventive Care'
      }
      return map[cat] || cat
    }

    const getStatusType = (status) => {
      const types = {
        active: 'success',
        completed: 'info',
        cancelled: 'danger',
        on_hold: 'warning'
      }
      return types[status] || 'info'
    }

    const getPriorityType = (priority) => {
      const types = { urgent: 'danger', high: 'warning', medium: '', low: 'info' }
      return types[priority] || ''
    }

    const getStepType = (status) => {
      const types = {
        completed: 'success',
        in_progress: 'primary',
        pending: 'info',
        skipped: 'warning',
        delayed: 'danger'
      }
      return types[status] || 'info'
    }

    const getStepStatusType = (status) => {
      const types = {
        completed: 'success',
        in_progress: 'primary',
        pending: 'info',
        skipped: 'warning',
        delayed: 'danger'
      }
      return types[status] || 'info'
    }

    const getStepIcon = (status) => {
      if (status === 'completed') return Check
      if (status === 'skipped') return Close
      return Clock
    }

    const getProgressColor = (progress) => {
      if (progress >= 75) return '#67c23a'
      if (progress >= 50) return '#409eff'
      if (progress >= 25) return '#e6a23c'
      return '#f56c6c'
    }

    const editPlan = () => {
      router.push({ name: 'EditTreatmentPlan', params: { id: plan.value.id } })
      handleClose()
    }

    const handleClose = () => {
      dialogVisible.value = false
      plan.value = null
      steps.value = []
      emit('closed')
    }

    watch(
        () => props.planId,
        () => {
          if (props.planId && props.modelValue) loadPlan()
        }
    )

    watch(dialogVisible, (val) => {
      if (val && props.planId) loadPlan()
    })

    return {
      loading,
      plan,
      steps,
      dialogVisible,
      formatDate,
      formatDateTime,
      formatCategory,
      getStatusType,
      getPriorityType,
      getStepType,
      getStepStatusType,
      getStepIcon,
      getProgressColor,
      editPlan,
      handleClose,
      handleExport
    }
  }
}
</script>

<style scoped>
.plan-detail {
  padding: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.plan-header {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.header-row {
  display: flex;
  gap: 30px;
  margin-bottom: 10px;
}

.header-row:last-child {
  margin-bottom: 0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item label {
  font-weight: 600;
  color: #606266;
}

.detail-section {
  margin: 15px 0;
}

.detail-section label {
  font-weight: 600;
  display: block;
  margin-bottom: 5px;
}

.date-box {
  padding: 10px;
  background: #f0f9ff;
  border-radius: 4px;
}

.date-box label {
  font-weight: 600;
  display: block;
  margin-bottom: 5px;
}

.progress-section {
  margin: 20px 0;
}

.progress-section label {
  font-weight: 600;
  display: block;
  margin-bottom: 10px;
}

.step-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.step-title h4 {
  margin: 0;
}

.instructions {
  margin-top: 10px;
  padding: 10px;
  background: #f0f9ff;
  border-radius: 4px;
}

.instructions label {
  font-weight: 600;
  display: block;
  margin-bottom: 5px;
}

.completion-info,
.completion-notes {
  margin-top: 10px;
  color: #909399;
}

.notes-section {
  margin-top: 20px;
}

.notes-section label {
  font-weight: 600;
  display: block;
  margin-bottom: 5px;
}
</style>