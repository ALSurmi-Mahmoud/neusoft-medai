<template>
  <div class="exports-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>My Exports</span>
          <el-button type="primary" @click="loadExports">
            <el-icon><Refresh /></el-icon>
            Refresh
          </el-button>
        </div>
      </template>

      <el-table :data="exports" v-loading="loading">
        <el-table-column prop="title" label="Title" min-width="250" />
        <el-table-column label="Type" width="150">
          <template #default="{ row }">
            <el-tag size="small">{{ formatType(row.exportType) }}</el-tag>
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
            <el-tag type="info">{{ row.downloadCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
                size="small"
                type="primary"
                @click="downloadExport(row.id)"
            >
              <el-icon><Download /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          v-model:current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="loadExports"
          style="margin-top: 20px; justify-content: center"
      />
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { Download, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../../utils/http'

export default {
  name: 'ExportsView',
  components: { Download, Refresh },
  setup() {
    const loading = ref(false)
    const exports = ref([])

    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })

    const loadExports = async () => {
      loading.value = true
      try {
        const res = await http.get('/export/history')
        exports.value = res.data
        pagination.total = res.data.length
      } catch (error) {
        console.error('Failed to load exports:', error)
        ElMessage.error('Failed to load exports')
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

    onMounted(() => {
      loadExports()
    })

    return {
      loading,
      exports,
      pagination,
      loadExports,
      downloadExport,
      formatType,
      formatFileSize,
      formatDate
    }
  }
}
</script>

<style scoped>
.exports-view {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>