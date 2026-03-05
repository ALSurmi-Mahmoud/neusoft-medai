<template>
  <div class="treatment-results-list">
    <el-table :data="displayedTreatments" stripe @row-click="handleRowClick">
      <el-table-column type="index" width="60" />

      <el-table-column label="Title" min-width="250">
        <template #default="{ row }">
          <div class="treatment-title-cell">
            <el-icon><Checked /></el-icon>
            <span>{{ row.title }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="Patient" width="180">
        <template #default="{ row }">
          {{ row.patientName || 'N/A' }}
        </template>
      </el-table-column>

      <el-table-column label="Doctor" width="150">
        <template #default="{ row }">
          {{ row.doctorName || 'N/A' }}
        </template>
      </el-table-column>

      <el-table-column label="Status" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusColor(row.status)" size="small">
            {{ formatStatus(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="Priority" width="100">
        <template #default="{ row }">
          <el-tag :type="getPriorityColor(row.priority)" size="small">
            {{ row.priority ? row.priority.toUpperCase() : 'N/A' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="Progress" width="150">
        <template #default="{ row }">
          <div class="progress-cell">
            <el-progress
                :percentage="row.progressPercentage || 0"
                :color="getProgressColor(row.progressPercentage)"
                :stroke-width="8"
            />
          </div>
        </template>
      </el-table-column>

      <el-table-column label="Diagnosis" min-width="200">
        <template #default="{ row }">
          <el-text :line-clamp="2">
            {{ row.diagnosis || 'No diagnosis provided' }}
          </el-text>
        </template>
      </el-table-column>

      <el-table-column label="Start Date" width="120">
        <template #default="{ row }">
          {{ formatDate(row.startDate) }}
        </template>
      </el-table-column>

      <el-table-column label="Actions" width="150" fixed="right">
        <template #default="{ row }">
          <el-space>
            <el-button
                size="small"
                type="primary"
                @click.stop="viewPlan(row.id)"
                :icon="View"
            >
              View
            </el-button>
            <el-dropdown @command="handleCommand($event, row)" trigger="click">
              <el-button size="small" :icon="More" circle />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" :icon="Edit">
                    Edit Plan
                  </el-dropdown-item>
                  <el-dropdown-item command="steps" :icon="List">
                    View Steps
                  </el-dropdown-item>
                  <el-dropdown-item command="patient" :icon="User">
                    View Patient
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="showAll && treatments.length > pageSize" class="pagination-container">
      <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="treatments.length"
          layout="total, prev, pager, next"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Checked, View, More, Edit, List, User } from '@element-plus/icons-vue'

const router = useRouter()

const props = defineProps({
  treatments: {
    type: Array,
    required: true
  },
  showAll: Boolean
})

const currentPage = ref(1)
const pageSize = ref(10)

const displayedTreatments = computed(() => {
  if (!props.showAll) return props.treatments
  const start = (currentPage.value - 1) * pageSize.value
  return props.treatments.slice(start, start + pageSize.value)
})

const getStatusColor = (status) => {
  const colors = {
    'active': 'success',
    'completed': 'info',
    'cancelled': 'danger',
    'on_hold': 'warning'
  }
  return colors[status] || 'info'
}

const getPriorityColor = (priority) => {
  const colors = {
    'urgent': 'danger',
    'high': 'warning',
    'medium': 'primary',
    'low': 'info'
  }
  return colors[priority] || 'info'
}

const getProgressColor = (progress) => {
  if (progress >= 80) return '#67c23a'
  if (progress >= 50) return '#409eff'
  if (progress >= 30) return '#e6a23c'
  return '#f56c6c'
}

const formatStatus = (status) => {
  return status ? status.replace('_', ' ').toUpperCase() : 'N/A'
}

const formatDate = (date) => {
  if (!date) return 'N/A'
  return new Date(date).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const handleRowClick = (row) => {
  viewPlan(row.id)
}

const viewPlan = (id) => {
  router.push(`/doctor/treatment-plans/${id}`)
}

const handleCommand = (command, row) => {
  switch (command) {
    case 'edit':
      router.push(`/doctor/treatment-plans/${row.id}/edit`)
      break
    case 'steps':
      router.push(`/doctor/treatment-plans/${row.id}/steps`)
      break
    case 'patient':
      if (row.patientId) {
        router.push(`/doctor/patients/${row.patientId}`)
      }
      break
  }
}
</script>

<style scoped>
.treatment-results-list {
  width: 100%;
}

.treatment-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-cell {
  padding: 5px 0;
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