<template>
  <div class="treatment-plans-tab">
    <div class="tab-header">
      <div class="stats">
        <el-statistic title="Active Plans" :value="stats.activePlans" />
        <el-statistic title="Completed" :value="stats.completedPlans" />
        <el-statistic title="Avg Progress" :value="stats.averageProgress" suffix="%" />
      </div>
      <el-button type="primary" @click="createPlan">
        <el-icon><Plus /></el-icon> New Plan
      </el-button>
    </div>

    <!-- Plans List -->
    <el-table :data="plans" v-loading="loading" stripe style="margin-top:20px;">
      <el-table-column prop="startDate" label="Start Date" width="110" sortable>
        <template #default="{row}">{{ formatDate(row.startDate) }}</template>
      </el-table-column>
      <el-table-column prop="title" label="Title" min-width="180" />
      <el-table-column prop="category" label="Category" width="120">
        <template #default="{row}">
          <el-tag size="small">{{ formatCategory(row.category) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="Priority" width="90">
        <template #default="{row}">
          <el-tag :type="getPriorityType(row.priority)" size="small">
            {{ row.priority }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="progressPercentage" label="Progress" width="140">
        <template #default="{row}">
          <el-progress :percentage="row.progressPercentage" :color="getProgressColor(row.progressPercentage)" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="Status" width="100">
        <template #default="{row}">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="doctorName" label="Doctor" width="130" />
      <el-table-column label="Actions" width="150" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="viewPlan(row)">
            <el-icon><View /></el-icon>
          </el-button>
          <el-button v-if="row.status === 'active'" size="small" type="primary" @click="trackProgress(row)">
            <el-icon><TrendCharts /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && plans.length === 0" description="No treatment plans yet" />

    <!-- Detail Dialog -->
    <TreatmentPlanDetail v-model="detailVisible" :plan-id="selectedPlanId" @closed="loadPlans" />

    <!-- Progress Tracker Dialog -->
    <el-dialog v-model="trackerVisible" title="Progress Tracker" width="800px">
      <TreatmentStepTracker v-if="selectedPlanId" :plan-id="selectedPlanId" @step-updated="loadPlans" />
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { Plus, View, TrendCharts } from '@element-plus/icons-vue'
import TreatmentPlanDetail from './TreatmentPlanDetail.vue'
import TreatmentStepTracker from './TreatmentStepTracker.vue'


export default {
  name: 'TreatmentPlansTab',
  components: { Plus, View, TrendCharts, TreatmentPlanDetail, TreatmentStepTracker },
  props: {
    patientId: { type: Number, required: true }
  },
  setup(props) {
    const router = useRouter()
    const loading = ref(false)
    const plans = ref([])
    const stats = ref({ activePlans: 0, completedPlans: 0, averageProgress: 0 })
    const detailVisible = ref(false)
    const trackerVisible = ref(false)
    const selectedPlanId = ref(null)

    const loadPlans = async () => {
      if (!props.patientId || props.patientId === 'null' || props.patientId === 'undefined') {
        console.warn('Invalid patient ID, skipping treatment plans load')
        plans.value = []
        stats.value = { activePlans: 0, completedPlans: 0, averageProgress: 0 }
        return
      }
      loading.value = true
      try {
        const [plansRes, statsRes] = await Promise.all([
          http.get('/treatment-plans/patient/' + props.patientId),
          http.get('/treatment-plans/patient/' + props.patientId + '/stats')
        ])
        plans.value = plansRes.data
        stats.value = statsRes.data
      } catch (e) {
        ElMessage.error('Failed to load plans')
      } finally {
        loading.value = false
      }
    }

    // FIXED:
    const loadStats = async () => {
      // Validate patientId before making API call
      if (!patientId || patientId === 'null' || patientId === 'undefined') {
        console.warn('Invalid patient ID, skipping stats load')
        return
      }

      try {
        const response = await http.get(`/treatment-plans/patient/${patientId}/stats`)
        // ... handle response
      } catch (error) {
        console.error('Failed to load stats:', error)
        // Don't show error to user if it's just a missing patient ID
        if (error.response?.status !== 400) {
          ElMessage.error('Failed to load treatment plan statistics')
        }
      }
    }

    // ADD THIS VALIDATION in createPlan():
    const createPlan = () => {
      if (!props.patientId || props.patientId === 'null' || props.patientId === 'undefined') {
        ElMessage.warning('Please wait for patient data to load')
        return
      }
      router.push({ name: 'NewTreatmentPlan', query: { patientId: props.patientId } })
    }


    const viewPlan = (plan) => {
      selectedPlanId.value = plan.id
      detailVisible.value = true
    }

    const trackProgress = (plan) => {
      selectedPlanId.value = plan.id
      trackerVisible.value = true
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleDateString()
    }

    const formatCategory = (cat) => {
      const map = {
        chronic_disease: 'Chronic',
        post_operative: 'Post-Op',
        rehabilitation: 'Rehab',
        preventive: 'Preventive'
      }
      return map[cat] || cat
    }

    const getPriorityType = (priority) => {
      const types = { urgent: 'danger', high: 'warning', medium: '', low: 'info' }
      return types[priority] || ''
    }

    const getStatusType = (status) => {
      const types = { active: 'success', completed: 'info', cancelled: 'danger', on_hold: 'warning' }
      return types[status] || 'info'
    }

    const getProgressColor = (progress) => {
      if (progress >= 75) return '#67c23a'
      if (progress >= 50) return '#409eff'
      if (progress >= 25) return '#e6a23c'
      return '#f56c6c'
    }

    onMounted(() => {
      if (props.patientId && props.patientId !== 'null' && props.patientId !== 'undefined') {
        loadPlans()
      }})

    watch(() => props.patientId, (newId, oldId) => {
      if (newId && newId !== 'null' && newId !== 'undefined' && newId !== oldId) {
        loadPlans()
      }
    }, { immediate: false })

    return {
      loading, plans, stats, detailVisible, trackerVisible, selectedPlanId,
      loadPlans, createPlan, viewPlan, trackProgress,
      formatDate, formatCategory, getPriorityType, getStatusType, getProgressColor
    }
  }
}
</script>

<style scoped>
.treatment-plans-tab { padding: 20px; }
.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.stats { display: flex; gap: 40px; }
</style>