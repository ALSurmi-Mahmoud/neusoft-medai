<template>
  <div class="patient-results-list">
    <el-table :data="patients" stripe @row-click="handleRowClick">
      <el-table-column type="index" width="60" />

      <el-table-column label="Patient" min-width="200">
        <template #default="{ row }">
          <div class="patient-info">
            <el-avatar :size="40" :icon="UserFilled" />
            <div class="patient-details">
              <div class="patient-name">{{ row.name }}</div>
              <div class="patient-id">ID: {{ row.patientId }}</div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="Gender" width="100">
        <template #default="{ row }">
          <el-tag :type="row.sex === 'M' ? 'primary' : 'danger'" size="small">
            {{ formatGender(row.sex) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="Birth Date" width="120">
        <template #default="{ row }">
          {{ formatDate(row.birthDate) }}
        </template>
      </el-table-column>

      <el-table-column label="Age" width="80">
        <template #default="{ row }">
          {{ calculateAge(row.birthDate) }}
        </template>
      </el-table-column>

      <el-table-column label="Blood Type" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.bloodType" type="info" size="small">
            {{ row.bloodType }}
          </el-tag>
          <span v-else class="text-secondary">N/A</span>
        </template>
      </el-table-column>

      <el-table-column label="Contact" min-width="180">
        <template #default="{ row }">
          <div class="contact-info">
            <div v-if="row.phone" class="contact-item">
              <el-icon><Phone /></el-icon>
              {{ row.phone }}
            </div>
            <div v-if="row.email" class="contact-item">
              <el-icon><Message /></el-icon>
              {{ row.email }}
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="City" width="120">
        <template #default="{ row }">
          {{ row.city || 'N/A' }}
        </template>
      </el-table-column>

      <el-table-column label="Actions" width="150" fixed="right">
        <template #default="{ row }">
          <el-space>
            <el-button
                size="small"
                type="primary"
                @click.stop="viewDetails(row.id)"
                :icon="View"
            >
              View
            </el-button>
            <el-dropdown @command="handleCommand($event, row)" trigger="click">
              <el-button size="small" :icon="More" circle />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="studies" :icon="FolderOpened">
                    View Studies
                  </el-dropdown-item>
                  <el-dropdown-item command="reports" :icon="Files">
                    View Reports
                  </el-dropdown-item>
                  <el-dropdown-item command="treatments" :icon="Checked">
                    Treatment Plans
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div v-if="showAll && patients.length > pageSize" class="pagination-container">
      <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="patients.length"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  UserFilled, View, More, Phone, Message, FolderOpened, Files, Checked
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const props = defineProps({
  patients: {
    type: Array,
    required: true,
    default: () => []
  },
  showAll: {
    type: Boolean,
    default: false
  }
})

const currentPage = ref(1)
const pageSize = ref(10)

const paginatedPatients = computed(() => {
  if (!props.showAll) return props.patients
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return props.patients.slice(start, end)
})

const formatGender = (sex) => {
  const genders = { M: 'Male', F: 'Female', O: 'Other' }
  return genders[sex] || sex
}

const formatDate = (date) => {
  if (!date) return 'N/A'
  return new Date(date).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const calculateAge = (birthDate) => {
  if (!birthDate) return 'N/A'
  const today = new Date()
  const birth = new Date(birthDate)
  let age = today.getFullYear() - birth.getFullYear()
  const monthDiff = today.getMonth() - birth.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--
  }
  return age
}

const handleRowClick = (row) => {
  viewDetails(row.id)
}

const viewDetails = (id) => {
  router.push(`/doctor/patients/${id}`)
}

const handleCommand = (command, row) => {
  switch (command) {
    case 'studies':
      router.push(`/studies?patientId=${row.id}`)
      break
    case 'reports':
      router.push(`/reports?patientId=${row.id}`)
      break
    case 'treatments':
      router.push(`/doctor/treatment-plans?patientId=${row.id}`)
      break
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
}
</script>

<style scoped>
.patient-results-list {
  width: 100%;
}

.patient-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.patient-details {
  flex: 1;
  min-width: 0;
}

.patient-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.patient-id {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
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

.el-table :deep(.el-table__row:hover) {
  background-color: var(--el-fill-color-light);
}
</style>