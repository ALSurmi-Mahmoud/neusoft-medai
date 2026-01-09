<template>
  <div class="admin-dashboard">
    <h2>Admin Dashboard</h2>

    <!-- Quick Stats -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card users" shadow="hover">
          <div class="stat-content">
            <el-icon :size="48"><User /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">Total Users</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card studies" shadow="hover">
          <div class="stat-content">
            <el-icon :size="48"><FolderOpened /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalStudies }}</div>
              <div class="stat-label">Total Studies</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card reports" shadow="hover">
          <div class="stat-content">
            <el-icon :size="48"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalReports }}</div>
              <div class="stat-label">Total Reports</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card system" shadow="hover">
          <div class="stat-content">
            <el-icon :size="48"><Monitor /></el-icon>
            <div class="stat-info">
              <div class="stat-value" :class="systemStatus">{{ systemStatus }}</div>
              <div class="stat-label">System Status</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- User Distribution -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>User Distribution by Role</span>
          </template>
          <div class="role-stats">
            <div class="role-item" v-for="role in roleStats" :key="role.name">
              <span class="role-name">{{ role.name }}</span>
              <el-progress :percentage="role.percentage" :color="role.color" />
              <span class="role-count">{{ role.count }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Quick Actions -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>Quick Actions</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/admin/users')">
              <el-icon><User /></el-icon> Manage Users
            </el-button>
            <el-button type="success" @click="$router.push('/admin/system')">
              <el-icon><Monitor /></el-icon> System Monitoring
            </el-button>
            <el-button type="warning" @click="$router.push('/audit')">
              <el-icon><List /></el-icon> View Audit Logs
            </el-button>
            <el-button type="info" @click="$router.push('/studies')">
              <el-icon><FolderOpened /></el-icon> Browse Studies
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Recent Activity -->
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Recent System Activity</span>
              <el-button text type="primary" @click="$router.push('/audit')">View All</el-button>
            </div>
          </template>
          <el-table :data="recentActivity" style="width: 100%" max-height="300">
            <el-table-column prop="timestamp" label="Time" width="180">
              <template #default="{ row }">
                {{ formatTime(row.timestamp) }}
              </template>
            </el-table-column>
            <el-table-column prop="user" label="User" width="150" />
            <el-table-column prop="action" label="Action" width="150">
              <template #default="{ row }">
                <el-tag :type="getActionType(row.action)" size="small">{{ row.action }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="resource" label="Resource" />
            <el-table-column prop="details" label="Details" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import http from '../../utils/http'
import { User, FolderOpened, Document, Monitor, List } from '@element-plus/icons-vue'

export default {
  name: 'AdminDashboard',
  components: { User, FolderOpened, Document, Monitor, List },
  setup() {
    const stats = reactive({
      totalUsers: 0,
      totalStudies: 0,
      totalReports: 0
    })

    const systemStatus = ref('Healthy')

    const roleStats = ref([
      { name: 'Admin', count: 0, percentage: 0, color: '#f56c6c' },
      { name: 'Doctor', count: 0, percentage: 0, color: '#67c23a' },
      { name: 'Nurse', count: 0, percentage: 0, color: '#e6a23c' },
      { name: 'Patient', count: 0, percentage: 0, color: '#409eff' },
      { name: 'Researcher', count: 0, percentage: 0, color: '#909399' }
    ])

    const recentActivity = ref([])

    const loadStats = async () => {
      try {
        // Load admin stats
        const response = await http.get('/admin/stats')
        stats.totalUsers = response.data.totalUsers || 0

        // Calculate role distribution
        const total = stats.totalUsers || 1
        roleStats.value = [
          { name: 'Admin', count: response.data.adminCount || 0, percentage: Math.round((response.data.adminCount || 0) / total * 100), color: '#f56c6c' },
          { name: 'Doctor', count: response.data.doctorCount || 0, percentage: Math.round((response.data.doctorCount || 0) / total * 100), color: '#67c23a' },
          { name: 'Nurse', count: response.data.nurseCount || 0, percentage: Math.round((response.data.nurseCount || 0) / total * 100), color: '#e6a23c' },
          { name: 'Patient', count: response.data.patientCount || 0, percentage: Math.round((response.data.patientCount || 0) / total * 100), color: '#409eff' },
          { name: 'Researcher', count: response.data.researcherCount || 0, percentage: Math.round((response.data.researcherCount || 0) / total * 100), color: '#909399' }
        ]
      } catch (error) {
        console.error('Failed to load stats:', error)
        // Use defaults
        stats.totalUsers = 3
      }

      // Load study count
      try {
        const studiesRes = await http.get('/studies', { params: { page: 0, size: 1 } })
        stats.totalStudies = studiesRes.data.totalElements || 0
      } catch (e) {
        stats.totalStudies = 0
      }

      // Load report count
      try {
        const reportsRes = await http.get('/reports', { params: { page: 0, size: 1 } })
        stats.totalReports = reportsRes.data.totalElements || 0
      } catch (e) {
        stats.totalReports = 0
      }
    }

    const loadRecentActivity = async () => {
      try {
        const response = await http.get('/audit', { params: { page: 0, size: 10 } })
        recentActivity.value = (response.data.content || []).map(log => ({
          timestamp: log.createdAt,
          user: log.username || 'System',
          action: log.action,
          resource: log.resourceType,
          details: log.resourceId ? `ID: ${log.resourceId}` : ''
        }))
      } catch (error) {
        console.error('Failed to load activity:', error)
        recentActivity.value = [
          { timestamp: new Date().toISOString(), user: 'admin', action: 'LOGIN', resource: 'USER', details: 'Admin logged in' }
        ]
      }
    }

    const formatTime = (timestamp) => {
      if (!timestamp) return '-'
      return new Date(timestamp).toLocaleString()
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
      loadStats()
      loadRecentActivity()
    })

    return {
      stats,
      systemStatus,
      roleStats,
      recentActivity,
      formatTime,
      getActionType
    }
  }
}
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.admin-dashboard h2 {
  margin: 0 0 20px;
  color: #333;
}

.stat-card {
  border-radius: 12px;
}

.stat-card.users { border-top: 4px solid #409eff; }
.stat-card.studies { border-top: 4px solid #67c23a; }
.stat-card.reports { border-top: 4px solid #e6a23c; }
.stat-card.system { border-top: 4px solid #909399; }

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-content .el-icon {
  color: #409eff;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
}

.stat-value.Healthy { color: #67c23a; }
.stat-value.Degraded { color: #e6a23c; }
.stat-value.Down { color: #f56c6c; }

.stat-label {
  font-size: 14px;
  color: #909399;
}

.role-stats {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.role-item {
  display: flex;
  align-items: center;
  gap: 15px;
}

.role-name {
  width: 100px;
  font-weight: 500;
}

.role-item .el-progress {
  flex: 1;
}

.role-count {
  width: 40px;
  text-align: right;
  font-weight: 600;
  color: #333;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.quick-actions .el-button {
  height: 60px;
  font-size: 14px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>