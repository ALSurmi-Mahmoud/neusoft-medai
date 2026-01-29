<template>
  <div class="clinical-note-editor">
    <el-card v-loading="loading">
      <template #header>
        <div class="header">
          <span>{{ noteId ? 'Edit' : 'New' }} Clinical Note</span>
          <div class="header-actions">
            <el-tag v-if="autoSaveStatus" type="success" size="small">{{ autoSaveStatus }}</el-tag>
            <el-tag :type="form.finalized ? 'success' : 'warning'">
              {{ form.finalized ? 'Finalized' : 'Draft' }}
            </el-tag>
          </div>
        </div>
      </template>

      <el-form :model="form" ref="formRef" :rules="rules" label-width="130px">
        <!-- ✅ PATIENT SELECTOR -->
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="Patient" prop="patientId">
              <el-select
                  v-model="form.patientId"
                  filterable
                  remote
                  reserve-keyword
                  placeholder="Type to search patient by name or ID..."
                  :remote-method="searchPatients"
                  :loading="searchingPatients"
                  style="width:100%;"
                  :disabled="form.finalized || isEditMode"
                  @change="onPatientSelected"
              >
                <el-option
                    v-for="p in patientOptions"
                    :key="p.id"
                    :label="`${p.name} (${p.patientId})`"
                    :value="p.id"
                >
                  <div style="display:flex; justify-content:space-between; align-items: center;">
                    <span style="font-weight: 500;">{{ p.name }}</span>
                    <el-tag size="small" type="info">{{ p.patientId }}</el-tag>
                  </div>
                </el-option>
              </el-select>
              <small style="color: #909399; display: block; margin-top: 5px;">
                <span v-if="!form.patientId">Search by typing patient name or ID</span>
                <span v-else-if="isEditMode">Patient cannot be changed when editing</span>
              </small>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ✅ SELECTED PATIENT INFO DISPLAY -->
        <el-alert
            v-if="form.patientId && selectedPatient"
            type="info"
            :closable="false"
            style="margin-bottom: 20px;"
        >
          <template #title>
            📋 Patient: {{ selectedPatient.name }} (ID: {{ selectedPatient.patientId }})
          </template>
          <div style="font-size: 13px; margin-top: 5px;">
            <span v-if="selectedPatient.age">Age: {{ selectedPatient.age }} years</span>
            <span v-if="selectedPatient.sex"> • Gender: {{ getGenderLabel(selectedPatient.sex) }}</span>
            <span v-if="selectedPatient.email"> • Email: {{ selectedPatient.email }}</span>
          </div>
        </el-alert>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Note Type" prop="noteType">
              <el-select v-model="form.noteType" placeholder="Select type" style="width:100%;" :disabled="form.finalized" @change="loadTemplates">
                <el-option label="SOAP Note" value="soap" />
                <el-option label="Progress Note" value="progress" />
                <el-option label="Consultation" value="consultation" />
                <el-option label="Procedure" value="procedure" />
                <el-option label="Discharge Summary" value="discharge" />
                <el-option label="Follow-up" value="followup" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Template (Optional)">
              <el-select v-model="templateId" placeholder="Use template" style="width:100%;" @change="applyTemplate" :disabled="form.finalized" clearable>
                <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id">
                  <span>{{ t.name }}</span>
                  <el-tag v-if="t.isSystem" size="small" type="info" style="margin-left:8px;">System</el-tag>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="18">
            <el-form-item label="Title" prop="title">
              <el-input v-model="form.title" placeholder="Note title or chief complaint" :disabled="form.finalized" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Date">
              <el-date-picker v-model="form.noteDate" type="date" style="width:100%;" :disabled="form.finalized" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- SOAP Sections -->
        <div v-if="form.noteType === 'soap'" class="soap-sections">
          <el-card shadow="never" class="vitals-card">
            <template #header><span>Vital Signs (Optional)</span></template>
            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="BP" label-width="60px">
                  <el-input v-model="vitals.bp" placeholder="120/80" :disabled="form.finalized">
                    <template #append>mmHg</template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="HR" label-width="60px">
                  <el-input v-model="vitals.hr" placeholder="72" :disabled="form.finalized">
                    <template #append>bpm</template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="Temp" label-width="60px">
                  <el-input v-model="vitals.temp" placeholder="98.6" :disabled="form.finalized">
                    <template #append>°F</template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="RR" label-width="60px">
                  <el-input v-model="vitals.rr" placeholder="16" :disabled="form.finalized">
                    <template #append>/min</template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="SpO2" label-width="60px">
                  <el-input v-model="vitals.spo2" placeholder="98" :disabled="form.finalized">
                    <template #append>%</template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>
          </el-card>

          <el-form-item label="S - Subjective">
            <small class="hint">Chief complaint, symptoms, patient's description</small>
            <el-input v-model="form.subjective" type="textarea" :rows="6" :disabled="form.finalized" />
          </el-form-item>
          <el-form-item label="O - Objective">
            <small class="hint">Physical exam findings, vitals, lab results</small>
            <el-input v-model="form.objective" type="textarea" :rows="6" :disabled="form.finalized" />
          </el-form-item>
          <el-form-item label="A - Assessment">
            <small class="hint">Diagnosis, impressions, clinical reasoning</small>
            <el-input v-model="form.assessment" type="textarea" :rows="5" :disabled="form.finalized" />
          </el-form-item>
          <el-form-item label="P - Plan">
            <small class="hint">Treatment plan, prescriptions, follow-up</small>
            <el-input v-model="form.plan" type="textarea" :rows="6" :disabled="form.finalized" />
          </el-form-item>
        </div>

        <!-- General Content -->
        <div v-else>
          <el-form-item label="Content">
            <el-input v-model="form.content" type="textarea" :rows="15" :disabled="form.finalized" />
          </el-form-item>
        </div>

        <!-- Actions -->
        <el-form-item>
          <el-button @click="$router.back()">Cancel</el-button>
          <el-button v-if="!form.finalized" type="primary" @click="save" :loading="saving">
            Save Draft
          </el-button>
          <el-button v-if="!form.finalized" type="success" @click="finalize" :loading="finalizing">
            <el-icon><Lock /></el-icon> Finalize
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import http from '../../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'

export default {
  name: 'ClinicalNoteEditor',
  components: { Lock },
  props: {
    patientId: { type: Number, default: null },
    noteId: { type: Number, default: null },
    appointmentId: { type: Number, default: null }
  },
  setup(props) {
    const router = useRouter()
    const route = useRoute()
    const loading = ref(false)
    const saving = ref(false)
    const finalizing = ref(false)
    const formRef = ref(null)
    const templateId = ref(null)
    const templates = ref([])
    const autoSaveStatus = ref('')
    const autoSaveTimer = ref(null)

    // Patient search state
    const searchingPatients = ref(false)
    const patientOptions = ref([])
    const selectedPatient = ref(null)

    const isEditMode = computed(() => !!props.noteId)

    const form = reactive({
      id: null,
      patientId: props.patientId || null,
      appointmentId: props.appointmentId,
      noteType: 'soap',
      title: '',
      noteDate: new Date(),
      subjective: '',
      objective: '',
      assessment: '',
      plan: '',
      content: '',
      finalized: false
    })

    const vitals = reactive({
      bp: '', hr: '', temp: '', rr: '', spo2: ''
    })

    const rules = {
      noteType: [{ required: true, message: 'Note type is required', trigger: 'change' }],
      title: [{ required: true, message: 'Title is required', trigger: 'blur' }],
      patientId: [{ required: true, message: 'Please select a patient', trigger: 'change' }]
    }

    // Helper function for gender labels
    const getGenderLabel = (sex) => {
      const labels = { 'M': 'Male', 'F': 'Female', 'O': 'Other' }
      return labels[sex] || sex
    }

    // ✅ FIXED: Search patients using correct endpoint
    const searchPatients = async (query) => {
      if (!query || query.length < 2) {
        return
      }

      searchingPatients.value = true
      try {
        const res = await http.get('/patient/doctor/my-patients', {
          params: { search: query, page: 0, size: 20 }
        })
        patientOptions.value = res.data.content || []
      } catch (e) {
        console.error('Failed to search patients:', e)
        patientOptions.value = []
      } finally {
        searchingPatients.value = false
      }
    }

    // ✅ NEW: When patient is selected from dropdown, save their info
    const onPatientSelected = (patientId) => {
      const patient = patientOptions.value.find(p => p.id === patientId)
      if (patient) {
        selectedPatient.value = patient
      }
    }

    // Load initial patient options
    const loadInitialPatients = async () => {
      try {
        const res = await http.get('/patient/doctor/my-patients', {
          params: { page: 0, size: 10 }
        })
        patientOptions.value = res.data.content || []

        // If patientId prop is provided, find and select that patient
        if (props.patientId) {
          const patient = patientOptions.value.find(p => p.id === props.patientId)
          if (patient) {
            selectedPatient.value = patient
          }
        }
      } catch (e) {
        console.error('Failed to load initial patients:', e)
      }
    }

    const loadTemplates = async () => {
      try {
        const res = await http.get('/clinical-notes/templates', {
          params: { noteType: form.noteType }
        })
        templates.value = res.data || []
      } catch (e) {
        console.error('Failed to load templates:', e)
      }
    }

    const loadNote = async () => {
      if (!props.noteId) return
      loading.value = true
      try {
        const res = await http.get(`/clinical-notes/${props.noteId}`)
        const data = res.data

        // Update form
        form.id = data.id
        form.patientId = data.patientId
        form.appointmentId = data.appointmentId
        form.noteType = data.noteType
        form.title = data.title
        form.subjective = data.subjective || ''
        form.objective = data.objective || ''
        form.assessment = data.assessment || ''
        form.plan = data.plan || ''
        form.content = data.content || ''
        form.finalized = data.finalized || false

        if (data.vitals) {
          Object.assign(vitals, data.vitals)
        }

        if (data.noteDate) {
          form.noteDate = new Date(data.noteDate)
        }

        // Set patient info from note data
        if (data.patientId && data.patientName) {
          selectedPatient.value = {
            id: data.patientId,
            name: data.patientName,
            patientId: data.patientIdNumber
          }
          // Add to options if not there
          if (!patientOptions.value.find(p => p.id === data.patientId)) {
            patientOptions.value.unshift(selectedPatient.value)
          }
        }
      } catch (e) {
        console.error('Failed to load note:', e)
        ElMessage.error('Failed to load note')
        router.back()
      } finally {
        loading.value = false
      }
    }

    const applyTemplate = async () => {
      if (!templateId.value) return
      try {
        const res = await http.post(`/clinical-notes/templates/${templateId.value}/apply`)
        const data = res.data

        if (form.noteType === 'soap') {
          form.subjective = data.subjective || ''
          form.objective = data.objective || ''
          form.assessment = data.assessment || ''
          form.plan = data.plan || ''
        } else {
          form.content = data.content || ''
        }

        ElMessage.success('Template applied')
      } catch (e) {
        console.error('Failed to apply template:', e)
        ElMessage.error('Failed to apply template')
      }
    }

    const buildPayload = () => {
      const payload = {
        patientId: form.patientId,
        appointmentId: form.appointmentId,
        noteType: form.noteType,
        title: form.title,
        noteDate: form.noteDate instanceof Date ?
            form.noteDate.toISOString().split('T')[0] :
            form.noteDate,
        subjective: form.subjective,
        objective: form.objective,
        assessment: form.assessment,
        plan: form.plan,
        content: form.content,
        vitals: (vitals.bp || vitals.hr || vitals.temp || vitals.rr || vitals.spo2) ?
            { ...vitals } : null
      }

      return payload
    }

    const save = async () => {
      // Validate patient selection
      if (!form.patientId) {
        ElMessage.error('Please select a patient before saving')
        return
      }

      // Validate form
      if (!formRef.value) return

      try {
        await formRef.value.validate()
      } catch (e) {
        ElMessage.warning('Please fill in all required fields')
        return
      }

      saving.value = true
      try {
        const payload = buildPayload()
        console.log('Saving note with payload:', payload)

        if (props.noteId) {
          // Update existing note
          const res = await http.put(`/clinical-notes/${props.noteId}`, payload)
          ElMessage.success('Note updated successfully')
          form.id = res.data.id
        } else {
          // Create new note
          const res = await http.post('/clinical-notes', payload)
          ElMessage.success('Note saved as draft')
          form.id = res.data.id
          startAutoSave()
        }
      } catch (e) {
        console.error('Save error:', e)
        const errorMsg = e.response?.data?.message || e.message || 'Failed to save note'
        ElMessage.error(errorMsg)
      } finally {
        saving.value = false
      }
    }

    const autoSave = async () => {
      if (form.finalized || !form.id) return
      try {
        await http.put(`/clinical-notes/${form.id}`, buildPayload())
        autoSaveStatus.value = 'Auto-saved'
        setTimeout(() => { autoSaveStatus.value = '' }, 2000)
      } catch (e) {
        console.error('Auto-save failed:', e)
      }
    }

    const startAutoSave = () => {
      if (autoSaveTimer.value) clearInterval(autoSaveTimer.value)
      autoSaveTimer.value = setInterval(autoSave, 30000)
    }

    const finalize = async () => {
      try {
        await ElMessageBox.confirm(
            'Once finalized, the note cannot be edited. Continue?',
            'Finalize Note',
            { type: 'warning', confirmButtonText: 'Finalize', cancelButtonText: 'Cancel' }
        )

        // Save first if not saved
        if (!form.id) {
          await save()
        }

        if (!form.id) {
          ElMessage.error('Please save the note first')
          return
        }

        finalizing.value = true
        await http.post(`/clinical-notes/${form.id}/finalize`)
        form.finalized = true
        ElMessage.success('Note finalized successfully')

        if (autoSaveTimer.value) {
          clearInterval(autoSaveTimer.value)
        }

        // Redirect to notes list after 1 second
        setTimeout(() => {
          router.push('/clinical-notes')
        }, 1000)

      } catch (e) {
        if (e !== 'cancel') {
          console.error('Finalize error:', e)
          ElMessage.error('Failed to finalize note')
        }
      } finally {
        finalizing.value = false
      }
    }

    // Watch for content changes to show unsaved status
    watch([
      () => form.subjective,
      () => form.objective,
      () => form.assessment,
      () => form.plan,
      () => form.content,
      () => form.title
    ], () => {
      if (form.id && !form.finalized) {
        autoSaveStatus.value = 'Unsaved changes...'
      }
    })

    onMounted(async () => {
      console.log('ClinicalNoteEditor mounted with props:', props)

      // Load templates
      await loadTemplates()

      // Handle different scenarios
      if (props.noteId) {
        // Editing existing note
        await loadNote()
      } else {
        // Creating new note - load patients
        await loadInitialPatients()
      }
    })

    onUnmounted(() => {
      if (autoSaveTimer.value) {
        clearInterval(autoSaveTimer.value)
      }
    })

    return {
      loading, saving, finalizing, formRef, form, vitals, rules,
      templates, templateId, autoSaveStatus,
      searchingPatients, patientOptions, selectedPatient, isEditMode,
      loadTemplates, applyTemplate, save, finalize,
      searchPatients, onPatientSelected, getGenderLabel
    }
  }
}
</script>

<style scoped>
.clinical-note-editor {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.soap-sections {
  margin-top: 20px;
}

.vitals-card {
  margin-bottom: 20px;
  background: #f0f9ff;
}

.hint {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.el-select {
  width: 100%;
}
</style>