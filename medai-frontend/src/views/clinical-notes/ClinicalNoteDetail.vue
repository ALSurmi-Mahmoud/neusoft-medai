<template>
  <el-dialog
      v-model="dialogVisible"
      title="Clinical Note Detail"
      width="900px"
      @closed="handleClose"
  >
    <div v-loading="loading" class="note-detail">
      <div v-if="note" class="note-content">
        <!-- Header Info -->
        <div class="note-header">
          <div class="header-row">
            <div class="info-item">
              <label>Note ID:</label>
              <span>{{ note.noteUid }}</span>
            </div>
            <div class="info-item">
              <label>Status:</label>
              <el-tag :type="note.finalized ? 'success' : 'warning'">
                {{ note.finalized ? 'Finalized' : 'Draft' }}
              </el-tag>
            </div>
            <div class="info-item">
              <label>Type:</label>
              <el-tag>{{ note.noteType }}</el-tag>
            </div>
          </div>
          <div class="header-row">
            <div class="info-item">
              <label>Patient:</label>
              <span>{{ note.patientName }}</span>
            </div>
            <div class="info-item">
              <label>Doctor:</label>
              <span>{{ note.doctorName }}</span>
            </div>
            <div class="info-item">
              <label>Date:</label>
              <span>{{ formatDate(note.noteDate) }}</span>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- Title -->
        <h3>{{ note.title }}</h3>

        <!-- SOAP Note -->
        <div v-if="note.noteType === 'soap'" class="soap-content">
          <!-- Vitals -->
          <div v-if="note.vitals && hasVitals" class="vitals-section">
            <h4>Vital Signs</h4>
            <el-row :gutter="15">
              <el-col :span="8" v-if="note.vitals.bp">
                <div class="vital-item">
                  <label>Blood Pressure:</label>
                  <span>{{ note.vitals.bp }} mmHg</span>
                </div>
              </el-col>
              <el-col :span="8" v-if="note.vitals.hr">
                <div class="vital-item">
                  <label>Heart Rate:</label>
                  <span>{{ note.vitals.hr }} bpm</span>
                </div>
              </el-col>
              <el-col :span="8" v-if="note.vitals.temp">
                <div class="vital-item">
                  <label>Temperature:</label>
                  <span>{{ note.vitals.temp }} °F</span>
                </div>
              </el-col>
              <el-col :span="8" v-if="note.vitals.rr">
                <div class="vital-item">
                  <label>Respiratory Rate:</label>
                  <span>{{ note.vitals.rr }} /min</span>
                </div>
              </el-col>
              <el-col :span="8" v-if="note.vitals.spo2">
                <div class="vital-item">
                  <label>SpO2:</label>
                  <span>{{ note.vitals.spo2 }} %</span>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- SOAP Sections -->
          <div class="soap-section" v-if="note.subjective">
            <h4>S - Subjective</h4>
            <p class="section-content">{{ note.subjective }}</p>
          </div>

          <div class="soap-section" v-if="note.objective">
            <h4>O - Objective</h4>
            <p class="section-content">{{ note.objective }}</p>
          </div>

          <div class="soap-section" v-if="note.assessment">
            <h4>A - Assessment</h4>
            <p class="section-content">{{ note.assessment }}</p>
          </div>

          <div class="soap-section" v-if="note.plan">
            <h4>P - Plan</h4>
            <p class="section-content">{{ note.plan }}</p>
          </div>
        </div>

        <!-- General Content -->
        <div v-else class="general-content">
          <p class="section-content">{{ note.content }}</p>
        </div>

        <!-- Footer Info -->
        <el-divider />
        <div class="note-footer">
          <small>Created: {{ formatDateTime(note.createdAt) }}</small>
          <small v-if="note.finalizedAt">Finalized: {{ formatDateTime(note.finalizedAt) }}</small>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">Close</el-button>
      <el-button type="primary" @click="printNote">
        <el-icon><Printer /></el-icon> Print
      </el-button>
      <el-button v-if="note && !note.finalized" type="primary" @click="editNote">
        <el-icon><Edit /></el-icon> Edit
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { Printer, Edit } from '@element-plus/icons-vue'

export default {
  name: 'ClinicalNoteDetail',
  components: { Printer, Edit },
  props: {
    modelValue: Boolean,
    noteId: Number
  },
  emits: ['update:modelValue', 'closed'],
  setup(props, { emit }) {
    const router = useRouter()
    const loading = ref(false)
    const note = ref(null)

    const dialogVisible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    const hasVitals = computed(() => {
      if (!note.value?.vitals) return false
      return Object.values(note.value.vitals).some(v => v)
    })

    const loadNote = async () => {
      if (!props.noteId) return

      loading.value = true
      try {
        const res = await http.get(`/clinical-notes/${props.noteId}`)
        note.value = res.data
      } catch (e) {
        ElMessage.error('Failed to load note')
        handleClose()
      } finally {
        loading.value = false
      }
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleDateString()
    }

    const formatDateTime = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleString()
    }

    const printNote = () => {
      window.print()
    }

    const editNote = () => {
      router.push({ name: 'EditClinicalNote', params: { id: note.value.id } })
      handleClose()
    }

    const handleClose = () => {
      dialogVisible.value = false
      note.value = null
      emit('closed')
    }

    watch(() => props.noteId, () => {
      if (props.noteId && props.modelValue) {
        loadNote()
      }
    })

    watch(dialogVisible, (val) => {
      if (val && props.noteId) {
        loadNote()
      }
    })

    return {
      loading,
      note,
      dialogVisible,
      hasVitals,
      formatDate,
      formatDateTime,
      printNote,
      editNote,
      handleClose
    }
  }
}
</script>

<style scoped>
.note-detail {
  padding: 10px;
}

.note-header {
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

.vitals-section {
  background: #f0f9ff;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.vitals-section h4 {
  margin-top: 0;
  margin-bottom: 15px;
}

.vital-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.vital-item label {
  font-size: 12px;
  color: #909399;
}

.vital-item span {
  font-weight: 600;
}

.soap-section {
  margin-bottom: 25px;
}

.soap-section h4 {
  color: #409eff;
  margin-bottom: 10px;
  font-size: 16px;
}

.section-content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
}

.general-content {
  margin: 20px 0;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  color: #909399;
}

@media print {
  .el-dialog__footer {
    display: none;
  }
}
</style>