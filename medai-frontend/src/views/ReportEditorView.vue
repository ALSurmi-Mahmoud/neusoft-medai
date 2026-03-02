<template>
  <div class="report-editor">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <img
                v-if="exportConfig.logoUrl"
                class="hospital-logo"
                :src="exportConfig.logoUrl"
                alt="Hospital Logo"
                @error="onLogoError"
            />
            <div class="header-titles">
              <div class="hospital-name">
                {{ exportConfig.hospitalName }}
              </div>
              <div class="page-title">
                {{ isEditing ? 'Edit Report' : 'Create Diagnostic Report' }}
              </div>
            </div>
          </div>

          <div class="header-actions">
            <el-dropdown @command="handleQuickExport" v-if="!loading && report.id" style="margin-right: 10px;">
              <el-button type="success">
                <el-icon><Download /></el-icon>
                Quick Export
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="pdf">Export as PDF</el-dropdown-item>
                  <el-dropdown-item command="docx">Export as DOCX</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

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
        <el-card class="study-info-card" shadow="never">
          <template #header>Study & Patient Information</template>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="info-item">
                <label>Study UID:</label>
                <span>{{ studyInfo.studyUid || (patientInfoFromQuery.patientName ? 'General Report' : '-') }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <label>Patient ID:</label>
                <span>{{ studyInfo.patient?.patientId || patientInfoFromQuery.patientId || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <label>Patient Name:</label>
                <span>{{ studyInfo.patient?.name || patientInfoFromQuery.patientName || 'N/A (requires DICOM parsing)' }}</span>
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
import { MagicStick, Check, Download, ArrowDown } from '@element-plus/icons-vue'

export default {
  name: 'ReportEditorView',
  components: { MagicStick, Check, Download, ArrowDown },
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

    const exportConfig = reactive({
      hospitalName: 'Medical Imaging Center',
      logoUrl: ''
    })

    const patientInfoFromQuery = reactive({
      patientId: route.query.patientId || null,
      patientName: route.query.patientName || null
    })

    const isEditing = computed(() => !!route.params.id)

    const rules = {
      title: [{ required: true, message: 'Please enter a title', trigger: 'blur' }],
      findings: [{ required: true, message: 'Please enter findings', trigger: 'blur' }]
    }

    const loadExportConfig = async () => {
      try {
        const res = await http.get('/config/export')
        const data = res?.data || {}
        exportConfig.hospitalName = data.hospitalName || exportConfig.hospitalName
        exportConfig.logoUrl = data.logoUrl || exportConfig.logoUrl
      } catch (e) {
        // Config endpoint not available, keep defaults
      }
    }

    const onLogoError = () => {
      exportConfig.logoUrl = ''
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

        if (data.studyId) {
          await loadStudy(data.studyId)
        } else {
          studyInfo.value = {}
        }
      } catch (error) {
        console.error('Failed to load report:', error)
        ElMessage.error('Failed to load report')
      } finally {
        loading.value = false
      }
    }

    const handleQuickExport = async (format) => {
      try {
        if (!report.id) {
          ElMessage.warning('Please save the report first before exporting')
          return
        }

        ElMessage.info(`Exporting report as ${format.toUpperCase()}...`)

        const response = await http.post(
            `/reports/${report.id}/export`,
            null,
            {
              params: { format },
              responseType: 'blob'
            }
        )

        const blob = new Blob([response.data])
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url

        const timestamp = new Date().toISOString()
            .replace(/[:.]/g, '-')
            .slice(0, 19)
            .replace('T', '_')
        const fileName = `Report_${report.id}_${timestamp}.${format}`

        link.setAttribute('download', fileName)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        ElMessage.success(`Report exported as ${format.toUpperCase()}`)

      } catch (error) {
        console.error('Quick export failed:', error)
        ElMessage.error(error.response?.data?.message || 'Export failed')
      }
    }

    const saveDraft = async () => {
      saving.value = true
      try {
        if (isEditing.value) {
          await http.put(`/reports/${report.id}`, {
            title: report.title,
            summary: report.summary,
            findings: report.findings,
            impression: report.impression,
            recommendations: report.recommendations,
            status: 'draft'
          })
          ElMessage.success('Report saved')
        } else {
          const patientIdFromQuery = route.query.patientId

          let endpoint = '/reports'
          let payload = {
            studyId: report.studyId,
            title: report.title,
            summary: report.summary,
            findings: report.findings,
            impression: report.impression,
            recommendations: report.recommendations
          }

          if (patientIdFromQuery && !report.studyId) {
            endpoint = `/reports/patient/${patientIdFromQuery}`
            payload = {
              title: report.title,
              summary: report.summary,
              findings: report.findings,
              impression: report.impression,
              recommendations: report.recommendations
            }
          }

          const response = await http.post(endpoint, payload)
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
        if (!report.id) {
          await saveDraft()
        }

        await http.post(`/reports/${report.id}/finalize`)
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
      const types = { draft: 'info', finalized: 'success', amended: 'warning' }
      return types[status] || 'info'
    }

    onMounted(async () => {
      await loadExportConfig()

      const studyId = route.params.studyId || route.query.studyId

      if (studyId) {
        const studyIdNum = parseInt(studyId)
        if (!isNaN(studyIdNum)) {
          report.studyId = studyIdNum
          await loadStudy(studyId)
        }
      }

      const reportId = route.params.id
      if (reportId && reportId !== 'new') {
        await loadReport(reportId)
      }

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
      patientInfoFromQuery,
      exportConfig,
      isEditing,
      rules,
      saveDraft,
      confirmFinalize,
      generateAIReport,
      handleQuickExport,
      getStatusType,
      onLogoError
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
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hospital-logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #ebeef5;
}

.header-titles {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.hospital-name {
  font-size: 12px;
  color: #909399;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 520px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 520px;
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