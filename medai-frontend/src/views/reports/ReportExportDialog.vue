  <template>
    <el-dialog
        v-model="dialogVisible"
        title="Export Report"
        width="500px"
        @close="handleClose"
    >
      <el-form :model="exportForm" label-width="120px">
        <el-form-item label="Format">
          <el-radio-group v-model="exportForm.format">
            <el-radio label="pdf">
              <el-icon><Document /></el-icon> PDF
            </el-radio>
            <el-radio label="docx">
              <el-icon><Edit /></el-icon> DOCX (Editable)
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Template">
          <el-select
              v-model="exportForm.templateId"
              placeholder="Select template"
              clearable
              style="width: 100%"
          >
            <el-option
                v-for="template in templates"
                :key="template.id"
                :label="template.name"
                :value="template.id"
            >
              <span>{{ template.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">
                {{ template.isSystem ? 'System' : 'Custom' }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="handleExport" :loading="exporting">
          <el-icon><Download /></el-icon>
          Export & Download
        </el-button>
      </template>
    </el-dialog>
  </template>

  <script>
  import { ref, reactive } from 'vue'
  import { Document, Edit, Download } from '@element-plus/icons-vue'
  import { ElMessage } from 'element-plus'
  import http from '../../utils/http'

  export default {
    name: 'ReportExportDialog',
    components: { Document, Edit, Download },
    props: {
      reportId: Number,
      reportType: {
        type: String,
        default: 'report' // report, treatment-plan, clinical-note
      },
      category: {
        type: String,
        default: 'radiology'
      }
    },
    emits: ['exported'],
    setup(props, { emit }) {
      const dialogVisible = ref(false)
      const exporting = ref(false)
      const templates = ref([])

      const exportForm = reactive({
        format: 'pdf',
        templateId: null
      })

      const open = () => {
        dialogVisible.value = true
        loadTemplates()
      }

      const loadTemplates = async () => {
        try {
          const res = await http.get('/report-templates', {
            params: { category: props.category }
          })
          templates.value = res.data
        } catch (error) {
          console.error('Failed to load templates:', error)
        }
      }

      const handleExport = async () => {
        exporting.value = true

        try {
          let endpoint = ''
          if (props.reportType === 'report') {
            endpoint = `/export/report/${props.reportId}`
          } else if (props.reportType === 'treatment-plan') {
            endpoint = `/export/treatment-plan/${props.reportId}`
          } else if (props.reportType === 'clinical-note') {
            endpoint = `/export/clinical-note/${props.reportId}`
          }

          const res = await http.post(endpoint, null, {
            params: {
              format: exportForm.format,
              templateId: exportForm.templateId
            }
          })

          // Download file immediately
          const downloadRes = await http.get(`/export/download/${res.data.exportId}`, {
            responseType: 'blob'
          })

          const url = window.URL.createObjectURL(new Blob([downloadRes.data]))
          const link = document.createElement('a')
          link.href = url
          link.setAttribute('download', res.data.fileName)
          document.body.appendChild(link)
          link.click()
          link.remove()

          ElMessage.success('Report exported successfully!')
          emit('exported')
          dialogVisible.value = false

        } catch (error) {
          console.error('Export failed:', error)
          ElMessage.error(error.response?.data?.message || 'Export failed')
        } finally {
          exporting.value = false
        }
      }

      const handleClose = () => {
        exportForm.format = 'pdf'
        exportForm.templateId = null
      }

      return {
        dialogVisible,
        exporting,
        templates,
        exportForm,
        open,
        handleExport,
        handleClose
      }
    }
  }
  </script>

  <style scoped>
  .el-radio-group {
    width: 100%;
  }

  .el-radio {
    width: 48%;
    margin-right: 4%;
    margin-bottom: 10px;
    padding: 10px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
  }

  .el-radio:nth-child(2n) {
    margin-right: 0;
  }

  .el-radio.is-checked {
    border-color: #409eff;
    background-color: #ecf5ff;
  }
  </style>