<template>
  <div class="my-reports">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>My Medical Reports</span>
        </div>
      </template>

      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="reports.length === 0">
        <el-empty description="No reports available yet" />
      </div>

      <div v-else class="reports-list">
        <el-card
            v-for="report in reports"
            :key="report.id"
            class="report-card"
            shadow="hover"
            @click="viewReport(report)"
        >
          <div class="report-content">
            <div class="report-icon-container">
              <el-icon :size="36"><Document /></el-icon>
            </div>
            <div class="report-info">
              <h3>{{ report.title }}</h3>
              <div class="report-meta">
                <span><el-icon><Calendar /></el-icon> {{ report.date }}</span>
                <span v-if="report.doctorName"><el-icon><User /></el-icon> {{ report.doctorName }}</span>
              </div>
              <div class="report-tags">
                <el-tag v-if="report.modality">{{ report.modality }}</el-tag>
                <el-tag :type="report.status === 'Final' ? 'success' : 'warning'" size="small">
                  {{ report.status }}
                </el-tag>
              </div>
            </div>
            <div class="report-actions">
              <el-button type="primary" circle @click.stop="viewReport(report)">
                <el-icon><View /></el-icon>
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </el-card>

    <!-- Report Viewer Dialog -->
    <el-dialog v-model="viewerVisible" :title="selectedReport?.title" width="700px">
      <div v-if="selectedReport" class="report-viewer">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Date">{{ selectedReport.date }}</el-descriptions-item>
          <el-descriptions-item label="Doctor">{{ selectedReport.doctorName || 'N/A' }}</el-descriptions-item>
          <el-descriptions-item label="Modality">{{ selectedReport.modality || 'N/A' }}</el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="selectedReport.status === 'Final' ? 'success' : 'warning'">
              {{ selectedReport.status }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <div v-if="selectedReport.findings" class="report-section">
          <h4>Findings</h4>
          <p>{{ selectedReport.findings }}</p>
        </div>

        <div v-if="selectedReport.impression" class="report-section">
          <h4>Impression</h4>
          <p>{{ selectedReport.impression }}</p>
        </div>

        <div v-if="selectedReport.recommendations" class="report-section">
          <h4>Recommendations</h4>
          <p>{{ selectedReport.recommendations }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="viewerVisible = false">Close</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import http from '../../utils/http'
import { Document, Calendar, User, View } from '@element-plus/icons-vue'

export default {
  name: 'MyReportsView',
  components: { Document, Calendar, User, View },
  setup() {
    const loading = ref(true)
    const reports = ref([])
    const viewerVisible = ref(false)
    const selectedReport = ref(null)

    const loadReports = async () => {
      loading.value = true
      try {
        const response = await http.get('/patient/reports')
        reports.value = response.data || []
      } catch (error) {
        console.error('Failed to load reports:', error)
        reports.value = []
      } finally {
        loading.value = false
      }
    }

    const viewReport = (report) => {
      selectedReport.value = report
      viewerVisible.value = true
    }

    onMounted(() => {
      loadReports()
    })

    return {
      loading,
      reports,
      viewerVisible,
      selectedReport,
      viewReport
    }
  }
}
</script>

<style scoped>
.my-reports { padding: 20px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-container { padding: 20px; }

.reports-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.report-card {
  cursor: pointer;
  transition: all 0.3s;
}

.report-card:hover {
  transform: translateY(-3px);
}

.report-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.report-icon-container {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  padding: 15px;
  border-radius: 12px;
  color: white;
}

.report-info { flex: 1; }

.report-info h3 {
  margin: 0 0 8px;
  color: #333;
  font-size: 16px;
}

.report-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 8px;
  color: #909399;
  font-size: 13px;
}

.report-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.report-tags {
  display: flex;
  gap: 8px;
}

.report-viewer {
  max-height: 60vh;
  overflow-y: auto;
}

.report-section {
  margin-bottom: 20px;
}

.report-section h4 {
  margin: 0 0 10px;
  color: #333;
}

.report-section p {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
  line-height: 1.6;
  white-space: pre-wrap;
  margin: 0;
}
</style>