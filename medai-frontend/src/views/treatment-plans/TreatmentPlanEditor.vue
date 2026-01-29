<template>
  <div class="treatment-plan-editor">
    <el-card v-loading="loading">
      <template #header>
        <div class="header">
          <span>{{ isEdit ? 'Edit' : 'New' }} Treatment Plan</span>
          <div class="header-actions">
            <el-tag v-if="form.planUid" size="small">{{ form.planUid }}</el-tag>
            <el-tag :type="getStatusType(form.status)">{{ form.status }}</el-tag>
          </div>
        </div>
      </template>

      <el-form :model="form" ref="formRef" :rules="rules" label-width="160px">
        <!-- Patient Selection -->
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="Patient" prop="patientId">
              <el-select
                  v-model="form.patientId"
                  filterable
                  remote
                  reserve-keyword
                  placeholder="Search patient by name or ID..."
                  :remote-method="searchPatients"
                  :loading="searchingPatients"
                  style="width:100%;"
                  :disabled="isEdit"
              >
                <el-option
                    v-for="p in patientOptions"
                    :key="p.id"
                    :label="`${p.name} (${p.patientId})`"
                    :value="p.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Template Selection -->
        <el-row :gutter="20" v-if="!isEdit">
          <el-col :span="24">
            <el-form-item label="Use Template (Optional)">
              <el-select v-model="selectedTemplate" placeholder="Choose a template" style="width:100%;" @change="applyTemplate" clearable>
                <el-option
                    v-for="t in templates"
                    :key="t.id"
                    :label="t.name"
                    :value="t.id"
                >
                  <span>{{ t.name }}</span>
                  <el-tag v-if="t.isSystem" size="small" type="info" style="margin-left:8px;">System</el-tag>
                  <el-tag size="small" style="margin-left:8px;">{{ t.category }}</el-tag>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Plan Details -->
        <el-divider content-position="left">Plan Details</el-divider>

        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="Title" prop="title">
              <el-input v-model="form.title" placeholder="Treatment plan title" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Priority" prop="priority">
              <el-select v-model="form.priority" style="width:100%;">
                <el-option label="Low" value="low" />
                <el-option label="Medium" value="medium" />
                <el-option label="High" value="high" />
                <el-option label="Urgent" value="urgent" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Category">
          <el-select v-model="form.category" placeholder="Select category" style="width:100%;">
            <el-option label="Chronic Disease Management" value="chronic_disease" />
            <el-option label="Post-Operative Care" value="post_operative" />
            <el-option label="Rehabilitation" value="rehabilitation" />
            <el-option label="Preventive Care" value="preventive" />
          </el-select>
        </el-form-item>

        <el-form-item label="Diagnosis">
          <el-input v-model="form.diagnosis" placeholder="Primary diagnosis" />
        </el-form-item>

        <el-form-item label="Goals">
          <el-input v-model="form.goals" type="textarea" :rows="3" placeholder="Treatment goals and objectives" />
        </el-form-item>

        <el-form-item label="Description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="Plan description" />
        </el-form-item>

        <!-- Dates -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Start Date" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Expected Completion">
              <el-date-picker v-model="form.expectedCompletionDate" type="date" style="width:100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Treatment Steps -->
        <el-divider content-position="left">
          Treatment Steps
          <el-button type="primary" size="small" @click="addStep" style="margin-left:10px;">
            <el-icon><Plus /></el-icon> Add Step
          </el-button>
        </el-divider>

        <div v-if="form.steps.length === 0" class="empty-steps">
          <el-empty description="No steps added yet. Click 'Add Step' to begin." />
        </div>

        <draggable v-model="form.steps" item-key="order" handle=".drag-handle" @end="reorderSteps">
          <template #item="{element, index}">
            <el-card class="step-card" shadow="hover">
              <div class="step-header">
                <el-icon class="drag-handle" style="cursor:move; margin-right:10px;"><Rank /></el-icon>
                <span class="step-number">Step {{ index + 1 }}</span>
                <div class="step-actions">
                  <el-button size="small" type="danger" @click="removeStep(index)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
              <el-row :gutter="15" style="margin-top:15px;">
                <el-col :span="16">
                  <el-input v-model="element.title" placeholder="Step title" />
                </el-col>
                <el-col :span="8">
                  <el-input v-model.number="element.durationDays" placeholder="Duration (days)" type="number">
                    <template #append>days</template>
                  </el-input>
                </el-col>
              </el-row>
              <el-input v-model="element.description" type="textarea" :rows="2" placeholder="Step description" style="margin-top:10px;" />
              <el-input v-model="element.instructions" type="textarea" :rows="2" placeholder="Instructions" style="margin-top:10px;" />
            </el-card>
          </template>
        </draggable>

        <!-- Notes -->
        <el-form-item label="Notes" style="margin-top:20px;">
          <el-input v-model="form.notes" type="textarea" :rows="3" placeholder="Additional notes" />
        </el-form-item>

        <!-- Actions -->
        <el-form-item>
          <el-button @click="$router.back()">Cancel</el-button>
          <el-button type="primary" @click="save" :loading="saving">
            {{ isEdit ? 'Update' : 'Create' }} Plan
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Rank } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'

export default {
  name: 'TreatmentPlanEditor',
  components: { Plus, Delete, Rank, draggable },
  props: {
    patientId: { type: Number, default: null },
    planId: { type: Number, default: null }
  },
  setup(props) {
    const router = useRouter()
    const loading = ref(false)
    const saving = ref(false)
    const formRef = ref(null)
    const searchingPatients = ref(false)
    const patientOptions = ref([])
    const templates = ref([])
    const selectedTemplate = ref(null)

    const isEdit = computed(() => props.planId !== null)

    const form = reactive({
      patientId: props.patientId || null,
      title: '',
      diagnosis: '',
      goals: '',
      description: '',
      priority: 'medium',
      category: '',
      startDate: new Date(),
      expectedCompletionDate: null,
      notes: '',
      steps: [],
      planUid: '',
      status: 'active'
    })

    const rules = {
      patientId: [{ required: true, message: 'Patient is required', trigger: 'change' }],
      title: [{ required: true, message: 'Title is required', trigger: 'blur' }],
      startDate: [{ required: true, message: 'Start date is required', trigger: 'change' }]
    }

    const searchPatients = async (query) => {
      if (!query) return
      searchingPatients.value = true
      try {
        const res = await http.get('/patient/doctor/patients', { params: { search: query } })
        patientOptions.value = res.data
      } catch (e) {
        console.error(e)
      } finally {
        searchingPatients.value = false
      }
    }

    const loadTemplates = async () => {
      try {
        const res = await http.get('/treatment-plans/templates')
        templates.value = res.data
      } catch (e) {
        console.error(e)
      }
    }

    const loadPlan = async () => {
      if (!props.planId) return
      loading.value = true
      try {
        const res = await http.get(`/treatment-plans/${props.planId}`)
        Object.assign(form, res.data)
        if (res.data.startDate) form.startDate = new Date(res.data.startDate)
        if (res.data.expectedCompletionDate) form.expectedCompletionDate = new Date(res.data.expectedCompletionDate)
        if (res.data.steps) form.steps = res.data.steps
      } catch (e) {
        ElMessage.error('Failed to load plan')
        router.back()
      } finally {
        loading.value = false
      }
    }

    const applyTemplate = async () => {
      if (!selectedTemplate.value) return
      try {
        const res = await http.post(`/treatment-plans/templates/${selectedTemplate.value}/apply`)
        form.title = res.data.title || form.title
        form.category = res.data.category || form.category
        form.description = res.data.description || form.description
        if (res.data.startDate) form.startDate = new Date(res.data.startDate)
        if (res.data.expectedCompletionDate) form.expectedCompletionDate = new Date(res.data.expectedCompletionDate)
        if (res.data.steps) {
          form.steps = res.data.steps.map(s => ({
            order: s.order,
            title: s.title,
            description: s.description,
            instructions: s.instructions,
            durationDays: s.durationDays
          }))
        }
        ElMessage.success('Template applied')
      } catch (e) {
        ElMessage.error('Failed to apply template')
      }
    }

    const addStep = () => {
      form.steps.push({
        order: form.steps.length + 1,
        title: '',
        description: '',
        instructions: '',
        durationDays: null
      })
    }

    const removeStep = (index) => {
      form.steps.splice(index, 1)
      reorderSteps()
    }

    const reorderSteps = () => {
      form.steps.forEach((step, index) => {
        step.order = index + 1
      })
    }

    const save = async () => {
      if (!formRef.value) return
      try { await formRef.value.validate() } catch (e) { return }

      saving.value = true
      try {
        const payload = {
          ...form,
          startDate: form.startDate?.toISOString().split('T')[0],
          expectedCompletionDate: form.expectedCompletionDate?.toISOString().split('T')[0]
        }

        if (isEdit.value) {
          await http.put(`/treatment-plans/${props.planId}`, payload)
          ElMessage.success('Plan updated')
        } else {
          await http.post('/treatment-plans', payload)
          ElMessage.success('Plan created')
        }
        router.back()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || 'Failed to save')
      } finally {
        saving.value = false
      }
    }

    const getStatusType = (status) => {
      const types = { active: 'success', completed: 'info', cancelled: 'danger', on_hold: 'warning' }
      return types[status] || 'info'
    }

    onMounted(() => {
      loadTemplates()
      if (props.planId) loadPlan()
      if (props.patientId) searchPatients('')
    })

    return {
      loading, saving, formRef, searchingPatients, patientOptions, templates,
      selectedTemplate, isEdit, form, rules,
      searchPatients, applyTemplate, addStep, removeStep, reorderSteps, save, getStatusType
    }
  }
}
</script>

<style scoped>
.treatment-plan-editor { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 10px; }
.step-card { margin-bottom: 15px; }
.step-header { display: flex; align-items: center; justify-content: space-between; }
.step-number { font-weight: 600; flex: 1; }
.empty-steps { padding: 40px 0; }
.drag-handle { cursor: move; }
</style>