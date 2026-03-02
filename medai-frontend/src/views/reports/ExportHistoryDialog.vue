<template>
  <el-dialog
      v-model="dialogVisible"
      title="Export History"
      width="800px"
  >
    <el-table :data="exports" v-loading="loading">
      <el-table-column prop="title" label="Title" min-width="200" />
      <el-table-column label="Type" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="getTypeColor(row.exportType)">
            {{ formatType(row.exportType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Format" width="80">
        <template #default="{ row }">
          <el-tag size="small">{{ row.format.toUpperCase() }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Size" width="100">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="Exported" width="180">
        <template #default="{ row }">
          {{ formatDate(row.exportedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="Downloads" width="100">
        <template #default="{ row }">
          {{ row.downloadCount }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="downloadExport(row.id)">
            <el-icon><Download /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script>
import { ref } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../../utils/http'

export default {
  name: 'ExportHistoryDialog',
  components: { Download },
  setup() {
    const dialogVisible = ref(false)
    const loading = ref(false)
    const exports = ref([])

    const open = async () => {
      dialogVisible.value = true
      await loadExports()
    }

    const loadExports = async () => {
      loading.value = true
      try {
        const res = await http.get('/export/history')
        exports.value = res.data
      } catch (error) {
        console.error('Failed to load exports:', error)
        ElMessage.error('Failed to load export history')
      } finally {
        loading.value = false
      }
    }

    const downloadExport = async (exportId) => {
      try {
        const res = await http.get(`/export/download/${exportId}`, {
          responseType: 'blob'
        })

        const export_ = exports.value.find(e => e.id === exportId)
        const url = window.URL.createObjectURL(new Blob([res.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', export_.fileName)
        document.body.appendChild(link)
        link.click()
        link.remove()

        ElMessage.success('Download started')

        // Reload to update download count
        await loadExports()

      } catch (error) {
        console.error('Download failed:', error)
        ElMessage.error('Download failed')
      }
    }

    const formatType = (type) => {
      const types = {
        'single_report': 'Report',
        'treatment_plan': 'Treatment Plan',
        'clinical_note': 'Clinical Note',
        'patient_record': 'Patient Summary'
      }
      return types[type] || type
    }

    const getTypeColor = (type) => {
      const colors = {
        'single_report': '',
        'treatment_plan': 'success',
        'clinical_note': 'warning',
        'patient_record': 'info'
      }
      return colors[type] || ''
    }

    const formatFileSize = (bytes) => {
      if (!bytes) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
    }

    const formatDate = (dateStr) => {
      return new Date(dateStr).toLocaleString()
    }

    return {
      dialogVisible,
      loading,
      exports,
      open,
      downloadExport,
      formatType,
      getTypeColor,
      formatFileSize,
      formatDate
    }
  }
}
</script>