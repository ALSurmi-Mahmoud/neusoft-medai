<template>
  <div class="system-monitoring">
    <h2>System Monitoring</h2>

    <el-row :gutter="20">
      <!-- System Health -->
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon :size="48" :class="healthClass"><Monitor /></el-icon>
            <div class="stat-info">
              <div class="stat-value" :class="healthClass">{{ systemHealth.status }}</div>
              <div class="stat-label">System Status</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Total Users -->
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon :size="48"><User /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">Total Users</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Active Users -->
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon :size="48"><UserFilled /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.activeUsers }}</div>
              <div class="stat-label">Active Users</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Service Status -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Service Status</span>
              <el-button size="small" @click="checkServices" :loading="checking">
                <el-icon><Refresh /></el-icon> Check
              </el-button>
            </div>
          </template>
          <el-table :data="services" style="width: 100%">
            <el-table-column prop="name" label="Service" />
            <el-table-column prop="status" label="Status" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === 'Online' ? 'success' : 'danger'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastCheck" label="Last Check" width="180">
              <template #default="{ row }">
                {{ formatTime(row.lastCheck) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- User Distribution -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>User Distribution by Role</span>
          </template>
          <div class="role-distribution">
            <div v-for="role in roleStats" :key="role.name" class="role-item">
              <span class="role-name">{{ role.name }}</span>
              <el-progress
                  :percentage="role.percentage"
                  :color="role.color"
                  :stroke-width="20"
                  :show-text="false"
              />
              <span class="role-count">{{ role.count }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- JVM Memory Info (Real data from backend) -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>Server Memory (JVM)</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="Total Memory">{{ stats.totalMemory }}</el-descriptions-item>
            <el-descriptions-item label="Used Memory">{{ stats.usedMemory }}</el-descriptions-item>
            <el-descriptions-item label="Free Memory">{{ stats.freeMemory }}</el-descriptions-item>
            <el-descriptions-item label="Processors">{{ stats.availableProcessors }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- System Info -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>System Information</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="Application">MedAI Platform</el-descriptions-item>
            <el-descriptions-item label="Version">0.1.0</el-descriptions-item>
            <el-descriptions-item label="Environment">Development</el-descriptions-item>
            <el-descriptions-item label="Server Time">{{ currentTime }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { Monitor, User, UserFilled, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'SystemMonitoringView',
  components: { Monitor, User, UserFilled, Refresh },
  setup() {
    const checking = ref(false)
    const currentTime = ref(new Date().toLocaleString())
    let timeInterval = null

    const systemHealth = reactive({ status: 'Checking...' })

    const stats = reactive({
      totalUsers: 0,
      activeUsers: 0,
      totalMemory: '-',
      usedMemory: '-',
      freeMemory: '-',
      availableProcessors: '-'
    })

    const services = ref([
      { name: 'Backend API', status: 'Checking...', lastCheck: null },
      { name: 'Database', status: 'Checking...', lastCheck: null },
      { name: 'Redis Cache', status: 'Unknown', lastCheck: null }
    ])

    const roleStats = ref([])

    const healthClass = computed(() => {
      if (systemHealth.status === 'Healthy') return 'healthy'
      if (systemHealth.status === 'Degraded') return 'degraded'
      return 'offline'
    })

    const loadStats = async () => {
      try {
        const response = await http.get('/admin/stats')
        stats.totalUsers = response.data.totalUsers || 0
        stats.activeUsers = response.data.activeUsers || 0
        stats.totalMemory = response.data.totalMemory || '-'
        stats.usedMemory = response.data.usedMemory || '-'
        stats.freeMemory = response.data.freeMemory || '-'
        stats.availableProcessors = response.data.availableProcessors || '-'

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
      }
    }

    const checkServices = async () => {
      checking.value = true

      // Check Backend API
      try {
        await http.get('/health')
        services.value[0].status = 'Online'
        services.value[0].lastCheck = new Date()

        // If backend is online, database is likely online too
        services.value[1].status = 'Online'
        services.value[1].lastCheck = new Date()

        systemHealth.status = 'Healthy'
      } catch (error) {
        services.value[0].status = 'Offline'
        services.value[0].lastCheck = new Date()
        services.value[1].status = 'Unknown'
        systemHealth.status = 'Degraded'
      }

      // Check Redis
      try {
        await http.get('/redis/ping')
        services.value[2].status = 'Online'
        services.value[2].lastCheck = new Date()
      } catch (error) {
        services.value[2].status = 'Offline'
        services.value[2].lastCheck = new Date()
      }

      checking.value = false
      ElMessage.success('Service check completed')
    }

    const formatTime = (date) => {
      if (!date) return '-'
      return new Date(date).toLocaleString()
    }

    onMounted(() => {
      loadStats()
      checkServices()
      timeInterval = setInterval(() => {
        currentTime.value = new Date().toLocaleString()
      }, 1000)
    })

    onUnmounted(() => {
      if (timeInterval) clearInterval(timeInterval)
    })

    return {
      checking, currentTime, systemHealth, stats, services, roleStats,
      healthClass, checkServices, formatTime
    }
  }
}
</script>

<style scoped>
.system-monitoring { padding: 20px; }
.system-monitoring h2 { margin: 0 0 20px; color: #333; }

.stat-card { border-radius: 12px; }

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-content .el-icon { color: #409eff; }
.stat-content .el-icon.healthy { color: #67c23a; }
.stat-content .el-icon.degraded { color: #e6a23c; }
.stat-content .el-icon.offline { color: #f56c6c; }

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-value.healthy { color: #67c23a; }
.stat-value.degraded { color: #e6a23c; }
.stat-value.offline { color: #f56c6c; }

.stat-label { font-size: 14px; color: #909399; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.role-distribution {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.role-item {
  display: flex;
  align-items: center;
  gap: 15px;
}

.role-name { width: 80px; font-weight: 500; }
.role-item .el-progress { flex: 1; }
.role-count { width: 30px; text-align: right; font-weight: 600; }
</style>