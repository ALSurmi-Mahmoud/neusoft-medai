<template>
  <div class="study-results-list">
    <el-table :data="displayedStudies" stripe @row-click="handleRowClick">
      <el-table-column type="index" width="60" />

      <el-table-column label="Accession Number" width="180">
        <template #default="{ row }">
          <el-text type="primary" class="accession-number">
            {{ row.accessionNumber }}
          </el-text>
        </template>
      </el-table-column>

      <el-table-column label="Modality" width="100">
        <template #default="{ row }">
          <el-tag :type="getModalityColor(row.modality)">
            {{ row.modality }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="Description" min-width="250">
        <template #default="{ row }">
          {{ row.description || 'No description' }}
        </template>
      </el-table-column>

      <el-table-column label="Patient" width="180">
        <template #default="{ row }">
          <div v-if="row.patientName">
            <div class="patient-name">{{ row.patientName }}</div>
            <div class="patient-id-small">{{ row.patientId }}</div>
          </div>
          <span v-else class="text-secondary">N/A</span>
        </template>
      </el-table-column>

      <el-table-column label="Study Date" width="140">
        <template #default="{ row }">
          {{ formatDateTime(row.studyDate) }}
        </template>
      </el-table-column>

      <el-table-column label="Status" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusColor(row.status)" size="small">
            {{ formatStatus(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="Actions" width="150" fixed="right">
        <template #default="{ row }">
          <el-space>
            <el-button
                size="small"
                type="primary"
                @click.stop="viewStudy(row.id)"
                :icon="View"
            >
              View
            </el-button>
            <el-button
                size="small"
                @click.stop="viewImages(row.id)"
                :icon="Picture"
                circle
            />
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="showAll && studies.length > pageSize" class="pagination-container">
      <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="studies.length"
          layout="total, prev, pager, next"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { View, Picture } from '@element-plus/icons-vue'

const router = useRouter()

const props = defineProps({
  studies: {
    type: Array,
    required: true
  },
  showAll: Boolean
})

const currentPage = ref(1)
const pageSize = ref(10)

const displayedStudies = computed(() => {
  if (!props.showAll) return props.studies
  const start = (currentPage.value - 1) * pageSize.value
  return props.studies.slice(start, start + pageSize.value)
})

const getModalityColor = (modality) => {
  const colors = {
    'CT': 'primary',
    'MR': 'success',
    'XR': 'info',
    'US': 'warning'
  }
  return colors[modality] || ''
}

const getStatusColor = (status) => {
  const colors = {
    'completed': 'success',
    'in_progress': 'warning',
    'pending': 'info',
    'cancelled': 'danger'
  }
  return colors[status] || 'info'
}

const formatStatus = (status) => {
  return status ? status.replace('_', ' ').toUpperCase() : 'N/A'
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
  viewStudy(row.id)
}

const viewStudy = (id) => {
  router.push(`/studies/${id}`)
}

const viewImages = (id) => {
  router.push(`/viewer/${id}`)
}
</script>

<style scoped>
.study-results-list {
  width: 100%;
}

.accession-number {
  font-family: monospace;
  font-weight: 500;
}

.patient-name {
  font-weight: 500;
  font-size: 13px;
}

.patient-id-small {
  font-size: 11px;
  color: var(--el-text-color-secondary);
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