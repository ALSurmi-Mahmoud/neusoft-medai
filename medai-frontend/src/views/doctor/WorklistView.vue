<template>
  <div class="worklist-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>My Worklist</span>
          <div class="header-actions">
            <el-button @click="loadWorklist" :loading="loading">
              <el-icon><Refresh /></el-icon> Refresh
            </el-button>
          </div>
        </div>
      </template>

      <!-- Stats -->
      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :span="6">
          <el-statistic title="Total Items" :value="stats.total" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="Pending Review" :value="stats.pending">
            <template #suffix>
              <el-tag type="warning" size="small">pending</el-tag>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="In Progress" :value="stats.inProgress" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="Completed" :value="stats.completed">
            <template #suffix>
              <el-tag type="success" size="small">done</el-tag>
            </template>
          </el-statistic>
        </el-col>
      </el-row>

      <!-- Filters -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="Status">
          <el-select v-model="filters.status" placeholder="All" clearable style="width: 150px;">
            <el-option label="All" value="" />
            <el-option label="Pending" value="pending" />
            <el-option label="Uploaded" value="uploaded" />
            <el-option label="Completed" value="completed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadWorklist">Filter</el-button>
        </el-form-item>
      </el-form>

      <!-- Worklist Table -->
      <el-table
          :data="worklistItems"
          v-loading="loading"
          style="width: 100%"
          @row-click="viewStudy"
          row-class-name="clickable-row"
      >
        <el-table-column prop="priority" label="Priority" width="100">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small" effect="dark">
              {{ row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="patientName" label="Patient" width="150" />
        <el-table-column prop="modality" label="Modality" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.modality || 'N/A' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="Description" show-overflow-tooltip />
        <el-table-column prop="taskType" label="Task" width="180" />
        <el-table-column prop="studyDate" label="Date" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.studyDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="viewStudy(row)">
              <el-icon><View /></el-icon> View
            </el-button>
            <el-button size="small" type="success" @click.stop="createReport(row)">
              <el-icon><Document /></el-icon> Report
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Empty State -->
      <el-empty v-if="!loading && worklistItems.length === 0" description="No items in worklist">
        <el-button type="primary" @click="loadWorklist">Refresh</el-button>
      </el-empty>

      <!-- Pagination -->
      <el-pagination
          v-if="pagination.total > 0"
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadWorklist"
          @current-change="loadWorklist"
          style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { Refresh, View, Document } from '@element-plus/icons-vue'

export default {
  name: 'WorklistView',
  components: { Refresh, View, Document },
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const worklistItems = ref([])

    const filters = reactive({
      status: ''
    })

    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })

    const stats = reactive({
      total: 0,
      pending: 0,
      inProgress: 0,
      completed: 0
    })

    const loadWorklist = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.page - 1,
          size: pagination.size
        }
        if (filters.status) {
          params.status = filters.status
        }

        const response = await http.get('/worklist', { params })
        worklistItems.value = response.data.content || []
        pagination.total = response.data.totalElements || 0
      } catch (error) {
        console.error('Failed to load worklist:', error)
        ElMessage.error('Failed to load worklist')
        worklistItems.value = []
      } finally {
        loading.value = false
      }
    }

    const loadStats = async () => {
      try {
        const response = await http.get('/worklist/stats')
        stats.total = response.data.total || 0
        stats.pending = response.data.pending || 0
        stats.inProgress = response.data.inProgress || 0
        stats.completed = response.data.completed || 0
      } catch (error) {
        console.error('Failed to load stats:', error)
      }
    }

    const viewStudy = (row) => {
      router.push(`/studies/${row.id}`)
    }

    const createReport = (row) => {
      router.push(`/reports/new/${row.id}`)
    }

    const formatDateTime = (dateStr) => {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleString()
    }

    const getPriorityType = (priority) => {
      const types = { urgent: 'danger', high: 'warning', normal: 'info', low: '' }
      return types[priority] || 'info'
    }

    const getStatusType = (status) => {
      const types = {
        'uploaded': 'success',
        'pending': 'warning',
        'processing': 'primary',
        'completed': 'success'
      }
      return types[status] || 'info'
    }

    onMounted(() => {
      loadWorklist()
      loadStats()
    })

    return {
      loading, worklistItems, filters, pagination, stats,
      loadWorklist, viewStudy, createReport, formatDateTime, getPriorityType, getStatusType
    }
  }
}
</script>

<style scoped>
.worklist-view { padding: 20px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-form {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: #ecf5ff !important;
}
</style>