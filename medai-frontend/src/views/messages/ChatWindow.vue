<template>
  <div class="chat-window">
    <el-container>
      <!-- Left Sidebar - Conversations List -->
      <el-aside width="320px" class="conversations-sidebar">
        <ConversationList
            :conversations="conversations"
            :selected-id="selectedConversationId"
            :user-role="currentUser.role"
            @select="selectConversation"
            @new="openNewConversationDialog"
        />
      </el-aside>

      <!-- Main Chat Area -->
      <el-main class="chat-main">
        <div v-if="selectedConversation" class="chat-container">
          <!-- Chat Header -->
          <div class="chat-header">
            <div class="header-left">
              <el-avatar :size="40">{{ getInitials(getOtherPersonName()) }}</el-avatar>
              <div class="person-info">
                <h3>{{ getOtherPersonName() }}</h3>
                <span class="person-id">{{ getOtherPersonId() }}</span>
              </div>
            </div>
            <div class="header-actions">
              <el-button size="small" @click="toggleMute">
                <el-icon><component :is="selectedConversation.isMuted ? Bell : MuteNotification" /></el-icon>
                {{ selectedConversation.isMuted ? 'Unmute' : 'Mute' }}
              </el-button>
              <el-button size="small" @click="archiveConversation">
                <el-icon><FolderOpened /></el-icon>
                Archive
              </el-button>
            </div>
          </div>

          <!-- Messages Area -->
          <div class="messages-area" ref="messagesArea">
            <div v-if="loadingMessages" class="loading">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div v-else>
              <MessageBubble
                  v-for="message in messages"
                  :key="message.id"
                  :message="message"
                  :is-own="isMessageFromCurrentUser(message)"
                  :sender-name="getSenderName(message)"
                  @download="downloadFile"
              />
            </div>
          </div>

          <!-- Message Input -->
          <MessageInput
              @send="sendMessage"
              @upload="uploadFile"
              :disabled="sending"
          />
        </div>

        <!-- Empty State -->
        <div v-else class="empty-state">
          <el-empty description="Select a conversation to start messaging">
            <el-button v-if="canCreateConversation" type="primary" @click="openNewConversationDialog">
              Start New Conversation
            </el-button>
          </el-empty>
        </div>
      </el-main>
    </el-container>

    <!-- New Conversation Dialog - FOR DOCTORS -->
    <el-dialog
        v-model="showNewConversationDialog"
        :title="dialogTitle"
        width="500px"
        @close="resetNewConversationForm"
    >
      <el-form :model="newConvForm" label-width="100px">
        <!-- FOR DOCTORS: Search Patients -->
        <el-form-item v-if="isDoctor" label="Patient">
          <el-select
              v-model="newConvForm.patientId"
              filterable
              remote
              reserve-keyword
              placeholder="Search patient by name or ID..."
              :remote-method="searchPatients"
              :loading="searchingPatients"
              style="width:100%;"
          >
            <el-option
                v-for="p in patientOptions"
                :key="p.id"
                :label="`${p.name} - ${p.patientId}`"
                :value="p.id"
            >
              <div style="display: flex; justify-content: space-between;">
                <span>{{ p.name }}</span>
                <span style="color: #8492a6; font-size: 13px;">{{ p.patientId }}</span>
              </div>
            </el-option>
          </el-select>
          <div style="margin-top: 8px; font-size: 12px; color: #909399;">
            Search for your patients to start a conversation
          </div>
        </el-form-item>

        <!-- FOR PATIENTS: Search Doctors -->
        <el-form-item v-else label="Doctor">
          <el-select
              v-model="newConvForm.doctorId"
              filterable
              remote
              reserve-keyword
              placeholder="Search doctor by name..."
              :remote-method="searchDoctors"
              :loading="searchingDoctors"
              style="width:100%;"
          >
            <el-option
                v-for="d in doctorOptions"
                :key="d.id"
                :label="d.fullName || d.username"
                :value="d.id"
            >
              <div style="display: flex; justify-content: space-between;">
                <span>{{ d.fullName || d.username }}</span>
                <span style="color: #8492a6; font-size: 13px;">Doctor</span>
              </div>
            </el-option>
          </el-select>
          <div style="margin-top: 8px; font-size: 12px; color: #909399;">
            Search for a doctor to start a conversation
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showNewConversationDialog = false">Cancel</el-button>
        <el-button type="primary" @click="createConversation" :loading="creatingConversation">
          Start Chat
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import { Bell, MuteNotification, FolderOpened, Loading } from '@element-plus/icons-vue'
import ConversationList from './ConversationList.vue'
import MessageBubble from './MessageBubble.vue'
import MessageInput from './MessageInput.vue'

export default {
  name: 'ChatWindow',
  components: {
    ConversationList,
    MessageBubble,
    MessageInput
  },
  setup() {
    const conversations = ref([])
    const selectedConversationId = ref(null)
    const selectedConversation = ref(null)
    const messages = ref([])
    const loadingMessages = ref(false)
    const sending = ref(false)
    const messagesArea = ref(null)
    const showNewConversationDialog = ref(false)
    const patientOptions = ref([])
    const doctorOptions = ref([])
    const searchingPatients = ref(false)
    const searchingDoctors = ref(false)
    const creatingConversation = ref(false)
    const newConvForm = reactive({
      patientId: null,
      doctorId: null
    })

    // Get current user info
    const currentUser = computed(() => {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      return {
        id: user.id,
        role: user.role,
        username: user.username,
        fullName: user.fullName
      }
    })

    const isDoctor = computed(() =>
        currentUser.value.role === 'DOCTOR' || currentUser.value.role === 'ADMIN'
    )

    const isPatient = computed(() =>
        currentUser.value.role === 'PATIENT'
    )

    const canCreateConversation = computed(() =>
        isDoctor.value || isPatient.value
    )

    const dialogTitle = computed(() =>
        isDoctor.value ? 'New Conversation with Patient' : 'New Conversation with Doctor'
    )

    const loadConversations = async () => {
      try {
        const res = await http.get('/conversations')
        conversations.value = res.data
      } catch (e) {
        ElMessage.error('Failed to load conversations')
      }
    }

    const selectConversation = async (conv) => {
      selectedConversationId.value = conv.id
      selectedConversation.value = conv
      await loadMessages(conv.id)
      markAsRead(conv.id)
    }

    const loadMessages = async (convId) => {
      loadingMessages.value = true
      try {
        const res = await http.get(`/conversations/${convId}/messages`)
        messages.value = res.data
        await nextTick()
        scrollToBottom()
      } catch (e) {
        ElMessage.error('Failed to load messages')
      } finally {
        loadingMessages.value = false
      }
    }

    const sendMessage = async (content) => {
      if (!selectedConversationId.value) return

      sending.value = true
      try {
        const res = await http.post('/messages', {
          conversationId: selectedConversationId.value,
          content
        })
        messages.value.push(res.data)
        await nextTick()
        scrollToBottom()
        loadConversations() // Update conversation list
      } catch (e) {
        ElMessage.error('Failed to send message')
      } finally {
        sending.value = false
      }
    }

    const uploadFile = async (file) => {
      if (!selectedConversationId.value) return

      sending.value = true
      const formData = new FormData()
      formData.append('conversationId', selectedConversationId.value)
      formData.append('file', file)

      try {
        const res = await http.post('/messages/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        messages.value.push(res.data)
        await nextTick()
        scrollToBottom()
        loadConversations()
      } catch (e) {
        ElMessage.error('Failed to upload file')
      } finally {
        sending.value = false
      }
    }

    const markAsRead = async (convId) => {
      try {
        await http.put(`/messages/conversation/${convId}/read-all`)
        loadConversations() // Refresh unread counts
      } catch (e) {
        console.error('Failed to mark as read')
      }
    }

    const toggleMute = async () => {
      try {
        const res = await http.put(`/conversations/${selectedConversationId.value}/mute`)
        selectedConversation.value = res.data
        loadConversations()
        ElMessage.success(res.data.isMuted ? 'Conversation muted' : 'Conversation unmuted')
      } catch (e) {
        ElMessage.error('Failed to update settings')
      }
    }

    const archiveConversation = async () => {
      try {
        await http.put(`/conversations/${selectedConversationId.value}/archive`)
        ElMessage.success('Conversation archived')
        selectedConversationId.value = null
        selectedConversation.value = null
        messages.value = []
        loadConversations()
      } catch (e) {
        ElMessage.error('Failed to archive')
      }
    }

    // ✅ FOR DOCTORS: Search patients
    const searchPatients = async (query) => {
      if (!query || query.length < 2) return
      searchingPatients.value = true
      try {
        const res = await http.get('/patient/doctor/patients', {
          params: { search: query }
        })
        patientOptions.value = res.data
      } catch (e) {
        console.error('Failed to search patients:', e)
      } finally {
        searchingPatients.value = false
      }
    }

    // ✅ FOR PATIENTS: Search doctors
    const searchDoctors = async (query) => {
      if (!query || query.length < 2) return
      searchingDoctors.value = true
      try {
        // Search for users with DOCTOR role
        const res = await http.get('/users', {
          params: {
            role: 'DOCTOR',
            search: query
          }
        })
        doctorOptions.value = res.data
      } catch (e) {
        console.error('Failed to search doctors:', e)
        ElMessage.warning('Search doctors feature requires backend support')
      } finally {
        searchingDoctors.value = false
      }
    }

    const openNewConversationDialog = () => {
      showNewConversationDialog.value = true
    }

    const resetNewConversationForm = () => {
      newConvForm.patientId = null
      newConvForm.doctorId = null
      patientOptions.value = []
      doctorOptions.value = []
    }

    const createConversation = async () => {
      // Validation
      if (isDoctor.value && !newConvForm.patientId) {
        ElMessage.warning('Please select a patient')
        return
      }
      if (isPatient.value && !newConvForm.doctorId) {
        ElMessage.warning('Please select a doctor')
        return
      }

      creatingConversation.value = true
      try {
        const payload = isDoctor.value
            ? { patientId: newConvForm.patientId }
            : { doctorId: newConvForm.doctorId }

        const res = await http.post('/conversations', payload)
        showNewConversationDialog.value = false
        resetNewConversationForm()
        await loadConversations()

        // Auto-select the new conversation
        const newConv = conversations.value.find(c => c.id === res.data.id)
        if (newConv) {
          selectConversation(newConv)
        }

        ElMessage.success('Conversation started successfully')
      } catch (e) {
        console.error('Create conversation error:', e)
        if (e.response?.status === 400) {
          ElMessage.error(e.response.data.message || 'Failed to create conversation')
        } else {
          ElMessage.error('Failed to start conversation. Please try again.')
        }
      } finally {
        creatingConversation.value = false
      }
    }

    const downloadFile = (message) => {
      window.open(message.filePath, '_blank')
    }

    const scrollToBottom = () => {
      if (messagesArea.value) {
        messagesArea.value.scrollTop = messagesArea.value.scrollHeight
      }
    }

    const getInitials = (name) => {
      if (!name) return '?'
      return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase()
    }

    // ✅ Determine if message is from current user
    const isMessageFromCurrentUser = (message) => {
      if (message.senderUserId) {
        return message.senderUserId === currentUser.value.id
      }

      // Fallback
      if (isDoctor.value) {
        return message.senderType === 'doctor'
      } else {
        return message.senderType === 'patient'
      }
    }

    // ✅ Get sender name for message
    const getSenderName = (message) => {
      if (isMessageFromCurrentUser(message)) {
        return currentUser.value.fullName || 'You'
      }
      return message.senderUserName || (message.senderType === 'doctor' ? 'Doctor' : 'Patient')
    }

    // ✅ Get the other person's name
    const getOtherPersonName = () => {
      if (!selectedConversation.value) return ''
      if (isDoctor.value) {
        return selectedConversation.value.patientName || 'Patient'
      } else {
        return selectedConversation.value.doctorName || 'Doctor'
      }
    }

    // ✅ Get the other person's ID
    const getOtherPersonId = () => {
      if (!selectedConversation.value) return ''
      if (isDoctor.value) {
        return selectedConversation.value.patientIdNumber || ''
      } else {
        return 'Medical Professional'
      }
    }

    onMounted(() => {
      loadConversations()
    })

    return {
      conversations, selectedConversationId, selectedConversation, messages,
      loadingMessages, sending, messagesArea, showNewConversationDialog,
      patientOptions, doctorOptions, searchingPatients, searchingDoctors,
      creatingConversation, newConvForm, currentUser, isDoctor, isPatient,
      canCreateConversation, dialogTitle,
      selectConversation, sendMessage, uploadFile, toggleMute, archiveConversation,
      searchPatients, searchDoctors, openNewConversationDialog,
      resetNewConversationForm, createConversation, downloadFile, getInitials,
      isMessageFromCurrentUser, getSenderName, getOtherPersonName, getOtherPersonId,
      Bell, MuteNotification, FolderOpened, Loading
    }
  }
}
</script>

<style scoped>
.chat-window { height: calc(100vh - 60px); }
.conversations-sidebar { border-right: 1px solid #dcdfe6; overflow-y: auto; background: #fff; }
.chat-main { padding: 0; display: flex; flex-direction: column; }
.chat-container { display: flex; flex-direction: column; height: 100%; }
.chat-header { padding: 16px 20px; border-bottom: 1px solid #dcdfe6; display: flex; justify-content: space-between; align-items: center; background: #fafafa; }
.header-left { display: flex; gap: 12px; align-items: center; }
.person-info h3 { margin: 0; font-size: 16px; font-weight: 600; }
.person-id { font-size: 12px; color: #909399; }
.header-actions { display: flex; gap: 8px; }
.messages-area { flex: 1; overflow-y: auto; padding: 20px; background: #f5f5f5; }
.loading { text-align: center; padding: 40px; }
.empty-state { display: flex; align-items: center; justify-content: center; height: 100%; background: #f5f5f5; }
</style>