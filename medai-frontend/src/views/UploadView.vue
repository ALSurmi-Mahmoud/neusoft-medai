<template>
  <div class="upload-view">
    <el-card>
      <template #header>
        <span>Upload Medical Images</span>
      </template>

      <el-alert
          title="Supported Formats"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
      >
        DICOM (.dcm), NIfTI (.nii, .nii.gz), JPEG, PNG
      </el-alert>

      <!-- Upload Area -->
      <el-upload
          ref="uploadRef"
          class="upload-area"
          drag
          multiple
          :auto-upload="false"
          :file-list="fileList"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          accept=".dcm,.dicom,.nii,.nii.gz,.jpg,.jpeg,.png"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          Drop files here or <em>click to select</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            Upload DICOM files or medical image series. Maximum 500MB per file.
          </div>
        </template>
      </el-upload>

      <!-- File List Preview -->
      <div v-if="fileList.length > 0" class="file-list-section">
        <h4>Selected Files ({{ fileList.length }})</h4>
        <el-table :data="fileList" size="small" max-height="300">
          <el-table-column prop="name" label="File Name" />
          <el-table-column prop="size" label="Size" width="120">
            <template #default="{ row }">
              {{ formatFileSize(row.size) }}
            </template>
          </el-table-column>
          <el-table-column label="Status" width="120">
            <template #default="{ row }">
              <el-tag :type="getFileStatusType(row.status)" size="small">
                {{ row.status || 'Ready' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Upload Progress -->
      <div v-if="uploading" class="upload-progress">
        <el-progress
            :percentage="uploadProgress"
            :status="uploadProgress === 100 ? 'success' : ''"
        />
        <p>Uploading {{ currentFileIndex + 1 }} of {{ fileList.length }} files...</p>
      </div>

      <!-- Action Buttons -->
      <div class="upload-actions">
        <el-button @click="clearFiles" :disabled="uploading">
          Clear All
        </el-button>
        <el-button
            type="primary"
            @click="startUpload"
            :loading="uploading"
            :disabled="fileList.length === 0"
        >
          <el-icon><Upload /></el-icon>
          {{ uploading ? 'Uploading...' : 'Upload Files' }}
        </el-button>
      </div>

      <!-- Upload Results -->
      <div v-if="uploadResult" class="upload-result">
        <el-result
            :icon="uploadResult.success ? 'success' : 'warning'"
            :title="uploadResult.success ? 'Upload Complete' : 'Upload Completed with Errors'"
            :sub-title="uploadResult.message"
        >
          <template #extra>
            <el-button type="primary" @click="viewStudy" v-if="uploadResult.studyId">
              View Study
            </el-button>
            <el-button @click="resetUpload">Upload More</el-button>
          </template>
        </el-result>

        <el-collapse v-if="uploadResult.files?.length > 0" style="margin-top: 20px;">
          <el-collapse-item title="Upload Details">
            <el-table :data="uploadResult.files" size="small">
              <el-table-column prop="originalFilename" label="File" />
              <el-table-column prop="success" label="Status" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.success ? 'success' : 'danger'" size="small">
                    {{ row.success ? 'Success' : 'Failed' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="instanceUid" label="Instance UID">
                <template #default="{ row }">
                  <span class="uid-text">{{ row.instanceUid || row.error || '-' }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../utils/http'
import { ElMessage } from 'element-plus'
import { UploadFilled, Upload } from '@element-plus/icons-vue'

export default {
  name: 'UploadView',
  components: { UploadFilled, Upload },
  setup() {
    const router = useRouter()
    const uploadRef = ref(null)

    const fileList = ref([])
    const uploading = ref(false)
    const uploadProgress = ref(0)
    const currentFileIndex = ref(0)
    const uploadResult = ref(null)

    const handleFileChange = (file, files) => {
      fileList.value = files
    }

    const handleFileRemove = (file, files) => {
      fileList.value = files
    }

    const clearFiles = () => {
      fileList.value = []
      uploadRef.value?.clearFiles()
      uploadResult.value = null
    }

    const startUpload = async () => {
      if (fileList.value.length === 0) return

      uploading.value = true
      uploadProgress.value = 0
      uploadResult.value = null

      const formData = new FormData()

      // Add all files to form data
      fileList.value.forEach(file => {
        formData.append('files', file.raw)
      })

      try {
        const response = await http.post('/images/upload-multiple', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          },
          onUploadProgress: (progressEvent) => {
            uploadProgress.value = Math.round(
                (progressEvent.loaded * 100) / progressEvent.total
            )
          }
        })

        uploadResult.value = response.data

        if (response.data.success) {
          ElMessage.success(`Successfully uploaded ${response.data.filesSuccessful} files`)
        } else {
          ElMessage.warning(`Upload completed: ${response.data.filesSuccessful} success, ${response.data.filesFailed} failed`)
        }

      } catch (error) {
        console.error('Upload failed:', error)
        ElMessage.error('Upload failed: ' + (error.response?.data?.message || error.message))
        uploadResult.value = {
          success: false,
          message: error.response?.data?.message || 'Upload failed'
        }
      } finally {
        uploading.value = false
      }
    }

    const viewStudy = () => {
      if (uploadResult.value?.studyId) {
        router.push(`/studies/${uploadResult.value.studyId}`)
      }
    }

    const resetUpload = () => {
      clearFiles()
      uploadProgress.value = 0
      currentFileIndex.value = 0
    }

    const formatFileSize = (bytes) => {
      if (!bytes) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    const getFileStatusType = (status) => {
      const types = {
        'success': 'success',
        'error': 'danger',
        'uploading': 'warning'
      }
      return types[status] || 'info'
    }

    return {
      uploadRef,
      fileList,
      uploading,
      uploadProgress,
      currentFileIndex,
      uploadResult,
      handleFileChange,
      handleFileRemove,
      clearFiles,
      startUpload,
      viewStudy,
      resetUpload,
      formatFileSize,
      getFileStatusType
    }
  }
}
</script>

<style scoped>
.upload-view {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.upload-area {
  width: 100%;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  height: 200px;
}

.file-list-section {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.upload-progress {
  margin-top: 20px;
  text-align: center;
}

.upload-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.upload-result {
  margin-top: 30px;
}

.uid-text {
  font-family: monospace;
  font-size: 12px;
}
</style>