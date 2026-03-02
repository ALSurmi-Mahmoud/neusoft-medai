<template>
  <div class="report-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Diagnostic Reports</span>
          <div class="header-actions">
            <el-button type="primary" @click="loadReports">
              <el-icon><Search /></el-icon>
              Refresh
            </el-button>
            <el-button type="primary" @click="exportPDF" :loading="exporting">
              <el-icon><Download /></el-icon>
              Export PDF
            </el-button>
          </div>
        </div>
      </template>

      <!-- Filters -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="Status">
          <el-select v-model="filters.status" placeholder="All" clearable @change="loadReports">
            <el-option label="DRAFT" value="DRAFT" />
            <el-option label="FINAL" value="FINAL" />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- Table -->
      <el-table
          v-loading="loading"
          :data="reports"
          stripe
          border
          style="width: 100%"
      >
        <el-table-column prop="reportUid" label="Report UID" min-width="220" />
        <el-table-column prop="patientId" label="Patient ID" width="140" />
        <el-table-column prop="authorName" label="Author" width="180" />
        <el-table-column prop="title" label="Title" min-width="220" />
        <el-table-column prop="status" label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="finalized" label="Finalized" width="120">
          <template #default="{ row }">
            <el-tag :type="row.finalized ? 'success' : 'info'">
              {{ row.finalized ? 'Yes' : 'No' }}
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

              <!-- Add to template in actions column -->
              <el-button size="small" type="success" @click="openExportDialog(row)">
                <el-icon><Download /></el-icon>
                Export
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- Add after table -->
      <ReportExportDialog
          ref="exportDialog"
          :report-id="selectedReportId"
          category="radiology"
          @exported="loadReports"
      />

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

        <div class="report-content">
          <h4>Content</h4>
          <pre class="content-pre">{{ selectedReport.content || 'No content' }}</pre>
        </div>
      </div>

      <template #footer>
        <el-button @click="viewDialogVisible = false">Close</el-button>
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
import ReportExportDialog from './reports/ReportExportDialog.vue'


export default {
  name: 'ReportListView',
  components: { Search, View, Edit, Delete, Check, Download, ReportExportDialog },
  setup() {
    const router = useRouter()

    const loading = ref(false)
    const reports = ref([])
    const selectedReport = ref(null)
    const viewDialogVisible = ref(false)
    const exporting = ref(false)

    // Add to setup()
    const exportDialog = ref(null)
    const selectedReportId = ref(null)

    const openExportDialog = (report) => {
      selectedReportId.value = report.id
      exportDialog.value.open()
    }

    const filters = reactive({
      status: ''
    })

    const pagination = reactive({
      page: 1,
      size: 10,
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

        const res = await http.get('/reports', { params })
        reports.value = res.data.content || []
        pagination.total = res.data.totalElements || 0
      } catch (e) {
        ElMessage.error(e?.response?.data?.message || 'Failed to load reports')
      } finally {
        loading.value = false
      }
    }

    const viewReport = async (report) => {
      try {
        const res = await http.get(`/reports/${report.id}`)
        selectedReport.value = res.data
        viewDialogVisible.value = true
      } catch (e) {
        ElMessage.error(e?.response?.data?.message || 'Failed to load report')
      }
    }

    const editReport = (report) => {
      router.push({ name: 'ReportEdit', params: { id: report.id } })
    }

    const confirmDelete = async (report) => {
      try {
        await ElMessageBox.confirm(
            `Delete report ${report.reportUid}?`,
            'Confirm',
            { type: 'warning' }
        )
        await http.delete(`/reports/${report.id}`)
        ElMessage.success('Deleted')
        await loadReports()
      } catch (e) {
        if (e !== 'cancel') {
          ElMessage.error(e?.response?.data?.message || 'Delete failed')
        }
      }
    }

    const exportPDF = async () => {
      exporting.value = true
      // TODO: Implement PDF export
      try {
        ElMessage.info('PDF export feature coming soon')
      } finally {
        exporting.value = false
      }
    }

    const formatDate = (val) => {
      if (!val) return '-'
      try {
        return new Date(val).toLocaleString()
      } catch {
        return String(val)
      }
    }

    const getStatusType = (status) => {
      switch (status) {
        case 'FINAL':
          return 'success'
        case 'DRAFT':
          return 'warning'
        default:
          return 'info'
      }
    }

    onMounted(() => {
      loadReports()
    })

    // Add to return
    return {
      loading,
      reports,
      selectedReport,
      viewDialogVisible,
      exporting,
      exportDialog,
      selectedReportId,
      openExportDialog,
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

.header-actions {
  display: flex;
  gap: 10px;
}

.filter-form {
  margin-bottom: 16px;
}

.report-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-content .content-pre {
  white-space: pre-wrap;
  background: #f7f7f7;
  padding: 12px;
  border-radius: 6px;
  max-height: 300px;
  overflow: auto;
}
</style>
