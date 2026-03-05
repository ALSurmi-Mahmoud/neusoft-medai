<template>
  <div class="system-logs">
    <el-card>
      <template #header>
        <div class="header">
          <span>System Activity Logs</span>
          <el-button @click="loadLogs">
            <el-icon><Refresh /></el-icon>
            Refresh
          </el-button>
        </div>
      </template>

      <!-- Filters -->
      <el-form :inline="true">
        <el-form-item label="Activity Type">
          <el-select v-model="activityType" placeholder="All Types" clearable style="width: 200px">
            <el-option label="User Created" value="USER_CREATED" />
            <el-option label="User Updated" value="USER_UPDATED" />
            <el-option label="Settings Update" value="SETTINGS_UPDATE" />
            <el-option label="Password Reset" value="PASSWORD_RESET" />
            <el-option label="Login" value="LOGIN" />
            <el-option label="Logout" value="LOGOUT" />
          </el-select>
        </el-form-item>
        <el-form-item label="Days">
          <el-select v-model="days" style="width: 150px">
            <el-option label="Last 7 days" :value="7" />
            <el-option label="Last 30 days" :value="30" />
            <el-option label="Last 90 days" :value="90" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">Filter</el-button>
        </el-form-item>
      </el-form>

      <!-- Logs Table -->
      <el-table :data="logs" v-loading="loading" max-height="600">
        <el-table-column prop="activityType" label="Type" width="180" />
        <el-table-column prop="description" label="Description" />
        <el-table-column prop="performedBy" label="User" width="150" />
        <el-table-column prop="ipAddress" label="IP Address" width="150" />
        <el-table-column prop="createdAt" label="Time" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadLogs"
          style="margin-top: 20px"
      />
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '@/utils/http'

export default {
  name: 'SystemLogsView',
  components: { Refresh },
  setup() {
    const loading = ref(false)
    const logs = ref([])
    const currentPage = ref(1)
    const pageSize = ref(20)
    const total = ref(0)
    const activityType = ref('')
    const days = ref(7)

    const loadLogs = async () => {
      loading.value = true
      try {
        let url = '/admin/activity-logs'
        const params = {
          page: currentPage.value - 1,
          size: pageSize.value
        }

        if (activityType.value) {
          url = `/admin/activity-logs/by-type/${activityType.value}`
        }

        const res = await http.get(url, { params })

        // Handle both paginated and non-paginated responses
        if (res.data.content) {
          logs.value = res.data.content
          total.value = res.data.totalElements
        } else if (Array.isArray(res.data)) {
          logs.value = res.data
          total.value = res.data.length
        } else {
          logs.value = []
          total.value = 0
        }
      } catch (error) {
        console.error('Failed to load logs:', error)
        ElMessage.error('Failed to load activity logs')
        logs.value = []
        total.value = 0
      } finally {
        loading.value = false
      }
    }

    const formatDateTime = (dateTime) => {
      if (!dateTime) return ''
      return new Date(dateTime).toLocaleString()
    }

    const getStatusType = (status) => {
      const types = {
        success: 'success',
        failure: 'danger',
        warning: 'warning'
      }
      return types[status?.toLowerCase()] || 'info'
    }

    onMounted(() => {
      loadLogs()
    })

    return {
      loading,
      logs,
      currentPage,
      pageSize,
      total,
      activityType,
      days,
      loadLogs,
      formatDateTime,
      getStatusType
    }
  }
}
</script>

<style scoped>
.system-logs {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>