<template>
  <el-card class="step-tracker">
    <template #header>
      <div class="header">
        <span>Step Progress Tracker</span>
        <el-tag :type="getStatusType(plan.status)">{{ plan.status }}</el-tag>
      </div>
    </template>

    <div v-loading="loading">
      <div class="progress-overview">
        <el-statistic title="Overall Progress" :value="plan.progressPercentage" suffix="%" />
        <el-progress :percentage="plan.progressPercentage" :color="getProgressColor(plan.progressPercentage)" :stroke-width="15" />
      </div>

      <el-divider />

      <div v-if="steps.length === 0">
        <el-empty description="No steps defined" />
      </div>

      <div v-else class="steps-list">
        <el-card
            v-for="step in steps"
            :key="step.id"
            class="step-item"
            :class="{'step-completed': step.status === 'completed', 'step-active': step.status === 'in_progress'}"
            shadow="hover"
        >
          <div class="step-content">
            <div class="step-info">
              <div class="step-title">
                <el-icon v-if="step.status === 'completed'" color="#67c23a"><CircleCheck /></el-icon>
                <el-icon v-else-if="step.status === 'skipped'" color="#e6a23c"><Close /></el-icon>
                <el-icon v-else color="#909399"><Clock /></el-icon>
                <h4>Step {{ step.stepOrder }}: {{ step.title }}</h4>
              </div>
              <el-tag :type="getStepStatusType(step.status)" size="small">{{ step.status }}</el-tag>
            </div>

            <p v-if="step.description" class="description">{{ step.description }}</p>

            <div v-if="step.instructions" class="instructions">
              <label>Instructions:</label>
              <p>{{ step.instructions }}</p>
            </div>

            <div class="step-meta">
              <div v-if="step.dueDate" class="meta-item">
                <el-icon><Calendar /></el-icon>
                <span>Due: {{ formatDate(step.dueDate) }}</span>
              </div>
              <div v-if="step.durationDays" class="meta-item">
                <el-icon><Timer /></el-icon>
                <span>{{ step.durationDays }} days</span>
              </div>
              <div v-if="step.assignedToName" class="meta-item">
                <el-icon><User /></el-icon>
                <span>{{ step.assignedToName }}</span>
              </div>
            </div>

            <div v-if="step.completedAt" class="completion-info">
              <el-icon><CircleCheck /></el-icon>
              <span>Completed on {{ formatDateTime(step.completedAt) }}</span>
            </div>

            <div v-if="step.completionNotes" class="completion-notes">
              <label>Completion Notes:</label>
              <p>{{ step.completionNotes }}</p>
            </div>

            <div v-if="canManageStep(step)" class="step-actions">
              <el-button
                  v-if="step.status === 'pending'"
                  size="small"
                  @click="startStep(step)"
              >
                <el-icon><VideoPlay /></el-icon> Start
              </el-button>
              <el-button
                  v-if="step.status === 'in_progress'"
                  type="success"
                  size="small"
                  @click="completeStep(step)"
              >
                <el-icon><CircleCheck /></el-icon> Complete
              </el-button>
              <el-button
                  v-if="['pending', 'in_progress'].includes(step.status)"
                  size="small"
                  @click="skipStep(step)"
              >
                <el-icon><Close /></el-icon> Skip
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </el-card>
</template>

<script>
import { ref, onMounted } from 'vue'
import http from '../../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Close, Clock, Calendar, Timer, User, VideoPlay } from '@element-plus/icons-vue'

export default {
  name: 'TreatmentStepTracker',
  components: { CircleCheck, Close, Clock, Calendar, Timer, User, VideoPlay },
  props: {
    planId: { type: Number, required: true }
  },
  emits: ['step-updated'],
  setup(props, { emit }) {
    const loading = ref(false)
    const plan = ref({ status: 'active', progressPercentage: 0 })
    const steps = ref([])

    const loadSteps = async () => {
      loading.value = true
      try {
        const [planRes, stepsRes] = await Promise.all([
          http.get(`/treatment-plans/${props.planId}`),
          http.get(`/treatment-plans/${props.planId}/steps`)
        ])
        plan.value = planRes.data
        steps.value = stepsRes.data
      } catch (e) {
        ElMessage.error('Failed to load steps')
      } finally {
        loading.value = false
      }
    }

    const startStep = async (step) => {
      try {
        await http.put(`/treatment-plans/steps/${step.id}`, { status: 'in_progress' })
        ElMessage.success('Step started')
        loadSteps()
        emit('step-updated')
      } catch (e) {
        ElMessage.error('Failed to start step')
      }
    }

    const completeStep = async (step) => {
      try {
        const { value: notes } = await ElMessageBox.prompt(
            'Add completion notes (optional):',
            'Complete Step',
            {
              confirmButtonText: 'Complete',
              cancelButtonText: 'Cancel',
              inputType: 'textarea'
            }
        )
        await http.post(`/treatment-plans/steps/${step.id}/complete`, { completionNotes: notes || '' })
        ElMessage.success('Step completed')
        loadSteps()
        emit('step-updated')
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('Failed to complete step')
      }
    }

    const skipStep = async (step) => {
      try {
        const { value: reason } = await ElMessageBox.prompt(
            'Reason for skipping:',
            'Skip Step',
            {
              confirmButtonText: 'Skip',
              cancelButtonText: 'Cancel',
              inputValidator: (val) => val ? true : 'Reason is required'
            }
        )
        await http.post(`/treatment-plans/steps/${step.id}/skip`, { reason })
        ElMessage.success('Step skipped')
        loadSteps()
        emit('step-updated')
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('Failed to skip step')
      }
    }

    const canManageStep = (step) => {
      return plan.value.status === 'active' && ['pending', 'in_progress'].includes(step.status)
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'Not set'
      return new Date(dateStr).toLocaleDateString()
    }

    const formatDateTime = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleString()
    }

    const getStatusType = (status) => {
      const types = { active: 'success', completed: 'info', cancelled: 'danger', on_hold: 'warning' }
      return types[status] || 'info'
    }

    const getStepStatusType = (status) => {
      const types = { completed: 'success', in_progress: 'primary', pending: 'info', skipped: 'warning', delayed: 'danger' }
      return types[status] || 'info'
    }

    const getProgressColor = (progress) => {
      if (progress >= 75) return '#67c23a'
      if (progress >= 50) return '#409eff'
      if (progress >= 25) return '#e6a23c'
      return '#f56c6c'
    }

    onMounted(loadSteps)

    return {
      loading, plan, steps,
      startStep, completeStep, skipStep, canManageStep,
      formatDate, formatDateTime, getStatusType, getStepStatusType, getProgressColor
    }
  }
}
</script>

<style scoped>
.step-tracker { margin: 20px 0; }
.header { display: flex; justify-content: space-between; align-items: center; }
.progress-overview { text-align: center; padding: 20px; }
.steps-list { display: flex; flex-direction: column; gap: 15px; }
.step-item { border-left: 4px solid #dcdfe6; }
.step-item.step-completed { border-left-color: #67c23a; background: #f0f9ff; }
.step-item.step-active { border-left-color: #409eff; }
.step-content { padding: 10px; }
.step-info { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.step-title { display: flex; align-items: center; gap: 8px; }
.step-title h4 { margin: 0; }
.description { color: #606266; margin: 10px 0; }
.instructions { margin: 10px 0; padding: 10px; background: #f0f9ff; border-radius: 4px; }
.instructions label { font-weight: 600; display: block; margin-bottom: 5px; }
.step-meta { display: flex; gap: 20px; margin: 10px 0; flex-wrap: wrap; }
.meta-item { display: flex; align-items: center; gap: 5px; font-size: 13px; color: #909399; }
.completion-info { margin: 10px 0; padding: 10px; background: #f0f9ff; border-radius: 4px; display: flex; align-items: center; gap: 8px; color: #67c23a; }
.completion-notes { margin: 10px 0; padding: 10px; background: #fff9e6; border-radius: 4px; }
.completion-notes label { font-weight: 600; display: block; margin-bottom: 5px; }
.step-actions { margin-top: 15px; display: flex; gap: 10px; }
</style>