<template>
  <div class="report-editor">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEditing ? 'Edit Report' : 'Create Diagnostic Report' }}</span>
          <div class="header-actions">
            <el-tag v-if="report.status" :type="getStatusType(report.status)">
              {{ report.status }}
            </el-tag>
          </div>
        </div>
      </template>

      <el-form
          ref="formRef"
          :model="report"
          :rules="rules"
          label-width="140px"
          v-loading="loading"
      >
        <!-- Study Info (Read-only) -->
        <el-card class="study-info-card" shadow="never">
          <template #header>Study & Patient Information</template>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="info-item">
                <label>Study UID:</label>
                <span>{{ studyInfo.studyUid || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <label>Patient ID:</label>
                <span>{{ studyInfo.patient?.patientId || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <label>Patient Name:</label>
                <span>{{ studyInfo.patient?.name || 'N/A (requires DICOM parsing)' }}</span>
              </div>
            </el-col>
          </el-row>
          <el-row :gutter="20" v-if="studyInfo.modality">
            <el-col :span="8">
              <div class="info-item">
                <label>Modality:</label>
                <span>{{ studyInfo.modality || '-' }}</span>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-divider />

        <!-- Report Content -->
        <el-form-item label="Report Title" prop="title">
          <el-input
              v-model="report.title"
              placeholder="Enter report title"
              :disabled="report.finalized"
          />
        </el-form-item>

        <el-form-item label="Summary" prop="summary">
          <el-input
              v-model="report.summary"
              type="textarea"
              :rows="3"
              placeholder="Brief summary of findings"
              :disabled="report.finalized"
          />
        </el-form-item>

        <el-form-item label="Findings" prop="findings">
          <el-input
              v-model="report.findings"
              type="textarea"
              :rows="6"
              placeholder="Detailed findings from the imaging study"
              :disabled="report.finalized"
          />
        </el-form-item>

        <el-form-item label="Impression" prop="impression">
          <el-input
              v-model="report.impression"
              type="textarea"
              :rows="4"
              placeholder="Clinical impression and diagnosis"
              :disabled="report.finalized"
          />
        </el-form-item>

        <el-form-item label="Recommendations" prop="recommendations">
          <el-input
              v-model="report.recommendations"
              type="textarea"
              :rows="3"
              placeholder="Follow-up recommendations"
              :disabled="report.finalized"
          />
        </el-form-item>

        <el-divider />

        <!-- AI Assistance Section -->
        <el-card class="ai-assist-card" shadow="never">
          <template #header>
            <div class="ai-header">
              <span>🤖 AI Assistance</span>
              <el-tag type="warning" size="small">Beta</el-tag>
            </div>
          </template>
          <p class="ai-description">
            Use AI to help generate report content based on the imaging study.
          </p>
          <el-button
              type="primary"
              @click="generateAIReport"
              :loading="generatingAI"
              :disabled="report.finalized"
          >
            <el-icon><MagicStick /></el-icon>
            Generate AI Suggestions
          </el-button>
          <el-alert
              v-if="aiSuggestion"
              :title="'AI Suggestion'"
              type="info"
              :description="aiSuggestion"
              show-icon
              :closable="true"
              style="margin-top: 15px;"
          />
        </el-card>

        <el-divider />

        <!-- Action Buttons -->
        <el-form-item>
          <div class="form-actions">
            <el-button @click="$router.back()">Cancel</el-button>
            <el-button
                type="info"
                @click="saveDraft"
                :loading="saving"
                :disabled="report.finalized"
            >
              Save Draft
            </el-button>
            <el-button
                type="success"
                @click="confirmFinalize"
                :loading="finalizing"
                :disabled="report.finalized"
            >
              <el-icon><Check /></el-icon>
              Finalize Report
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Check } from '@element-plus/icons-vue'

export default {
  name: 'ReportEditorView',
  components: { MagicStick, Check },
  setup() {
    const route = useRoute()
    const router = useRouter()

    const formRef = ref(null)
    const loading = ref(false)
    const saving = ref(false)
    const finalizing = ref(false)
    const generatingAI = ref(false)
    const aiSuggestion = ref('')

    const studyInfo = ref({})
    const report = reactive({
      id: null,
      studyId: null,
      title: '',
      summary: '',
      findings: '',
      impression: '',
      recommendations: '',
      status: 'draft',
      finalized: false
    })

    const isEditing = computed(() => !!route.params.id)

    const rules = {
      title: [{ required: true, message: 'Please enter a title', trigger: 'blur' }],
      findings: [{ required: true, message: 'Please enter findings', trigger: 'blur' }]
    }

    const loadStudy = async (studyId) => {
      try {
        const response = await http.get(`/studies/${studyId}`)
        studyInfo.value = response.data
      } catch (error) {
        console.error('Failed to load study:', error)
        ElMessage.error('Failed to load study information')
      }
    }

    const loadReport = async (reportId) => {
      loading.value = true
      try {
        const response = await http.get(`/reports/${reportId}`)
        const data = response.data

        Object.assign(report, {
          id: data.id,
          studyId: data.studyId,
          title: data.title || '',
          summary: data.summary || '',
          findings: data.findings || '',
          impression: data.impression || '',
          recommendations: data.recommendations || '',
          status: data.status,
          finalized: data.finalized
        })

        // Only load study if studyId exists (not null)
        if (data.studyId) {
          await loadStudy(data.studyId)
        } else {
          // General report without a study - clear study info
          studyInfo.value = {}
        }
      } catch (error) {
        console.error('Failed to load report:', error)
        ElMessage.error('Failed to load report')
      } finally {
        loading.value = false
      }
    }

    const saveDraft = async () => {
      saving.value = true
      try {
        // Get current user info
        const user = JSON.parse(localStorage.getItem('user') || '{}')

        if (isEditing.value) {
          await http.put(`/reports/${report.id}`, {
            title: report.title,
            summary: report.summary,
            findings: report.findings,
            impression: report.impression,
            recommendations: report.recommendations,
            status: 'draft'
          }, {
            params: { userId: user.id }  // ← ADD userId
          })
          ElMessage.success('Report saved')
        } else {
          const response = await http.post('/reports', {
            studyId: report.studyId,
            title: report.title,
            summary: report.summary,
            findings: report.findings,
            impression: report.impression,
            recommendations: report.recommendations
          }, {
            params: { authorId: user.id }  // ← ADD authorId
          })
          report.id = response.data.id
          ElMessage.success('Report created')
          router.replace(`/reports/${report.id}/edit`)
        }
      } catch (error) {
        console.error('Failed to save report:', error)
        ElMessage.error('Failed to save report')
      } finally {
        saving.value = false
      }
    }

    const confirmFinalize = () => {
      ElMessageBox.confirm(
          'Once finalized, the report cannot be edited. Are you sure?',
          'Finalize Report',
          {
            confirmButtonText: 'Finalize',
            cancelButtonText: 'Cancel',
            type: 'warning'
          }
      ).then(() => {
        finalizeReport()
      }).catch(() => {})
    }

    const finalizeReport = async () => {
      finalizing.value = true
      try {
        // Save first if needed
        if (!report.id) {
          await saveDraft()
        }

        const user = JSON.parse(localStorage.getItem('user') || '{}')
        await http.post(`/reports/${report.id}/finalize`, null, {
          params: { userId: user.id }
        })
        report.finalized = true
        report.status = 'finalized'
        ElMessage.success('Report finalized')
        router.push('/reports')
      } catch (error) {
        console.error('Failed to finalize report:', error)
        ElMessage.error('Failed to finalize report')
      } finally {
        finalizing.value = false
      }
    }

    const generateAIReport = async () => {
      generatingAI.value = true
      aiSuggestion.value = ''

      try {
        // TODO: Integrate with actual AI model
        // For now, generate placeholder suggestion
        await new Promise(resolve => setTimeout(resolve, 2000))

        aiSuggestion.value = `Based on the ${studyInfo.value.modality || 'imaging'} study analysis:

FINDINGS: The examination demonstrates normal anatomical structures without significant abnormality. No focal lesions, masses, or areas of abnormal enhancement identified.

IMPRESSION: Normal ${studyInfo.value.modality || 'imaging'} study. No acute findings.

RECOMMENDATIONS: Clinical correlation recommended. Follow-up as clinically indicated.

[Note: This is a placeholder AI suggestion. In production, this would be generated by an integrated AI model analyzing the actual imaging data.]`

        ElMessage.success('AI suggestions generated')
      } catch (error) {
        console.error('Failed to generate AI report:', error)
        ElMessage.error('Failed to generate AI suggestions')
      } finally {
        generatingAI.value = false
      }
    }

    const getStatusType = (status) => {
      const types = { 'draft': 'info', 'finalized': 'success', 'amended': 'warning' }
      return types[status] || 'info'
    }

    onMounted(async () => {
      // Get studyId from route params or query
      const studyId = route.params.studyId || route.query.studyId

      if (studyId) {
        const studyIdNum = parseInt(studyId)
        if (!isNaN(studyIdNum)) {
          report.studyId = studyIdNum
          await loadStudy(studyId) // keep studyId as-is for API path compatibility
        }
      }

      // Get reportId from route params
      const reportId = route.params.id
      if (reportId && reportId !== 'new') {
        await loadReport(reportId)
      }

      // If no studyId, show warning
      if (!report.studyId) {
        console.warn('No studyId provided for report creation')
      }
    })

    return {
      formRef,
      loading,
      saving,
      finalizing,
      generatingAI,
      aiSuggestion,
      studyInfo,
      report,
      isEditing,
      rules,
      saveDraft,
      confirmFinalize,
      generateAIReport,
      getStatusType
    }
  }
}
</script>

<style scoped>
.report-editor {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.study-info-card {
  background-color: #f5f7fa;
  margin-bottom: 20px;
}

.study-info-card :deep(.el-card__header) {
  padding: 10px 15px;
  font-weight: bold;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.info-item label {
  font-size: 12px;
  color: #909399;
}

.info-item span {
  font-size: 14px;
  color: #303133;
  font-family: monospace;
}

.ai-assist-card {
  background-color: #ecf5ff;
  border: 1px solid #b3d8ff;
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-description {
  margin-bottom: 15px;
  color: #606266;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  width: 100%;
}
</style>