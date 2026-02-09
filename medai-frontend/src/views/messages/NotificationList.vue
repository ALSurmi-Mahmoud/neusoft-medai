<template>
  <div class="notification-list">
    <div class="list-header">
      <el-button size="small" @click="markAllAsRead" :disabled="notifications.length === 0">
        Mark all as read
      </el-button>
      <el-button size="small" @click="showSettings = true" :icon="Setting">Settings</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-click="loadNotifications">
      <el-tab-pane label="All" name="all" />
      <el-tab-pane label="Unread" name="unread" />
      <el-tab-pane label="Messages" name="message" />
      <el-tab-pane label="System" name="system" />
    </el-tabs>

    <div v-loading="loading" class="notifications-container">
      <div
          v-for="notif in notifications"
          :key="notif.id"
          class="notification-item"
          :class="{ unread: !notif.isRead }"
          @click="handleNotificationClick(notif)"
      >
        <div class="notif-icon" :class="`type-${notif.type}`">
          <el-icon><component :is="getIcon(notif.type)" /></el-icon>
        </div>
        <div class="notif-content">
          <h4>{{ notif.title }}</h4>
          <p>{{ notif.content }}</p>
          <span class="time">{{ formatTime(notif.createdAt) }}</span>
        </div>
        <el-button
            size="small"
            circle
            @click.stop="deleteNotification(notif.id)"
            :icon="Close"
        />
      </div>

      <el-empty v-if="notifications.length === 0" description="No notifications" />
    </div>

    <!-- Settings Dialog -->
    <NotificationSettings v-model="showSettings" />
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { Setting, Close, ChatDotRound, Bell, Warning, InfoFilled } from '@element-plus/icons-vue'
import NotificationSettings from './NotificationSettings.vue'

export default {
  name: 'NotificationList',
  components: { Setting, Close, ChatDotRound, Bell, Warning, InfoFilled, NotificationSettings },
  emits: ['close'],
  setup(props, { emit }) {
    const router = useRouter()
    const notifications = ref([])
    const loading = ref(false)
    const activeTab = ref('all')
    const showSettings = ref(false)

    const loadNotifications = async () => {
      loading.value = true
      try {
        const params = {}
        if (activeTab.value === 'unread') params.unreadOnly = true
        if (['message', 'system'].includes(activeTab.value)) params.type = activeTab.value

        const res = await http.get('/notifications', { params })
        notifications.value = res.data
      } catch (e) {
        ElMessage.error('Failed to load notifications')
      } finally {
        loading.value = false
      }
    }

    const handleNotificationClick = async (notif) => {
      // Mark as read
      if (!notif.isRead) {
        try {
          await http.put(`/notifications/${notif.id}/read`)
          notif.isRead = true
        } catch (e) {
          console.error('Failed to mark as read')
        }
      }

      // Navigate if has action URL
      if (notif.actionUrl) {
        router.push(notif.actionUrl)
        emit('close')
      }
    }

    const markAllAsRead = async () => {
      try {
        await http.put('/notifications/read-all')
        ElMessage.success('All notifications marked as read')
        loadNotifications()
      } catch (e) {
        ElMessage.error('Failed to mark all as read')
      }
    }

    const deleteNotification = async (id) => {
      try {
        await http.delete(`/notifications/${id}`)
        notifications.value = notifications.value.filter(n => n.id !== id)
        ElMessage.success('Notification deleted')
      } catch (e) {
        ElMessage.error('Failed to delete notification')
      }
    }

    const getIcon = (type) => {
      const icons = {
        message: ChatDotRound,
        appointment: Bell,
        system: InfoFilled,
        prescription: Warning
      }
      return icons[type] || InfoFilled
    }

    const formatTime = (dateStr) => {
      const date = new Date(dateStr)
      const now = new Date()
      const diff = now - date

      if (diff < 3600000) { // Less than 1 hour
        return Math.floor(diff / 60000) + 'm ago'
      } else if (diff < 86400000) { // Less than 24 hours
        return Math.floor(diff / 3600000) + 'h ago'
      } else if (diff < 604800000) { // Less than 7 days
        return Math.floor(diff / 86400000) + 'd ago'
      } else {
        return date.toLocaleDateString()
      }
    }

    onMounted(loadNotifications)

    return {
      notifications, loading, activeTab, showSettings,
      loadNotifications, handleNotificationClick, markAllAsRead, deleteNotification,
      getIcon, formatTime
    }
  }
}
</script>

<style scoped>
.notification-list { display: flex; flex-direction: column; height: 100%; }
.list-header { padding: 12px; display: flex; justify-content: space-between; border-bottom: 1px solid #dcdfe6; }
.notifications-container { flex: 1; overflow-y: auto; padding: 8px; }
.notification-item { padding: 12px; display: flex; gap: 12px; border-radius: 8px; cursor: pointer; transition: background 0.2s; margin-bottom: 8px; }
.notification-item:hover { background: #f5f7fa; }
.notification-item.unread { background: #ecf5ff; }
.notif-icon { width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.type-message { background: #e1f3d8; color: #67c23a; }
.type-appointment { background: #fef0f0; color: #f56c6c; }
.type-system { background: #f4f4f5; color: #909399; }
.type-prescription { background: #fdf6ec; color: #e6a23c; }
.notif-content { flex: 1; min-width: 0; }
.notif-content h4 { margin: 0 0 4px 0; font-size: 14px; font-weight: 600; }
.notif-content p { margin: 0 0 4px 0; font-size: 13px; color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.time { font-size: 12px; color: #909399; }
</style>