<template>
  <div class="report-results-list">
    <el-table :data="displayedReports" stripe @row-click="handleRowClick">
      <el-table-column type="index" width="60" />

      <el-table-column label="Title" min-width="250">
        <template #default="{ row }">
          <div class="report-title-cell">
            <el-icon><Files /></el-icon>
            <span>{{ row.title || 'Untitled Report' }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="Patient" width="180">
        <template #default="{ row }">
          <div v-if="row.patientName">
            {{ row.patientName }}
          </div>
          <span v-else class="text-secondary">N/A</span>
        </template>
      </el-table-column>

      <el-table-column label="Author" width="150">
        <template #default="{ row }">
          {{ row.authorName || 'N/A' }}
        </template>
      </el-table-column>

      <el-table-column label="Status" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusColor(row.status)" size="small">
            {{ formatStatus(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="Finalized" width="100">
        <template #default="{ row }">
          <el-icon v-if="row.finalized" color="green" :size="18">
            <CircleCheck />
          </el-icon>
          <el-icon v-else color="grey" :size="18">
            <CircleClose />
          </el-icon>
        </template>
      </el-table-column>

      <el-table-column label="Summary" min-width="200">
        <template #default="{ row }">
          <el-text :line-clamp="2" class="summary-text">
            {{ row.summary || 'No summary available' }}
          </el-text>
        </template>
      </el-table-column>

      <el-table-column label="Created" width="140">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>

      <el-table-column label="Actions" width="180" fixed="right">
        <template #default="{ row }">
          <el-space>
            <el-button
                size="small"
                type="primary"
                @click.stop="viewReport(row.id)"
                :icon="View"
            >
              View
            </el-button>
            <el-button
                v-if="row.finalized"
                size="small"
                @click.stop="exportReport(row.id)"
                :icon="Download"
                circle
            />
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="showAll && reports.length > pageSize" class="pagination-container">
      <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="reports.length"
          layout="total, prev, pager, next"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Files, View, Download, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '@/utils/http'

const router = useRouter()

const props = defineProps({
  reports: {
    type: Array,
    required: true
  },
  showAll: Boolean
})

const currentPage = ref(1)
const pageSize = ref(10)

const displayedReports = computed(() => {
  if (!props.showAll) return props.reports
  const start = (currentPage.value - 1) * pageSize.value
  return props.reports.slice(start, start + pageSize.value)
})

const getStatusColor = (status) => {
  const colors = {
    'draft': 'info',
    'finalized': 'success',
    'amended': 'warning'
  }
  return colors[status] || 'info'
}

const formatStatus = (status) => {
  return status ? status.charAt(0).toUpperCase() + status.slice(1) : 'N/A'
}

const formatDateTime = (date) => {
  if (!date) return 'N/A'
  return new Date(date).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleRowClick = (row) => {
  viewReport(row.id)
}

const viewReport = (id) => {
  router.push(`/reports/${id}`)
}

const exportReport = async (id) => {
  try {
    const response = await http.get(`/reports/${id}/export/pdf`, {
      responseType: 'blob'
    })

    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `report_${id}.pdf`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('Report exported successfully')
  } catch (error) {
    console.error('Export failed:', error)
    ElMessage.error('Failed to export report')
  }
}
</script>

<style scoped>
.report-results-list {
  width: 100%;
}

.report-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-text {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.text-secondary {
  color: var(--el-text-color-secondary);
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.el-table :deep(.el-table__row) {
  cursor: pointer;
}
</style>