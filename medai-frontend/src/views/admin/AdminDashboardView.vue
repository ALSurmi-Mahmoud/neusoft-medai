<template>
  <div class="admin-dashboard">
    <el-card>
      <template #header>
        <span>System Administration Dashboard</span>
      </template>

      <!-- Quick Stats -->
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover">
            <el-statistic title="Total Users" :value="stats.totalUsers">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover">
            <el-statistic title="Active Sessions" :value="stats.activeSessions">
              <template #prefix>
                <el-icon><Connection /></el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover">
            <el-statistic title="System Health" value="Good">
              <template #prefix>
                <el-icon style="color: #67C23A"><CircleCheck /></el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover">
            <el-statistic title="Storage Used" :value="stats.storageUsed" suffix="GB">
              <template #prefix>
                <el-icon><Folder /></el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>

      <!-- Quick Actions -->
      <el-divider />
      <h3>Quick Actions</h3>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-button type="primary" @click="$router.push('/admin/users')" style="width: 100%">
            <el-icon><User /></el-icon>
            Manage Users
          </el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-button type="success" @click="$router.push('/admin/settings')" style="width: 100%">
            <el-icon><Setting /></el-icon>
            System Settings
          </el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-button type="warning" @click="$router.push('/admin/logs')" style="width: 100%">
            <el-icon><Document /></el-icon>
            View Logs
          </el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-button type="info" @click="$router.push('/admin/system')" style="width: 100%">
            <el-icon><Operation /></el-icon>
            System Monitor
          </el-button>
        </el-col>
      </el-row>

      <!-- Recent Activity -->
      <el-divider />
      <h3>Recent System Activity</h3>
      <el-table :data="recentActivity" max-height="400" v-loading="loading">
        <el-table-column prop="activityType" label="Type" width="150" />
        <el-table-column prop="description" label="Description" />
        <el-table-column prop="performedBy" label="User" width="150" />
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
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { User, Connection, CircleCheck, Folder, Setting, Document, Operation } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '@/utils/http'

export default {
  name: 'AdminDashboardView',
  components: {
    User, Connection, CircleCheck, Folder, Setting, Document, Operation
  },
  setup() {
    const loading = ref(false)
    const stats = ref({
      totalUsers: 0,
      activeSessions: 0,
      storageUsed: 0
    })

    const recentActivity = ref([])

    const loadStats = async () => {
      try {
        // Load user count
        const usersRes = await http.get('/admin/user-management', {
          params: { page: 0, size: 1 }
        })
        stats.value.totalUsers = usersRes.data.totalElements || 0

        // Mock other stats (you can implement real endpoints later)
        stats.value.activeSessions = 5
        stats.value.storageUsed = 120
      } catch (error) {
        console.error('Failed to load stats:', error)
        // Set default values
        stats.value = {
          totalUsers: 0,
          activeSessions: 0,
          storageUsed: 0
        }
      }
    }

    const loadRecentActivity = async () => {
      loading.value = true
      try {
        const res = await http.get('/admin/activity-logs/recent', {
          params: { days: 7 }
        })
        recentActivity.value = res.data.slice(0, 10)
      } catch (error) {
        console.error('Failed to load activity:', error)
        ElMessage.warning('Could not load recent activity')
        recentActivity.value = []
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
      return types[status] || 'info'
    }

    onMounted(() => {
      loadStats()
      loadRecentActivity()
    })

    return {
      loading,
      stats,
      recentActivity,
      formatDateTime,
      getStatusType
    }
  }
}
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

h3 {
  margin: 20px 0 10px 0;
}
</style>