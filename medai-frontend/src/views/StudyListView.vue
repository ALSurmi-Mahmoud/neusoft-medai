<template>
  <div class="study-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Medical Studies</span>
          <el-button type="primary" @click="$router.push('/upload')">
            <el-icon><Upload /></el-icon>
            Upload New
          </el-button>
        </div>
      </template>

      <!-- Filters -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="Patient ID">
          <el-input
              v-model="filters.patientId"
              placeholder="Enter Patient ID"
              clearable
              style="width: 150px;"
          />
        </el-form-item>
        <el-form-item label="Modality">
          <el-select v-model="filters.modality" placeholder="All" clearable style="width: 120px;">
            <el-option label="CT" value="CT" />
            <el-option label="MR" value="MR" />
            <el-option label="US" value="US" />
            <el-option label="XR" value="XR" />
            <el-option label="Other" value="OT" />
          </el-select>
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" placeholder="All" clearable style="width: 120px;">
            <el-option label="Uploaded" value="uploaded" />
            <el-option label="Processing" value="processing" />
            <el-option label="Completed" value="completed" />
            <el-option label="Archived" value="archived" />
          </el-select>
        </el-form-item>
        <el-form-item label="Date Range">
          <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="To"
              start-placeholder="Start date"
              end-placeholder="End date"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 260px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadStudies">
            <el-icon><Search /></el-icon>
            Search
          </el-button>
          <el-button @click="resetFilters">Reset</el-button>
        </el-form-item>
      </el-form>

      <!-- Studies Table -->
      <el-table
          :data="studies"
          v-loading="loading"
          style="width: 100%"
          @row-click="handleRowClick"
          highlight-current-row
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="patientId" label="Patient ID" width="130">
          <template #default="{ row }">
            <span class="patient-id">{{ row.patientId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="patientName" label="Patient Name" width="150">
          <template #default="{ row }">
            {{ row.patientName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="modality" label="Modality" width="100">
          <template #default="{ row }">
            <el-tag :type="getModalityType(row.modality)">{{ row.modality || 'N/A' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="Description" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.description || 'No description' }}
          </template>
        </el-table-column>
        <el-table-column prop="seriesCount" label="Series" width="80" align="center" />
        <el-table-column prop="instanceCount" label="Images" width="80" align="center" />
        <el-table-column prop="studyDate" label="Study Date" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.studyDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="220" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-tooltip content="View Details" placement="top">
                <el-button size="small" type="primary" @click.stop="viewStudy(row)">
                  <el-icon><View /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="Open Viewer" placement="top">
                <el-button size="small" type="success" @click.stop="openViewer(row)">
                  <el-icon><Picture /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="Create Report" placement="top">
                <el-button size="small" type="warning" @click.stop="createReport(row)">
                  <el-icon><Document /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="Delete Study" placement="top">
                <el-button size="small" type="danger" @click.stop="deleteStudy(row)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadStudies"
          @current-change="loadStudies"
          style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- Study Detail Dialog -->
    <el-dialog v-model="detailDialogVisible" title="Study Details" width="700px" destroy-on-close>
      <div v-if="selectedStudy" class="study-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Study UID">
            {{ selectedStudy.studyUid || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Patient ID">
            {{ selectedStudy.patient?.patientId || selectedStudy.patientId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Patient Name">
            {{ selectedStudy.patient?.name || selectedStudy.patientName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Patient Sex">
            {{ selectedStudy.patient?.sex || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Modality">
            <el-tag :type="getModalityType(selectedStudy.modality)">
              {{ selectedStudy.modality || 'N/A' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="getStatusType(selectedStudy.status)">
              {{ selectedStudy.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Study Date" :span="2">
            {{ formatDateTime(selectedStudy.studyDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="Description" :span="2">
            {{ selectedStudy.description || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 20px;">Series ({{ selectedStudy.series?.length || 0 }})</h4>
        <el-table :data="selectedStudy.series || []" size="small" max-height="250">
          <el-table-column prop="seriesNumber" label="#" width="60" />
          <el-table-column prop="modality" label="Modality" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ row.modality || 'N/A' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="Description" show-overflow-tooltip />
          <el-table-column prop="imageCount" label="Images" width="80" align="center" />
          <el-table-column prop="manufacturer" label="Manufacturer" width="120" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">Close</el-button>
        <el-button type="success" @click="openViewer(selectedStudy)">
          <el-icon><Picture /></el-icon>
          Open Viewer
        </el-button>
        <el-button type="primary" @click="createReport(selectedStudy)">
          <el-icon><Document /></el-icon>
          Create Report
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Search, View, Picture, Document, Delete } from '@element-plus/icons-vue'

export default {
  name: 'StudyListView',
  components: { Upload, Search, View, Picture, Document, Delete },
  setup() {
    const router = useRouter()

    const loading = ref(false)
    const studies = ref([])
    const selectedStudy = ref(null)
    const detailDialogVisible = ref(false)

    const filters = reactive({
      patientId: '',
      modality: '',
      status: '',
      dateRange: null
    })

    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })

    // Load studies from API
    const loadStudies = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.page - 1,
          size: pagination.size
        }

        if (filters.patientId) params.patientId = filters.patientId
        if (filters.modality) params.modality = filters.modality
        if (filters.status) params.status = filters.status
        if (filters.dateRange && filters.dateRange.length === 2) {
          params.dateFrom = filters.dateRange[0]
          params.dateTo = filters.dateRange[1]
        }

        const response = await http.get('/studies', { params })
        studies.value = response.data.content || []
        pagination.total = response.data.totalElements || 0
      } catch (error) {
        console.error('Failed to load studies:', error)
        ElMessage.error('Failed to load studies')
        studies.value = []
      } finally {
        loading.value = false
      }
    }

    // Reset all filters
    const resetFilters = () => {
      filters.patientId = ''
      filters.modality = ''
      filters.status = ''
      filters.dateRange = null
      pagination.page = 1
      loadStudies()
    }

    // View study details in dialog
    const viewStudy = async (study) => {
      try {
        loading.value = true
        const response = await http.get(`/studies/${study.id}`)
        selectedStudy.value = response.data
        detailDialogVisible.value = true
      } catch (error) {
        console.error('Failed to load study details:', error)
        ElMessage.error('Failed to load study details')
      } finally {
        loading.value = false
      }
    }

    // Handle row click
    const handleRowClick = (row) => {
      viewStudy(row)
    }

    // Open DICOM viewer
    const openViewer = (study) => {
      if (study) {
        detailDialogVisible.value = false
        router.push(`/viewer/${study.id}`)
      }
    }

    // Create new report for study
    const createReport = (study) => {
      if (study) {
        detailDialogVisible.value = false
        router.push(`/reports/new/${study.id}`)
      }
    }

    // Delete study with confirmation
    const deleteStudy = async (study) => {
      try {
        await ElMessageBox.confirm(
            `Are you sure you want to delete this study? This action cannot be undone.`,
            'Delete Study',
            {
              confirmButtonText: 'Delete',
              cancelButtonText: 'Cancel',
              type: 'warning',
              confirmButtonClass: 'el-button--danger'
            }
        )

        loading.value = true
        await http.delete(`/studies/${study.id}`)
        ElMessage.success('Study deleted successfully')
        loadStudies()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Failed to delete study:', error)
          ElMessage.error('Failed to delete study')
        }
      } finally {
        loading.value = false
      }
    }

    // Format date/time for display
    const formatDateTime = (dateStr) => {
      if (!dateStr) return '-'
      try {
        return new Date(dateStr).toLocaleString()
      } catch {
        return dateStr
      }
    }

    // Get tag type for modality
    const getModalityType = (modality) => {
      const types = {
        'CT': 'danger',
        'MR': 'success',
        'US': 'warning',
        'XR': 'info',
        'OT': ''
      }
      return types[modality] || ''
    }

    // Get tag type for status
    const getStatusType = (status) => {
      const types = {
        'uploaded': 'success',
        'processing': 'warning',
        'completed': 'success',
        'failed': 'danger',
        'pending': 'info',
        'available': 'success'
      }
      return types[status?.toLowerCase()] || 'info'
    }

    onMounted(() => {
      loadStudies()
    })

    return {
      loading,
      studies,
      selectedStudy,
      detailDialogVisible,
      filters,
      pagination,
      loadStudies,
      resetFilters,
      viewStudy,
      handleRowClick,
      openViewer,
      createReport,
      deleteStudy,
      formatDateTime,
      getModalityType,
      getStatusType,
    }
  }
}
</script>

<style scoped>
.study-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-form {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.patient-id {
  font-family: monospace;
  font-weight: 500;
}

.study-detail {
  max-height: 60vh;
  overflow-y: auto;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .filter-form {
    padding: 10px;
  }

  .filter-form .el-form-item {
    margin-bottom: 10px;
  }
}
</style>