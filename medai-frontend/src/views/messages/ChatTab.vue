<template>
  <div class="chat-tab">
    <div class="tab-header">
      <h3>Messages</h3>
      <el-button type="primary" size="small" @click="startConversation">
        <el-icon><ChatDotRound /></el-icon> Start Conversation
      </el-button>
    </div>

    <div v-loading="loading">
      <div v-if="conversation" class="conversation-card">
        <div class="card-header">
          <el-tag :type="conversation.status === 'active' ? 'success' : 'info'">
            {{ conversation.status }}
          </el-tag>
          <span class="last-message-time">{{ formatDate(conversation.lastMessageAt) }}</span>
        </div>

        <p v-if="conversation.lastMessagePreview" class="preview">
          {{ conversation.lastMessagePreview }}
        </p>

        <div class="card-actions">
          <el-button size="small" @click="openChat">
            <el-icon><ChatDotRound /></el-icon> Open Chat
          </el-button>
          <el-badge :value="conversation.unreadCount" :hidden="conversation.unreadCount === 0">
            <span>{{ conversation.unreadCount }} unread</span>
          </el-badge>
        </div>
      </div>

      <el-empty v-else description="No conversation yet" />
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'

export default {
  name: 'ChatTab',
  components: { ChatDotRound },
  props: {
    patientId: { type: Number, required: true }
  },
  setup(props) {
    const router = useRouter()
    const loading = ref(false)
    const conversation = ref(null)

    const loadConversation = async () => {
      loading.value = true
      try {
        const res = await http.get('/conversations', {
          params: { patientId: props.patientId }
        })
        if (res.data && res.data.length > 0) {
          conversation.value = res.data[0]
        }
      } catch (e) {
        console.error('Failed to load conversation')
      } finally {
        loading.value = false
      }
    }

    const startConversation = async () => {
      try {
        const res = await http.post('/conversations', {
          patientId: props.patientId
        })
        conversation.value = res.data
        ElMessage.success('Conversation started')
      } catch (e) {
        ElMessage.error('Failed to start conversation')
      }
    }

    const openChat = () => {
      router.push({ name: 'Messages', query: { conversationId: conversation.value.id } })
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'No messages yet'
      return new Date(dateStr).toLocaleString()
    }

    onMounted(loadConversation)

    return { loading, conversation, startConversation, openChat, formatDate }
  }
}
</script>

<style scoped>
.chat-tab { padding: 20px; }
.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.tab-header h3 { margin: 0; }
.conversation-card { padding: 16px; border: 1px solid #dcdfe6; border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.last-message-time { font-size: 12px; color: #909399; }
.preview { margin: 0 0 12px 0; color: #606266; }
.card-actions { display: flex; justify-content: space-between; align-items: center; }
</style>