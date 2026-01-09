<template>
  <div class="audit-log-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Audit Logs</span>
          <el-button type="success" @click="exportLogs" :loading="exporting">
            <el-icon><Download /></el-icon>
            Export CSV
          </el-button>
        </div>
      </template>

      <!-- Filters -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="Action">
          <el-select v-model="filters.action" placeholder="All Actions" clearable>
            <el-option label="LOGIN" value="LOGIN" />
            <el-option label="LOGOUT" value="LOGOUT" />
            <el-option label="CREATE" value="CREATE" />
            <el-option label="UPDATE" value="UPDATE" />
            <el-option label="DELETE" value="DELETE" />
            <el-option label="UPLOAD" value="UPLOAD" />
          </el-select>
        </el-form-item>
        <el-form-item label="Resource">
          <el-select v-model="filters.resourceType" placeholder="All Resources" clearable>
            <el-option label="USER" value="USER" />
            <el-option label="STUDY" value="STUDY" />
            <el-option label="REPORT" value="REPORT" />
            <el-option label="APPOINTMENT" value="APPOINTMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="Date Range">
          <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="to"
              start-placeholder="Start"
              end-placeholder="End"
              value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">
            <el-icon><Search /></el-icon> Search
          </el-button>
          <el-button @click="resetFilters">Reset</el-button>
        </el-form-item>
      </el-form>

      <!-- Logs Table -->
      <el-table :data="logs" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="createdAt" label="Timestamp" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="action" label="Action" width="120">
          <template #default="{ row }">
            <el-tag :type="getActionType(row.action)" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resourceType" label="Resource" width="120" />
        <el-table-column prop="resourceId" label="Resource ID" width="120" />
        <el-table-column prop="username" label="User" width="150" />
        <el-table-column prop="ipAddress" label="IP Address" width="140" />
      </el-table>

      <!-- Pagination -->
      <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadLogs"
          @current-change="loadLogs"
          style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import http from '../utils/http'
import { ElMessage } from 'element-plus'
import { Download, Search } from '@element-plus/icons-vue'

export default {
  name: 'AuditLogView',
  components: { Download, Search },
  setup() {
    const loading = ref(false)
    const exporting = ref(false)
    const logs = ref([])

    const filters = reactive({
      action: '',
      resourceType: '',
      dateRange: null
    })

    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })

    const loadLogs = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.page - 1,
          size: pagination.size
        }
        if (filters.action) params.action = filters.action
        if (filters.resourceType) params.resourceType = filters.resourceType
        if (filters.dateRange && filters.dateRange.length === 2) {
          params.startDate = filters.dateRange[0]
          params.endDate = filters.dateRange[1]
        }

        const response = await http.get('/audit', { params })
        logs.value = response.data.content || []
        pagination.total = response.data.totalElements || 0
      } catch (error) {
        console.error('Failed to load audit logs:', error)
        ElMessage.error('Failed to load audit logs')
      } finally {
        loading.value = false
      }
    }

    const exportLogs = async () => {
      exporting.value = true
      try {
        const response = await http.get('/admin/audit/export', {
          responseType: 'blob'
        })

        // Create download link
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `audit_logs_${new Date().toISOString().split('T')[0]}.csv`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        ElMessage.success('Audit logs exported successfully')
      } catch (error) {
        console.error('Failed to export logs:', error)
        ElMessage.error('Failed to export audit logs')
      } finally {
        exporting.value = false
      }
    }

    const resetFilters = () => {
      filters.action = ''
      filters.resourceType = ''
      filters.dateRange = null
      pagination.page = 1
      loadLogs()
    }

    const formatDateTime = (dateStr) => {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleString()
    }

    const getActionType = (action) => {
      const types = {
        'LOGIN': 'success',
        'LOGOUT': 'info',
        'CREATE': 'primary',
        'UPDATE': 'warning',
        'DELETE': 'danger',
        'UPLOAD': 'success'
      }
      return types[action] || 'info'
    }

    onMounted(() => {
      loadLogs()
    })

    return {
      loading,
      exporting,
      logs,
      filters,
      pagination,
      loadLogs,
      exportLogs,
      resetFilters,
      formatDateTime,
      getActionType
    }
  }
}
</script>

<style scoped>
.audit-log-view {
  padding: 20px;
}

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
</style>