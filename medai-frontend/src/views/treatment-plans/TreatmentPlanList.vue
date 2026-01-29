<template>
  <div class="treatment-plan-list">
    <el-card>
      <template #header>
        <div class="header">
          <span>Treatment Plans</span>
          <el-button type="primary" @click="createPlan">
            <el-icon><Plus /></el-icon> New Plan
          </el-button>
        </div>
      </template>

      <!-- Filters -->
      <el-row :gutter="15" style="margin-bottom:20px;">
        <el-col :span="6">
          <el-input v-model="search" placeholder="Search plans..." clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-col>
        <el-col :span="5">
          <el-select v-model="filterStatus" placeholder="Status" clearable style="width:100%;">
            <el-option label="Active" value="active" />
            <el-option label="Completed" value="completed" />
            <el-option label="Cancelled" value="cancelled" />
            <el-option label="On Hold" value="on_hold" />
          </el-select>
        </el-col>
        <el-col :span="5">
          <el-select v-model="filterPriority" placeholder="Priority" clearable style="width:100%;">
            <el-option label="Urgent" value="urgent" />
            <el-option label="High" value="high" />
            <el-option label="Medium" value="medium" />
            <el-option label="Low" value="low" />
          </el-select>
        </el-col>
        <el-col :span="5">
          <el-select v-model="filterCategory" placeholder="Category" clearable style="width:100%;">
            <el-option label="Chronic Disease" value="chronic_disease" />
            <el-option label="Post-Operative" value="post_operative" />
            <el-option label="Rehabilitation" value="rehabilitation" />
            <el-option label="Preventive" value="preventive" />
          </el-select>
        </el-col>
        <el-col :span="3">
          <el-button @click="loadPlans" style="width:100%;">Filter</el-button>
        </el-col>
      </el-row>

      <!-- Plans Table -->
      <el-table :data="filteredPlans" v-loading="loading" stripe>
        <el-table-column prop="startDate" label="Start Date" width="110" sortable>
          <template #default="{row}">{{ formatDate(row.startDate) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="Title" min-width="200" />
        <el-table-column prop="patientName" label="Patient" width="140" />
        <el-table-column prop="category" label="Category" width="130">
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
        <el-table-column prop="progressPercentage" label="Progress" width="120">
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
        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="viewPlan(row)">
              <el-icon><View /></el-icon>
            </el-button>
            <el-button v-if="row.status === 'active'" size="small" type="primary" @click="editPlan(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-dropdown v-if="row.status !== 'completed'" trigger="click">
              <el-button size="small">
                <el-icon><More /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status === 'active'" @click="holdPlan(row)">
                    <el-icon><VideoPause /></el-icon> Hold
                  </el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'on_hold'" @click="resumePlan(row)">
                    <el-icon><VideoPlay /></el-icon> Resume
                  </el-dropdown-item>
                  <el-dropdown-item @click="cancelPlan(row)" divided>
                    <el-icon><Close /></el-icon> Cancel
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Detail Dialog -->
    <TreatmentPlanDetail v-model="detailVisible" :plan-id="selectedPlanId" @closed="loadPlans" />
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, View, Edit, More, VideoPause, VideoPlay, Close } from '@element-plus/icons-vue'
import TreatmentPlanDetail from './TreatmentPlanDetail.vue'

export default {
  name: 'TreatmentPlanList',
  components: { Plus, Search, View, Edit, More, VideoPause, VideoPlay, Close, TreatmentPlanDetail },
  props: {
    patientId: { type: Number, default: null }
  },
  setup(props) {
    const router = useRouter()
    const loading = ref(false)
    const plans = ref([])
    const search = ref('')
    const filterStatus = ref('')
    const filterPriority = ref('')
    const filterCategory = ref('')
    const detailVisible = ref(false)
    const selectedPlanId = ref(null)

    const filteredPlans = computed(() => {
      let result = plans.value
      if (search.value) {
        const q = search.value.toLowerCase()
        result = result.filter(p =>
            p.title?.toLowerCase().includes(q) ||
            p.diagnosis?.toLowerCase().includes(q) ||
            p.description?.toLowerCase().includes(q)
        )
      }
      return result
    })

    const loadPlans = async () => {
      loading.value = true
      try {
        const params = {}
        if (props.patientId) params.patientId = props.patientId
        if (filterStatus.value) params.status = filterStatus.value
        if (filterPriority.value) params.priority = filterPriority.value
        if (filterCategory.value) params.category = filterCategory.value

        const res = await http.get('/treatment-plans', { params })
        plans.value = res.data
      } catch (e) {
        ElMessage.error('Failed to load plans')
      } finally {
        loading.value = false
      }
    }

    const createPlan = () => {
      if (props.patientId) {
        router.push({ name: 'NewTreatmentPlan', query: { patientId: props.patientId } })
      } else {
        router.push({ name: 'NewTreatmentPlan' })
      }
    }

    const viewPlan = (plan) => {
      selectedPlanId.value = plan.id
      detailVisible.value = true
    }

    const editPlan = (plan) => {
      router.push({ name: 'EditTreatmentPlan', params: { id: plan.id } })
    }

    const holdPlan = async (plan) => {
      try {
        const { value: reason } = await ElMessageBox.prompt('Reason for holding plan:', 'Hold Plan', {
          confirmButtonText: 'Hold',
          cancelButtonText: 'Cancel'
        })
        await http.post(`/treatment-plans/${plan.id}/hold`, { reason })
        ElMessage.success('Plan put on hold')
        loadPlans()
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('Failed to hold plan')
      }
    }

    const resumePlan = async (plan) => {
      try {
        await ElMessageBox.confirm('Resume this plan?', 'Confirm', { type: 'info' })
        await http.post(`/treatment-plans/${plan.id}/resume`)
        ElMessage.success('Plan resumed')
        loadPlans()
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('Failed to resume plan')
      }
    }

    const cancelPlan = async (plan) => {
      try {
        const { value: reason } = await ElMessageBox.prompt('Reason for cancellation:', 'Cancel Plan', {
          confirmButtonText: 'Cancel Plan',
          cancelButtonText: 'Back',
          inputValidator: (val) => val ? true : 'Reason is required'
        })
        await http.post(`/treatment-plans/${plan.id}/cancel`, { reason })
        ElMessage.success('Plan cancelled')
        loadPlans()
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('Failed to cancel plan')
      }
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

    onMounted(loadPlans)

    return {
      loading, plans, search, filterStatus, filterPriority, filterCategory,
      filteredPlans, detailVisible, selectedPlanId,
      loadPlans, createPlan, viewPlan, editPlan, holdPlan, resumePlan, cancelPlan,
      formatDate, formatCategory, getPriorityType, getStatusType, getProgressColor
    }
  }
}
</script>

<style scoped>
.treatment-plan-list { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; }
</style>