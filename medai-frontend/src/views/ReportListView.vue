<template>
  <div class="report-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Diagnostic Reports</span>
        </div>
      </template>

      <!-- Filters -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="Status">
          <el-select v-model="filters.status" placeholder="All" clearable @change="loadReports">
            <el-option label="Draft" value="draft" />
            <el-option label="Finalized" value="finalized" />
            <el-option label="Amended" value="amended" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadReports">
            <el-icon><Search /></el-icon>
            Search
          </el-button>
        </el-form-item>
      </el-form>

      <!-- Reports Table -->
      <el-table :data="reports" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="reportUid" label="Report UID" width="150">
          <template #default="{ row }">
            <span class="uid-text">{{ row.reportUid }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="Title" min-width="200">
          <template #default="{ row }">
            {{ row.title || 'Untitled Report' }}
          </template>
        </el-table-column>
        <el-table-column prop="patientId" label="Patient" width="130" />
        <el-table-column prop="authorName" label="Author" width="150" />
        <el-table-column prop="status" label="Status" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.status }}
              <el-icon v-if="row.finalized"><Check /></el-icon>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" type="primary" @click="viewReport(row)">
                <el-icon><View /></el-icon>
              </el-button>
              <el-button
                  size="small"
                  type="warning"
                  @click="editReport(row)"
                  :disabled="row.finalized"
              >
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button
                  size="small"
                  type="danger"
                  @click="confirmDelete(row)"
                  :disabled="row.finalized"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadReports"
          @current-change="loadReports"
          style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- View Report Dialog -->
    <el-dialog v-model="viewDialogVisible" title="Report Details" width="800px">
      <div v-if="selectedReport" class="report-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Report UID">{{ selectedReport.reportUid }}</el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="getStatusType(selectedReport.status)">{{ selectedReport.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Patient ID">{{ selectedReport.patientId }}</el-descriptions-item>
          <el-descriptions-item label="Author">{{ selectedReport.authorName }}</el-descriptions-item>
          <el-descriptions-item label="Created">{{ formatDate(selectedReport.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="Finalized">
            {{ selectedReport.finalizedAt ? formatDate(selectedReport.finalizedAt) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Title" :span="2">{{ selectedReport.title || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <h4>Summary</h4>
        <p class="report-text">{{ selectedReport.summary || 'No summary provided' }}</p>

        <h4>Findings</h4>
        <p class="report-text">{{ selectedReport.findings || 'No findings documented' }}</p>

        <h4>Impression</h4>
        <p class="report-text">{{ selectedReport.impression || 'No impression documented' }}</p>

        <h4>Recommendations</h4>
        <p class="report-text">{{ selectedReport.recommendations || 'No recommendations' }}</p>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">Close</el-button>
        <el-button type="primary" @click="exportPDF" :loading="exporting">
          <el-icon><Download /></el-icon>
          Export PDF
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
import { Search, View, Edit, Delete, Check, Download } from '@element-plus/icons-vue'

export default {
  name: 'ReportListView',
  components: { Search, View, Edit, Delete, Check, Download },
  setup() {
    const router = useRouter()

    const loading = ref(false)
    const reports = ref([])
    const selectedReport = ref(null)
    const viewDialogVisible = ref(false)
    const exporting = ref(false)

    const filters = reactive({
      status: ''
    })

    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })

    const loadReports = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.page - 1,
          size: pagination.size
        }
        if (filters.status) params.status = filters.status

        const response = await http.get('/reports', { params })
        reports.value = response.data.content || []
        pagination.total = response.data.totalElements || 0
      } catch (error) {
        console.error('Failed to load reports:', error)
        ElMessage.error('Failed to load reports')
      } finally {
        loading.value = false
      }
    }

    const viewReport = (report) => {
      selectedReport.value = report
      viewDialogVisible.value = true
    }

    const editReport = (report) => {
      router.push(`/reports/${report.id}/edit`)
    }

    const confirmDelete = (report) => {
      ElMessageBox.confirm(
          `Are you sure you want to delete report "${report.reportUid}"?`,
          'Confirm Delete',
          {
            confirmButtonText: 'Delete',
            cancelButtonText: 'Cancel',
            type: 'warning'
          }
      ).then(async () => {
        try {
          await http.delete(`/reports/${report.id}`)
          ElMessage.success('Report deleted')
          loadReports()
        } catch (error) {
          ElMessage.error('Failed to delete report')
        }
      }).catch(() => {})
    }

    const exportPDF = async () => {
      exporting.value = true
      // TODO: Implement PDF export
      setTimeout(() => {
        ElMessage.info('PDF export feature coming soon')
        exporting.value = false
      }, 1000)
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleString()
    }

    const getStatusType = (status) => {
      const types = { 'draft': 'info', 'finalized': 'success', 'amended': 'warning' }
      return types[status] || 'info'
    }

    onMounted(() => {
      loadReports()
    })

    return {
      loading,
      reports,
      selectedReport,
      viewDialogVisible,
      exporting,
      filters,
      pagination,
      loadReports,
      viewReport,
      editReport,
      confirmDelete,
      exportPDF,
      formatDate,
      getStatusType
    }
  }
}
</script>

<style scoped>
.report-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-form {
  margin-bottom: 20px;
}

.uid-text {
  font-family: monospace;
  font-size: 12px;
}

.report-detail {
  max-height: 60vh;
  overflow-y: auto;
}

.report-text {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  min-height: 60px;
}

h4 {
  margin-top: 20px;
  margin-bottom: 10px;
  color: #333;
}
</style>