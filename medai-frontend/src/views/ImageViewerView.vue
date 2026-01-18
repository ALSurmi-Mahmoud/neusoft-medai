
<template>
  <div class="image-viewer">
    <!-- Toolbar -->
    <div class="viewer-toolbar">
      <div class="toolbar-left">
        <el-button-group>
          <el-button :type="currentTool === 'Wwwc' ? 'primary' : ''" @click="setTool('Wwwc')">
            <el-icon><Sunny /></el-icon>
            W/L
          </el-button>
          <el-button :type="currentTool === 'Zoom' ? 'primary' : ''" @click="setTool('Zoom')">
            <el-icon><ZoomIn /></el-icon>
            Zoom
          </el-button>
          <el-button :type="currentTool === 'Pan' ? 'primary' : ''" @click="setTool('Pan')">
            <el-icon><Rank /></el-icon>
            Pan
          </el-button>
          <el-button @click="resetView">
            <el-icon><RefreshRight /></el-icon>
            Reset
          </el-button>
        </el-button-group>

        <el-divider direction="vertical" />

        <el-button-group>
          <el-button @click="previousImage" :disabled="currentIndex <= 0">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <el-button disabled style="min-width: 100px;">
            {{ currentIndex + 1 }} / {{ imageIds.length || 1 }}
          </el-button>
          <el-button @click="nextImage" :disabled="currentIndex >= imageIds.length - 1">
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </el-button-group>
      </div>

      <div class="toolbar-right">
        <el-button type="success" @click="createReport">
          <el-icon><Document /></el-icon>
          Create Report
        </el-button>
        <el-button @click="$router.back()">
          <el-icon><Back /></el-icon>
          Back
        </el-button>
      </div>
    </div>

    <!-- Main Viewer Area -->
    <div class="viewer-content">
      <!-- Study Info Panel -->
      <div class="info-panel">
        <el-card size="small">
          <template #header>Study Information</template>
          <div class="info-item">
            <label>Patient ID:</label>
            <span>{{ studyInfo.patientId || '-' }}</span>
          </div>
          <div class="info-item">
            <label>Patient Name:</label>
            <span>{{ studyInfo.patientName || '-' }}</span>
          </div>
          <div class="info-item">
            <label>Modality:</label>
            <span>{{ studyInfo.modality || '-' }}</span>
          </div>
          <div class="info-item">
            <label>Study Date:</label>
            <span>{{ formatDate(studyInfo.studyDate) }}</span>
          </div>
          <div class="info-item">
            <label>Series:</label>
            <span>{{ studyInfo.seriesCount || 0 }}</span>
          </div>
          <div class="info-item">
            <label>Images:</label>
            <span>{{ studyInfo.instanceCount || 0 }}</span>
          </div>
        </el-card>

        <!-- Series List -->
        <el-card size="small" style="margin-top: 10px;">
          <template #header>Series</template>
          <div
              v-for="series in seriesList"
              :key="series.id"
              class="series-item"
              :class="{ active: selectedSeriesId === series.id }"
              @click="selectSeries(series)"
          >
            <div class="series-info">
              <span class="series-number">#{{ series.seriesNumber || '?' }}</span>
              <span class="series-modality">{{ series.modality }}</span>
            </div>
            <div class="series-desc">{{ series.description || 'No description' }}</div>
            <div class="series-count">{{ series.imageCount }} images</div>
          </div>
        </el-card>
      </div>

      <!-- Viewer Canvas -->
      <div class="viewer-canvas-container">
        <div
            ref="viewerElement"
            class="viewer-canvas"
            @wheel="handleScroll"
        >
          <!-- Loading State -->
          <!-- Cornerstone viewport will be rendered here -->
          <div v-if="loading" class="viewer-loading">
            <el-icon class="is-loading" :size="48"><Loading /></el-icon>
            <p>Loading study information...</p>
          </div>

          <!-- Show placeholder after loading completes -->
          <div v-if="!loading" class="viewer-placeholder" style="text-align: center; padding: 50px;">
            <el-icon :size="80" style="color: #e6a23c"><Warning /></el-icon>
            <h2 style="margin: 20px 0; color: #333;">DICOM Image Viewer - Coming Soon</h2>
            <p style="color: #606266; max-width: 600px; margin: 0 auto 10px;">
              Your medical images have been <strong style="color: #67c23a;">successfully uploaded</strong>
              and stored in the system.
            </p>
            <p style="color: #909399; margin: 10px auto; max-width: 600px;">
              The interactive DICOM viewer requires Cornerstone.js library integration.
              This advanced feature will be implemented in Phase 5 of the project.
            </p>
            <p style="margin: 20px 0; font-size: 14px; color: #909399;">
              <strong>Study ID:</strong> {{ studyInfo?.studyUid || 'N/A' }}
            </p>
            <el-button type="primary" size="large" style="margin-top: 20px" @click="$router.back()">
              <el-icon><ArrowLeft /></el-icon>
              Back to Studies
            </el-button>
          </div>

          <!-- Viewer Not Implemented Message -->

          <div v-if="!loading && !viewerImplemented" class="viewer-placeholder">
            <el-icon :size="64" style="color: #e6a23c"><Warning /></el-icon>
            <h3 style="margin: 20px 0; color: #333;">DICOM Image Viewer - Coming Soon</h3>
            <div style="max-width: 500px; text-align: center;">
              <p style="margin: 10px 0; color: #606266;">
                Your medical images have been <strong style="color: #67c23a;">successfully uploaded</strong>
                and stored in the system.
              </p>
              <p style="margin: 10px 0; color: #909399;">
                The interactive DICOM viewer requires Cornerstone.js library integration.
                This feature will be implemented in Phase 5.
              </p>
              <p style="margin: 10px 0; font-size: 14px; color: #909399;">
                Study ID: {{ studyInfo?.studyInstanceUid || 'Loading...' }}
              </p>
              <el-button type="primary" size="large" style="margin-top: 20px" @click="$router.back()">
                <el-icon><ArrowLeft /></el-icon>
                Back to Studies
              </el-button>
            </div>
          </div>
          <!-- Original placeholder for no images -->
          <!-- Image overlay info -->
          <div v-if="!loading && viewerImplemented && !hasImages" class="viewer-placeholder">
            <el-icon :size="64"><Picture /></el-icon>
            <p>No images available in this study</p>
            <p class="hint">Upload DICOM files to view medical images</p>
          </div>

          <!-- Image overlays (only show if viewer is implemented) -->
          <div v-if="hasImages && !loading && viewerImplemented" class="image-overlay top-left">
            <div>{{ studyInfo.patientId }}</div>
            <div>{{ studyInfo.patientName }}</div>
          </div>
          <div v-if="hasImages && !loading && viewerImplemented" class="image-overlay top-right">
            <div>{{ studyInfo.modality }}</div>
            <div>{{ formatDate(studyInfo.studyDate) }}</div>
          </div>
          <div v-if="hasImages && !loading && viewerImplemented" class="image-overlay bottom-left">
            <div>W: {{ windowWidth }} L: {{ windowLevel }}</div>
            <div>Zoom: {{ zoomLevel }}%</div>
          </div>
          <div v-if="hasImages && !loading && viewerImplemented" class="image-overlay bottom-right">
            <div>Image {{ currentIndex + 1 }} / {{ imageIds.length }}</div>
          </div>
        </div>

        <!-- Thumbnail Strip -->
        <div class="thumbnail-strip" v-if="imageIds.length > 1">
          <div
              v-for="(imageId, index) in imageIds.slice(0, 20)"
              :key="index"
              class="thumbnail"
              :class="{ active: currentIndex === index }"
              @click="goToImage(index)"
          >
            {{ index + 1 }}
          </div>
          <div v-if="imageIds.length > 20" class="thumbnail more">
            +{{ imageIds.length - 20 }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../utils/http'
import { ElMessage } from 'element-plus'

import {
  Sunny,
  ZoomIn,
  Rank,
  RefreshRight,
  ArrowLeft,
  ArrowRight,
  Document,
  Back,
  Loading,
  Picture,
  Warning,
} from '@element-plus/icons-vue'

export default {
  name: 'ImageViewerView',
  components: {
    Sunny,
    ZoomIn,
    Rank,
    RefreshRight,
    ArrowLeft,
    ArrowRight,
    Document,
    Back,
    Loading,
    Picture,
    Warning,
  },
  setup() {
    const route = useRoute()
    const router = useRouter()

    const viewerElement = ref(null)
    const loading = ref(true)
    const studyInfo = ref({})
    const seriesList = ref([])
    const selectedSeriesId = ref(null)
    const imageIds = ref([])
    const currentIndex = ref(0)
    const currentTool = ref('Wwwc')

    const windowWidth = ref(400)
    const windowLevel = ref(40)
    const zoomLevel = ref(100)

    const viewerImplemented = ref(false)
    const hasImages = computed(() => imageIds.value.length > 0)

    const loadStudy = async () => {
      const studyId = route.params.studyId
      if (!studyId) return

      loading.value = true

      try {
        const response = await http.get(`/studies/${studyId}`)
        studyInfo.value = response.data
        seriesList.value = response.data.series || []

        if (seriesList.value.length > 0) {
          selectSeries(seriesList.value[0])
        }
      } catch (error) {
        console.error('Failed to load study:', error)
        ElMessage.error('Failed to load study')
      } finally {
        loading.value = false
      }
    }

    const selectSeries = (series) => {
      selectedSeriesId.value = series.id
      imageIds.value = Array(series.imageCount || 1)
          .fill(null)
          .map((_, i) => `dicom://${series.seriesUid}/${i}`)
      currentIndex.value = 0
      initializeViewer()
    }

    const initializeViewer = () => {
      console.log('Viewer initialized for series:', selectedSeriesId.value)
      loading.value = false
    }

    const setTool = (toolName) => {
      currentTool.value = toolName
    }

    const resetView = () => {
      windowWidth.value = 400
      windowLevel.value = 40
      zoomLevel.value = 100
    }

    const previousImage = () => {
      if (currentIndex.value > 0) {
        currentIndex.value--
        loadCurrentImage()
      }
    }

    const nextImage = () => {
      if (currentIndex.value < imageIds.value.length - 1) {
        currentIndex.value++
        loadCurrentImage()
      }
    }

    const goToImage = (index) => {
      currentIndex.value = index
      loadCurrentImage()
    }

    const loadCurrentImage = () => {
      console.log('Loading image:', currentIndex.value)
    }

    const handleScroll = (event) => {
      event.preventDefault()
      if (event.deltaY < 0) previousImage()
      else nextImage()
    }

    const createReport = () => {
      router.push(`/reports/new/${route.params.studyId}`)
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleDateString()
    }

    onMounted(() => {
      loadStudy()
    })

    return {
      viewerElement,
      loading,
      studyInfo,
      seriesList,
      selectedSeriesId,
      imageIds,
      currentIndex,
      currentTool,
      windowWidth,
      windowLevel,
      zoomLevel,
      hasImages,
      viewerImplemented,
      selectSeries,
      setTool,
      resetView,
      previousImage,
      nextImage,
      goToImage,
      handleScroll,
      createReport,
      formatDate,
    }
  },
}
</script>


<style scoped>
.image-viewer {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background-color: #1a1a1a;
}

.viewer-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background-color: #2d2d2d;
  border-bottom: 1px solid #404040;
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.viewer-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.info-panel {
  width: 280px;
  padding: 10px;
  background-color: #2d2d2d;
  overflow-y: auto;
}

.info-panel .el-card {
  background-color: #363636;
  border: none;
  color: #fff;
}

.info-panel :deep(.el-card__header) {
  padding: 10px 15px;
  border-bottom: 1px solid #404040;
  font-weight: bold;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 13px;
}

.info-item label {
  color: #999;
}

.info-item span {
  color: #fff;
}

.series-item {
  padding: 10px;
  margin-bottom: 5px;
  background-color: #404040;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.series-item:hover {
  background-color: #4a4a4a;
}

.series-item.active {
  background-color: #409eff;
}

.series-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.series-number {
  font-weight: bold;
}

.series-modality {
  background-color: #606060;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.series-desc {
  font-size: 12px;
  color: #ccc;
  margin-bottom: 5px;
}

.series-count {
  font-size: 11px;
  color: #999;
}

.viewer-canvas-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #000;
}

.viewer-canvas {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.viewer-loading, .viewer-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666;
}

.viewer-placeholder .hint {
  font-size: 12px;
  color: #444;
  margin-top: 10px;
}

.image-overlay {
  position: absolute;
  padding: 10px;
  color: #fff;
  font-size: 12px;
  font-family: monospace;
  text-shadow: 1px 1px 2px #000;
}

.image-overlay.top-left { top: 10px; left: 10px; }
.image-overlay.top-right { top: 10px; right: 10px; text-align: right; }
.image-overlay.bottom-left { bottom: 10px; left: 10px; }
.image-overlay.bottom-right { bottom: 10px; right: 10px; text-align: right; }

.thumbnail-strip {
  display: flex;
  padding: 10px;
  background-color: #2d2d2d;
  overflow-x: auto;
  gap: 5px;
}

.thumbnail {
  min-width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #404040;
  border: 2px solid transparent;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.thumbnail:hover {
  background-color: #505050;
}

.thumbnail.active {
  border-color: #409eff;
  background-color: #409eff;
}

.thumbnail.more {
  background-color: #333;
  color: #999;
}
</style>