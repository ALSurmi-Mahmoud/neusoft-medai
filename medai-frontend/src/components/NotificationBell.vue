<template>
  <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
    <el-button circle @click="showNotifications">
      <el-icon :size="20">
        <Bell />
      </el-icon>
    </el-button>
  </el-badge>

  <!-- Notifications Drawer -->
  <el-drawer
      v-model="drawerVisible"
      title="Notifications"
      direction="rtl"
      size="400px"
  >
    <div v-loading="loading">
      <el-empty v-if="notifications.length === 0" description="No notifications" />

      <div v-else class="notifications-list">
        <div
            v-for="notif in notifications"
            :key="notif.id"
            class="notification-item"
            :class="{ 'unread': !notif.isRead }"
            @click="markAsRead(notif.id)"
        >
          <div class="notif-header">
            <span class="notif-title">{{ notif.title }}</span>
            <span class="notif-time">{{ formatTime(notif.createdAt) }}</span>
          </div>
          <div class="notif-message">{{ notif.message }}</div>
          <el-tag v-if="!notif.isRead" type="primary" size="small">New</el-tag>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import http from '../utils/http'

export default {
  name: 'NotificationBell',
  components: { Bell },
  setup() {
    const unreadCount = ref(0)
    const drawerVisible = ref(false)
    const notifications = ref([])
    const loading = ref(false)
    let pollInterval = null

    // ✅ FIX: Check if user is logged in before fetching
    const isLoggedIn = () => {
      return !!localStorage.getItem('accessToken')
    }

    const fetchUnreadCount = async () => {
      // ✅ FIX: Don't fetch if not logged in
      if (!isLoggedIn()) {
        unreadCount.value = 0
        return
      }

      try {
        const res = await http.get('/notifications/unread-count')
        unreadCount.value = res.data.unreadCount || 0
      } catch (error) {
        // ✅ FIX: Silently handle 403 errors (happens during logout)
        if (error.response?.status === 403 || error.response?.status === 401) {
          unreadCount.value = 0
          stopPolling() // Stop polling if unauthorized
        } else {
          console.error('Failed to load unread count:', error)
        }
      }
    }

    const fetchNotifications = async () => {
      // ✅ FIX: Don't fetch if not logged in
      if (!isLoggedIn()) {
        notifications.value = []
        return
      }

      loading.value = true
      try {
        const res = await http.get('/notifications')
        notifications.value = res.data
      } catch (error) {
        // ✅ FIX: Silently handle auth errors
        if (error.response?.status === 403 || error.response?.status === 401) {
          notifications.value = []
        } else {
          console.error('Failed to load notifications:', error)
        }
      } finally {
        loading.value = false
      }
    }

    const showNotifications = () => {
      drawerVisible.value = true
      fetchNotifications()
    }

    const markAsRead = async (id) => {
      try {
        await http.put(`/notifications/${id}/read`)
        // Update local state
        const notif = notifications.value.find(n => n.id === id)
        if (notif) {
          notif.isRead = true
          unreadCount.value = Math.max(0, unreadCount.value - 1)
        }
      } catch (error) {
        console.error('Failed to mark as read:', error)
      }
    }

    const formatTime = (dateStr) => {
      const date = new Date(dateStr)
      const now = new Date()
      const diff = now - date

      if (diff < 60000) return 'Just now'
      if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`
      if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`
      if (diff < 604800000) return `${Math.floor(diff / 86400000)}d ago`
      return date.toLocaleDateString()
    }

    const startPolling = () => {
      // Poll every 30 seconds
      pollInterval = setInterval(() => {
        if (isLoggedIn()) {
          fetchUnreadCount()
        } else {
          stopPolling()
        }
      }, 30000)
    }

    const stopPolling = () => {
      if (pollInterval) {
        clearInterval(pollInterval)
        pollInterval = null
      }
    }

    onMounted(() => {
      // Only fetch if logged in
      if (isLoggedIn()) {
        fetchUnreadCount()
        startPolling()
      }
    })

    onUnmounted(() => {
      stopPolling()
    })

    return {
      unreadCount,
      drawerVisible,
      notifications,
      loading,
      showNotifications,
      markAsRead,
      formatTime
    }
  }
}
</script>

<style scoped>
.notification-badge {
  margin: 0 12px;
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  padding: 16px;
  border-radius: 8px;
  background: #f5f7fa;
  cursor: pointer;
  transition: background 0.2s;
}

.notification-item:hover {
  background: #e4e7ed;
}

.notification-item.unread {
  background: #ecf5ff;
  border-left: 3px solid #409eff;
}

.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notif-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.notif-time {
  font-size: 12px;
  color: #909399;
}

.notif-message {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  line-height: 1.5;
}
</style>