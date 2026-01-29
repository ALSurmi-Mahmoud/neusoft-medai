<template>
  <div class="clinical-notes-tab">
    <div class="tab-header">
      <el-statistic title="Total Notes" :value="notes.length" />
      <el-button type="primary" @click="quickAddVisible = true" :disabled="!patientId">
        <el-icon><Plus /></el-icon> Add Note
      </el-button>
    </div>

    <!-- Notes Table -->
    <el-table :data="notes" v-loading="loading" stripe style="margin-top: 20px;">
      <el-table-column prop="noteDate" label="Date" width="110" sortable>
        <template #default="{row}">{{ formatDate(row.noteDate) }}</template>
      </el-table-column>
      <el-table-column prop="noteType" label="Type" width="110">
        <template #default="{row}">
          <el-tag size="small">{{ row.noteType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="Title" min-width="200" />
      <el-table-column prop="doctorName" label="Doctor" width="130" />
      <el-table-column prop="status" label="Status" width="100">
        <template #default="{row}">
          <el-tag :type="row.status === 'draft' ? 'warning' : 'success'" size="small">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="150" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="viewNote(row)">
            <el-icon><View /></el-icon>
          </el-button>
          <el-button v-if="row.status === 'draft'" size="small" type="primary" @click="editNote(row)">
            <el-icon><Edit /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && notes.length === 0" description="No clinical notes yet" />

    <!-- Quick Add Dialog -->
    <NoteQuickAdd
        v-model="quickAddVisible"
        :patient-id="patientId"
        @note-added="handleNoteAdded"
    />

    <!-- Detail Dialog -->
    <ClinicalNoteDetail
        v-model="detailVisible"
        :note-id="selectedNoteId"
        @closed="loadNotes"
    />
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { Plus, View, Edit } from '@element-plus/icons-vue'
import NoteQuickAdd from './NoteQuickAdd.vue'
import ClinicalNoteDetail from './ClinicalNoteDetail.vue'

export default {
  name: 'ClinicalNotesTab',
  components: { Plus, View, Edit, NoteQuickAdd, ClinicalNoteDetail },
  props: {
    patientId: { type: Number, required: true }
  },
  setup(props) {
    const router = useRouter()
    const loading = ref(false)
    const notes = ref([])
    const quickAddVisible = ref(false)
    const detailVisible = ref(false)
    const selectedNoteId = ref(null)

    const loadNotes = async () => {
      // ✅ FIX: Check if patientId is valid before loading
      if (!props.patientId) {
        console.warn('ClinicalNotesTab: No patientId provided')
        notes.value = []
        return
      }

      loading.value = true
      try {
        const res = await http.get(`/clinical-notes/patient/${props.patientId}`)
        notes.value = res.data || []
      } catch (e) {
        console.error('Failed to load notes:', e)
        // ✅ FIX: Don't show error if it's just a 404 (no notes yet)
        if (e.response?.status !== 404) {
          ElMessage.error('Failed to load notes')
        }
        notes.value = []
      } finally {
        loading.value = false
      }
    }

    const viewNote = (note) => {
      selectedNoteId.value = note.id
      detailVisible.value = true
    }

    const editNote = (note) => {
      router.push({ name: 'EditClinicalNote', params: { id: note.id } })
    }

    const handleNoteAdded = (newNote) => {
      loadNotes()
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleDateString()
    }

    onMounted(() => {
      loadNotes()
    })

    return {
      loading,
      notes,
      quickAddVisible,
      detailVisible,
      selectedNoteId,
      loadNotes,
      viewNote,
      editNote,
      handleNoteAdded,
      formatDate
    }
  }
}
</script>

<style scoped>
.clinical-notes-tab {
  padding: 20px;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>