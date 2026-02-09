<template>
  <div class="conversation-list">
    <div class="list-header">
      <h3>Messages</h3>
      <el-button
          v-if="showNewButton"
          size="small"
          type="primary"
          @click="$emit('new')"
          :icon="Plus"
      >
        New
      </el-button>
    </div>

    <el-input
        v-model="searchQuery"
        placeholder="Search conversations..."
        clearable
        style="margin-bottom: 12px;"
    >
      <template #prefix><el-icon><Search /></el-icon></template>
    </el-input>

    <div class="conversations">
      <div
          v-for="conv in filteredConversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: conv.id === selectedId }"
          @click="$emit('select', conv)"
      >
        <el-badge :value="conv.unreadCount" :hidden="conv.unreadCount === 0" class="badge">
          <el-avatar :size="48">{{ getInitials(getDisplayName(conv)) }}</el-avatar>
        </el-badge>
        <div class="conv-info">
          <div class="conv-header">
            <span class="person-name">{{ getDisplayName(conv) }}</span>
            <span class="time">{{ formatTime(conv.lastMessageAt) }}</span>
          </div>
          <div class="person-id">{{ getDisplayId(conv) }}</div>
          <div class="last-message">{{ conv.lastMessagePreview || 'No messages yet' }}</div>
        </div>
      </div>

      <el-empty v-if="filteredConversations.length === 0" description="No conversations" />
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'

export default {
  name: 'ConversationList',
  components: { Plus, Search },
  props: {
    conversations: { type: Array, default: () => [] },
    selectedId: Number,
    userRole: { type: String, required: true }
  },
  emits: ['select', 'new'],
  setup(props) {
    const searchQuery = ref('')

    // Only doctors and patients can create new conversations
    // Nurses cannot message
    const showNewButton = computed(() => {
      return props.userRole === 'DOCTOR' || props.userRole === 'PATIENT' || props.userRole === 'ADMIN'
    })

    const filteredConversations = computed(() => {
      if (!searchQuery.value) return props.conversations
      const q = searchQuery.value.toLowerCase()
      return props.conversations.filter(c => {
        const displayName = getDisplayName(c)
        return displayName?.toLowerCase().includes(q) ||
            c.lastMessagePreview?.toLowerCase().includes(q)
      })
    })

    // Get display name based on user role
    const getDisplayName = (conv) => {
      if (props.userRole === 'DOCTOR' || props.userRole === 'ADMIN') {
        // Doctors see patient name
        return conv.patientName || 'Unknown Patient'
      } else {
        // Patients see doctor name
        return conv.doctorName || 'Doctor'
      }
    }

    // Get display ID based on user role
    const getDisplayId = (conv) => {
      if (props.userRole === 'DOCTOR' || props.userRole === 'ADMIN') {
        return conv.patientIdNumber ? `ID: ${conv.patientIdNumber}` : ''
      }
      return ''
    }

    const getInitials = (name) => {
      if (!name) return '?'
      return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase()
    }

    const formatTime = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      const now = new Date()
      const diff = now - date

      if (diff < 86400000) { // Less than 24 hours
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      } else if (diff < 604800000) { // Less than 7 days
        return date.toLocaleDateString([], { weekday: 'short' })
      } else {
        return date.toLocaleDateString([], { month: 'short', day: 'numeric' })
      }
    }

    return {
      searchQuery,
      filteredConversations,
      showNewButton,
      getDisplayName,
      getDisplayId,
      getInitials,
      formatTime
    }
  }
}
</script>

<style scoped>
.conversation-list { display: flex; flex-direction: column; height: 100%; }
.list-header { padding: 16px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #dcdfe6; }
.list-header h3 { margin: 0; }
.conversations { flex: 1; overflow-y: auto; padding: 8px; }
.conversation-item { padding: 12px; display: flex; gap: 12px; cursor: pointer; border-radius: 8px; transition: background 0.2s; margin-bottom: 4px; }
.conversation-item:hover { background: #f5f7fa; }
.conversation-item.active { background: #ecf5ff; }
.badge { flex-shrink: 0; }
.conv-info { flex: 1; min-width: 0; }
.conv-header { display: flex; justify-content: space-between; margin-bottom: 2px; }
.person-name { font-weight: 600; font-size: 14px; color: #303133; }
.person-id { font-size: 11px; color: #909399; margin-bottom: 4px; }
.time { font-size: 12px; color: #909399; }
.last-message { font-size: 13px; color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>