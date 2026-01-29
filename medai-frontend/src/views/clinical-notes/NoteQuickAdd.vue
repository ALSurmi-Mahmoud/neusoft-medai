<template>
  <el-dialog
      v-model="dialogVisible"
      title="Quick Add Clinical Note"
      width="700px"
      @closed="resetForm"
  >
    <el-form :model="form" ref="formRef" :rules="rules" label-width="100px">
      <el-form-item label="Note Type" prop="noteType">
        <el-select v-model="form.noteType" placeholder="Select type" style="width:100%;">
          <el-option label="SOAP Note" value="soap" />
          <el-option label="Progress Note" value="progress" />
          <el-option label="Follow-up" value="followup" />
        </el-select>
      </el-form-item>

      <el-form-item label="Title" prop="title">
        <el-input v-model="form.title" placeholder="Brief note title" />
      </el-form-item>

      <div v-if="form.noteType === 'soap'">
        <el-form-item label="Subjective">
          <el-input v-model="form.subjective" type="textarea" :rows="3" placeholder="Chief complaint..." />
        </el-form-item>
        <el-form-item label="Assessment">
          <el-input v-model="form.assessment" type="textarea" :rows="3" placeholder="Diagnosis..." />
        </el-form-item>
        <el-form-item label="Plan">
          <el-input v-model="form.plan" type="textarea" :rows="3" placeholder="Treatment plan..." />
        </el-form-item>
      </div>
      <div v-else>
        <el-form-item label="Content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="Note content..." />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">Cancel</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">Save Note</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ref, reactive, computed } from 'vue'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'

export default {
  name: 'NoteQuickAdd',
  props: {
    modelValue: Boolean,
    patientId: { type: Number, required: true },
    appointmentId: { type: Number, default: null }
  },
  emits: ['update:modelValue', 'note-added'],
  setup(props, { emit }) {
    const saving = ref(false)
    const formRef = ref(null)

    const dialogVisible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    const form = reactive({
      noteType: 'soap',
      title: '',
      subjective: '',
      assessment: '',
      plan: '',
      content: ''
    })

    const rules = {
      noteType: [{ required: true, message: 'Required', trigger: 'change' }],
      title: [{ required: true, message: 'Required', trigger: 'blur' }]
    }

    const handleSave = async () => {
      if (!formRef.value) return
      try { await formRef.value.validate() } catch (e) { return }

      saving.value = true
      try {
        const payload = {
          patientId: props.patientId,
          appointmentId: props.appointmentId,
          noteType: form.noteType,
          title: form.title,
          noteDate: new Date().toISOString().split('T')[0]
        }

        if (form.noteType === 'soap') {
          payload.subjective = form.subjective
          payload.assessment = form.assessment
          payload.plan = form.plan
        } else {
          payload.content = form.content
        }

        const res = await http.post('/clinical-notes', payload)
        ElMessage.success('Note saved')
        emit('note-added', res.data)
        dialogVisible.value = false
      } catch (e) {
        ElMessage.error('Failed to save note')
      } finally {
        saving.value = false
      }
    }

    const resetForm = () => {
      form.noteType = 'soap'
      form.title = ''
      form.subjective = ''
      form.assessment = ''
      form.plan = ''
      form.content = ''
    }

    return {
      saving,
      formRef,
      dialogVisible,
      form,
      rules,
      handleSave,
      resetForm
    }
  }
}
</script>