<template>
  <div class="message-bubble" :class="{ 'is-own': isOwn }">
    <!-- Avatar for other person's messages (left side) -->
    <el-avatar v-if="!isOwn" :size="32" class="message-avatar">
      {{ senderInitial }}
    </el-avatar>

    <div class="bubble-content">
      <!-- Sender name for received messages -->
      <div v-if="!isOwn && senderName" class="sender-name">{{ senderName }}</div>

      <!-- Text Message -->
      <div v-if="message.messageType === 'text'" class="text-content">
        {{ message.content }}
      </div>

      <!-- Image Message -->
      <div v-else-if="message.messageType === 'image'" class="image-content">
        <el-image
            :src="message.filePath"
            :preview-src-list="[message.filePath]"
            fit="cover"
            style="max-width: 300px; border-radius: 8px;"
        />
        <p class="file-name">{{ message.fileName }}</p>
      </div>

      <!-- File Message -->
      <div v-else-if="message.messageType === 'file'" class="file-content">
        <el-icon size="32"><Document /></el-icon>
        <div class="file-info">
          <p class="file-name">{{ message.fileName }}</p>
          <p class="file-size">{{ formatFileSize(message.fileSize) }}</p>
        </div>
        <el-button size="small" @click="$emit('download', message)">
          <el-icon><Download /></el-icon>
          Download
        </el-button>
      </div>

      <!-- Timestamp -->
      <div class="timestamp">
        {{ formatTime(message.sentAt) }}
        <el-icon v-if="isOwn && message.isRead" color="#67c23a" size="14"><Check /></el-icon>
      </div>
    </div>

    <!-- Avatar for own messages (right side) -->
    <el-avatar v-if="isOwn" :size="32" class="message-avatar">
      {{ senderInitial }}
    </el-avatar>
  </div>
</template>

<script>
import { computed } from 'vue'
import { Document, Download, Check } from '@element-plus/icons-vue'

export default {
  name: 'MessageBubble',
  components: { Document, Download, Check },
  props: {
    message: { type: Object, required: true },
    isOwn: { type: Boolean, default: false },
    senderName: { type: String, default: '' }
  },
  emits: ['download'],
  setup(props) {
    const formatTime = (dateStr) => {
      return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }

    const formatFileSize = (bytes) => {
      if (!bytes) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
    }

    const senderInitial = computed(() => {
      if (props.senderName) {
        return props.senderName.charAt(0).toUpperCase()
      }
      return props.message.senderType === 'doctor' ? 'D' : 'P'
    })

    return { formatTime, formatFileSize, senderInitial }
  }
}
</script>

<style scoped>
.message-bubble {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-end;
  gap: 8px;
}

.message-bubble.is-own {
  justify-content: flex-end;
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: bold;
}

.is-own .message-avatar {
  background: linear-gradient(135deg, #42a5f5 0%, #478ed1 100%);
}

.bubble-content {
  max-width: 60%;
  padding: 12px 16px;
  border-radius: 12px;
  background: #f0f0f0;
  position: relative;
}

/* Sender name for received messages */
.sender-name {
  font-size: 11px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 4px;
}

/* WhatsApp-style message tails */
.bubble-content::before {
  content: '';
  position: absolute;
  bottom: 0;
  width: 0;
  height: 0;
  border: 8px solid transparent;
}

.message-bubble:not(.is-own) .bubble-content::before {
  left: -8px;
  border-right-color: #f0f0f0;
  border-left: 0;
}

.message-bubble.is-own .bubble-content::before {
  right: -8px;
  border-left-color: #409eff;
  border-right: 0;
}

.is-own .bubble-content {
  background: #409eff;
  color: white;
}

.is-own .sender-name {
  color: rgba(255, 255, 255, 0.9);
}

.text-content {
  word-wrap: break-word;
  line-height: 1.5;
  white-space: pre-wrap;
}

.image-content {
  text-align: center;
}

.file-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-info {
  flex: 1;
}

.file-name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

.file-size {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #909399;
}

.is-own .file-size {
  color: rgba(255, 255, 255, 0.8);
}

.timestamp {
  margin-top: 4px;
  font-size: 11px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
  justify-content: flex-end;
}

.is-own .timestamp {
  color: rgba(255, 255, 255, 0.8);
}
</style>