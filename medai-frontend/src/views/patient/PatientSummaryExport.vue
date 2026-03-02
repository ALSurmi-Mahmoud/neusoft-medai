<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>Export Patient Summary</span>
      </div>
    </template>

    <el-form :model="form" label-width="120px">
      <el-form-item label="Export Format">
        <el-radio-group v-model="form.format">
          <el-radio label="pdf">PDF</el-radio>
          <el-radio label="docx">DOCX (Editable)</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="Include">
        <el-checkbox-group v-model="form.sections">
          <el-checkbox label="demographics">Demographics</el-checkbox>
          <el-checkbox label="medical_history">Medical History</el-checkbox>
          <el-checkbox label="medications">Current Medications</el-checkbox>
          <el-checkbox label="allergies">Allergies</el-checkbox>
          <el-checkbox label="insurance">Insurance Info</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>

    <el-button type="primary" @click="handleExport" :loading="exporting">
      <el-icon><Download /></el-icon>
      Export Patient Summary
    </el-button>
  </el-card>
</template>

<script>
import { ref, reactive } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../../utils/http'

export default {
  name: 'PatientSummaryExport',
  components: { Download },
  props: {
    patientId: {
      type: Number,
      required: true
    }
  },
  setup(props) {
    const exporting = ref(false)

    const form = reactive({
      format: 'pdf',
      sections: ['demographics', 'medical_history', 'medications', 'allergies', 'insurance']
    })

    const handleExport = async () => {
      exporting.value = true

      try {
        const res = await http.post(`/export/patient-summary/${props.patientId}`, null, {
          params: { format: form.format }
        })

        // Download immediately
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

        ElMessage.success('Patient summary exported successfully!')

      } catch (error) {
        console.error('Export failed:', error)
        ElMessage.error(error.response?.data?.message || 'Export failed')
      } finally {
        exporting.value = false
      }
    }

    return {
      exporting,
      form,
      handleExport
    }
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>