<template>
  <div class="message-input">
    <el-button @click="$refs.fileInput.click()" :disabled="disabled">
      <el-icon><Paperclip /></el-icon>
      Attach
    </el-button>
    <el-button @click="showTemplates = true" :disabled="disabled">
      <el-icon><Document /></el-icon>
      Template
    </el-button>

    <el-input
        v-model="message"
        type="textarea"
        :rows="2"
        placeholder="Type a message..."
        @keydown.enter.exact.prevent="send"
        :disabled="disabled"
        style="flex: 1;"
    />

    <el-button type="primary" @click="send" :disabled="disabled || !message.trim()">
      <el-icon><Promotion /></el-icon>
      Send
    </el-button>

    <input
        ref="fileInput"
        type="file"
        style="display: none;"
        @change="handleFileSelect"
        accept="image/*,.pdf,.doc,.docx"
    />

    <!-- Templates Dialog -->
    <MessageTemplates v-model="showTemplates" @select="applyTemplate" />
  </div>
</template>

<script>
import { ref } from 'vue'
import { Paperclip, Document, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MessageTemplates from './MessageTemplates.vue'

export default {
  name: 'MessageInput',
  components: { MessageTemplates },
  props: {
    disabled: { type: Boolean, default: false }
  },
  emits: ['send', 'upload'],
  setup(props, { emit }) {
    const message = ref('')
    const fileInput = ref(null)
    const showTemplates = ref(false)

    const send = () => {
      if (!message.value.trim() || props.disabled) return
      emit('send', message.value)
      message.value = ''
    }

    const handleFileSelect = (event) => {
      const file = event.target.files[0]
      if (!file) return

      // Validate file size (10MB)
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.error('File size cannot exceed 10MB')
        return
      }

      emit('upload', file)
      event.target.value = '' // Reset input
    }

    const applyTemplate = (template) => {
      message.value = template.content
      showTemplates.value = false
    }

    return {
      message, fileInput, showTemplates, send, handleFileSelect, applyTemplate,
      Paperclip, Document, Promotion
    }
  }
}
</script>

<style scoped>
.message-input { padding: 16px 20px; border-top: 1px solid #dcdfe6; display: flex; gap: 8px; align-items: flex-end; }
</style>