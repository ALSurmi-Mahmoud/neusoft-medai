<template>
  <div class="my-reports">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>My Medical Reports</span>
          <el-tag type="info" size="small">{{ reports.length }} Reports</el-tag>
        </div>
      </template>

      <div v-loading="loading">
        <!-- Reports Table -->
        <el-table :data="reports" style="width: 100%" v-if="reports.length > 0">
          <el-table-column prop="reportDate" label="Date" width="120" sortable>
            <template #default="{ row }">
              {{ formatDate(row.reportDate) }}
            </template>
          </el-table-column>

          <el-table-column prop="title" label="Report Title">
            <template #default="{ row }">
              <strong>{{ row.title || 'Medical Report' }}</strong>
            </template>
          </el-table-column>

          <el-table-column prop="doctorName" label="Doctor" width="180" />

          <el-table-column prop="reportType" label="Type" width="130">
            <template #default="{ row }">
              <el-tag size="small" :type="getReportType(row.reportType)">
                {{ row.reportType || 'General' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="Status" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'completed' ? 'success' : 'warning'">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Actions" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="viewReport(row)">
                <el-icon><View /></el-icon>
                View
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Empty State -->
        <el-empty
            v-if="!loading && reports.length === 0"
            description="No medical reports available yet"
        >
          <template #image>
            <el-icon :size="64" style="color: #909399"><Document /></el-icon>
          </template>
          <p style="color: #909399; margin-top: 10px;">
            Your doctor will create reports after your appointments and examinations.
          </p>
        </el-empty>
      </div>
    </el-card>

    <!-- Report View Dialog -->
    <el-dialog
        v-model="viewDialogVisible"
        title="Medical Report"
        width="800px"
        :close-on-click-modal="false"
    >
      <div v-if="selectedReport" class="report-content">
        <h2>{{ selectedReport.title || 'Medical Report' }}</h2>

        <el-descriptions :column="2" border style="margin: 20px 0;">
          <el-descriptions-item label="Report Date">
            {{ formatDate(selectedReport.reportDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="Doctor">
            {{ selectedReport.doctorName }}
          </el-descriptions-item>
          <el-descriptions-item label="Report Type" :span="1">
            {{ selectedReport.reportType || 'General' }}
          </el-descriptions-item>
          <el-descriptions-item label="Status" :span="1">
            <el-tag :type="selectedReport.status === 'completed' ? 'success' : 'warning'">
              {{ selectedReport.status }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="report-body">
          <div class="report-section">
            <h3><el-icon><Document /></el-icon> Findings</h3>
            <p>{{ selectedReport.findings || 'No findings recorded' }}</p>
          </div>

          <div class="report-section">
            <h3><el-icon><Check /></el-icon> Impression</h3>
            <p>{{ selectedReport.impression || 'No impression recorded' }}</p>
          </div>

          <div class="report-section">
            <h3><el-icon><Bell /></el-icon> Recommendations</h3>
            <p>{{ selectedReport.recommendations || 'No recommendations provided' }}</p>
          </div>

          <div v-if="selectedReport.notes" class="report-section">
            <h3><el-icon><Memo /></el-icon> Additional Notes</h3>
            <p>{{ selectedReport.notes }}</p>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">Close</el-button>
        <el-button type="primary" @click="downloadReport" disabled>
          <el-icon><Download /></el-icon>
          Download PDF (Coming Soon)
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { View, Document, Download, Check, Bell, Memo } from '@element-plus/icons-vue'

export default {
  name: 'MyReportsView',
  components: { View, Document, Download, Check, Bell, Memo },
  setup() {
    const loading = ref(true)
    const reports = ref([])
    const viewDialogVisible = ref(false)
    const selectedReport = ref(null)

    const loadReports = async () => {
      loading.value = true
      try {
        const response = await http.get('/reports/patient')  // ← CHANGED
        reports.value = response.data.map(report => ({
          id: report.id,
          reportDate: report.reportDate,  // ← Changed from report.date
          title: report.title || `Diagnostic Report #${report.id}`,
          doctorName: report.doctorName || 'Unknown',
          reportType: report.reportType || 'General',  // ← Changed from report.modality
          status: report.status,
          findings: report.findings,
          impression: report.impression,
          recommendations: report.recommendations,
          notes: report.notes
        })) || []
      } catch (error) {
        console.error('Failed to load reports:', error)
        reports.value = []
        ElMessage.warning('Unable to load reports at this time')
      } finally {
        loading.value = false
      }
    }

    const viewReport = (report) => {
      selectedReport.value = report
      viewDialogVisible.value = true
    }

    const downloadReport = () => {
      ElMessage.info('PDF download feature will be available in Phase 6')
    }

    const getReportType = (type) => {
      const types = {
        'Radiology': 'primary',
        'Laboratory': 'success',
        'Consultation': 'info',
        'Follow-up': 'warning',
        'General': 'info'
      }
      return types[type] || 'info'
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      })
    }

    onMounted(() => {
      loadReports()
    })

    return {
      loading,
      reports,
      viewDialogVisible,
      selectedReport,
      viewReport,
      downloadReport,
      getReportType,
      formatDate
    }
  }
}
</script>

<style scoped>
.my-reports {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.report-content h2 {
  margin: 0 0 20px;
  color: #333;
  font-size: 24px;
}

.report-body {
  margin-top: 20px;
}

.report-section {
  margin-bottom: 25px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.report-section h3 {
  margin: 0 0 10px;
  color: #409eff;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.report-section p {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #606266;
  margin: 0;
}
</style>