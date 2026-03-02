<template>
  <el-dropdown @command="handleCommand">
    <el-button type="primary" :loading="exporting">
      <el-icon><Download /></el-icon>
      Export
      <el-icon class="el-icon--right"><ArrowDown /></el-icon>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="pdf">
          <el-icon><Document /></el-icon> Export as PDF
        </el-dropdown-item>
        <el-dropdown-item command="docx">
          <el-icon><Edit /></el-icon> Export as DOCX
        </el-dropdown-item>
        <el-dropdown-item divided command="custom">
          <el-icon><Setting /></el-icon> Custom Export...
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script>
import { ref } from 'vue'
import { Download, Document, Edit, Setting, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../utils/http'

export default {
  name: 'QuickExportButton',
  components: { Download, Document, Edit, Setting, ArrowDown },
  props: {
    entityId: {
      type: Number,
      required: true
    },
    entityType: {
      type: String,
      required: true, // 'report', 'treatment-plan', 'clinical-note'
    }
  },
  emits: ['custom-export'],
  setup(props, { emit }) {
    const exporting = ref(false)

    const handleCommand = async (command) => {
      if (command === 'custom') {
        emit('custom-export')
        return
      }

      exporting.value = true

      try {
        let endpoint = ''
        if (props.entityType === 'report') {
          endpoint = `/export/report/${props.entityId}`
        } else if (props.entityType === 'treatment-plan') {
          endpoint = `/export/treatment-plan/${props.entityId}`
        } else if (props.entityType === 'clinical-note') {
          endpoint = `/export/clinical-note/${props.entityId}`
        }

        const res = await http.post(endpoint, null, {
          params: { format: command }
        })

        // Auto-download
        const downloadRes = await http.get(`/export/download/${res.data.exportId}`, {
          responseType: 'blob'
        })

        const url = window.URL.createObjectURL(new Blob([downloadRes.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', res.data.fileName)
        document.body.appendChild(link)
        link.click()
        link.remove()

        ElMessage.success(`Exported as ${command.toUpperCase()}`)

      } catch (error) {
        console.error('Export failed:', error)
        ElMessage.error(error.response?.data?.message || 'Export failed')
      } finally {
        exporting.value = false
      }
    }

    return {
      exporting,
      handleCommand
    }
  }
}
</script>