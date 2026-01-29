<template>
  <div class="clinical-note-list">
    <el-card>
      <template #header>
        <div class="header">
          <span>Clinical Notes</span>
          <el-button type="primary" @click="createNote">
            <el-icon><Plus /></el-icon> New Note
          </el-button>
        </div>
      </template>

      <!-- Filters -->
      <el-row :gutter="15" style="margin-bottom: 20px;">
        <el-col :span="8">
          <el-input v-model="search" placeholder="Search notes..." clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-col>
        <el-col :span="5">
          <el-select v-model="filterType" placeholder="Note type" clearable style="width:100%;">
            <el-option label="SOAP" value="soap" />
            <el-option label="Progress" value="progress" />
            <el-option label="Consultation" value="consultation" />
          </el-select>
        </el-col>
        <el-col :span="5">
          <el-select v-model="filterStatus" placeholder="Status" clearable style="width:100%;">
            <el-option label="Draft" value="draft" />
            <el-option label="Finalized" value="finalized" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button @click="loadNotes" style="width:100%;">Apply Filters</el-button>
        </el-col>
      </el-row>

      <!-- Table -->
      <el-table :data="filteredNotes" v-loading="loading" stripe>
        <el-table-column prop="noteDate" label="Date" width="110" sortable>
          <template #default="{row}">{{ formatDate(row.noteDate) }}</template>
        </el-table-column>
        <el-table-column prop="noteType" label="Type" width="110">
          <template #default="{row}">
            <el-tag size="small">{{ row.noteType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="Title" min-width="200" />
        <el-table-column prop="patientName" label="Patient" width="140" />
        <el-table-column prop="doctorName" label="Doctor" width="130" />
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{row}">
            <el-tag :type="row.status === 'draft' ? 'warning' : 'success'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="180" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="viewNote(row)">
              <el-icon><View /></el-icon>
            </el-button>
            <el-button v-if="row.status === 'draft'" size="small" type="primary" @click="editNote(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button v-if="row.status === 'draft'" size="small" type="danger" @click="deleteNote(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Detail Dialog -->
    <ClinicalNoteDetail v-model="detailVisible" :note-id="selectedNoteId" @closed="loadNotes" />
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, View, Edit, Delete } from '@element-plus/icons-vue'
import ClinicalNoteDetail from './ClinicalNoteDetail.vue'

export default {
  name: 'ClinicalNoteList',
  components: { Plus, Search, View, Edit, Delete, ClinicalNoteDetail },
  props: {
    patientId: { type: Number, default: null }
  },
  setup(props) {
    const router = useRouter()
    const loading = ref(false)
    const notes = ref([])
    const search = ref('')
    const filterType = ref('')
    const filterStatus = ref('')
    const detailVisible = ref(false)
    const selectedNoteId = ref(null)

    const filteredNotes = computed(() => {
      let result = notes.value
      if (search.value) {
        const q = search.value.toLowerCase()
        result = result.filter(n =>
            n.title?.toLowerCase().includes(q) ||
            n.subjective?.toLowerCase().includes(q) ||
            n.content?.toLowerCase().includes(q)
        )
      }
      return result
    })

    const loadNotes = async () => {
      loading.value = true
      try {
        const params = {}
        if (props.patientId) params.patientId = props.patientId
        if (filterType.value) params.noteType = filterType.value
        if (filterStatus.value) params.status = filterStatus.value

        const res = await http.get('/clinical-notes', { params })
        notes.value = res.data
      } catch (e) {
        ElMessage.error('Failed to load notes')
      } finally {
        loading.value = false
      }
    }

    const createNote = () => {
      if (props.patientId) {
        router.push({
          name: 'NewClinicalNote',
          query: { patientId: props.patientId }
        })
      } else {
        router.push({ name: 'NewClinicalNote' })
      }
    }

    const viewNote = (note) => {
      selectedNoteId.value = note.id
      detailVisible.value = true
    }

    const editNote = (note) => {
      router.push({ name: 'EditClinicalNote', params: { id: note.id } })
    }

    const deleteNote = async (note) => {
      try {
        await ElMessageBox.confirm('Delete this note?', 'Confirm', { type: 'warning' })
        await http.delete(`/clinical-notes/${note.id}`)
        ElMessage.success('Note deleted')
        loadNotes()
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('Failed to delete')
      }
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleDateString()
    }

    onMounted(loadNotes)

    return {
      loading, notes, search, filterType, filterStatus,
      filteredNotes, detailVisible, selectedNoteId,
      loadNotes, createNote, viewNote, editNote, deleteNote, formatDate
    }
  }
}
</script>

<style scoped>
.clinical-note-list { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; }
</style>