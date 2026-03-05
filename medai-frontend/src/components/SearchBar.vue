<template>
  <div class="search-bar-container">
    <el-autocomplete
        v-model="searchQuery"
        :fetch-suggestions="fetchSuggestions"
        :placeholder="placeholder"
        :prefix-icon="Search"
        :trigger-on-focus="false"
        clearable
        @select="handleSelect"
        @keydown.enter="handleEnter"
        class="search-autocomplete"
        popper-class="search-suggestions-popper"
    >
      <template #suffix>
        <div class="search-suffix">
          <el-tag v-if="!searchQuery" size="small" type="info" class="keyboard-hint">
            {{ isMac ? '⌘K' : 'Ctrl+K' }}
          </el-tag>
          <el-button
              v-if="searchQuery"
              :icon="Search"
              circle
              size="small"
              @click="performSearch"
              class="search-button"
          />
        </div>
      </template>

      <template #default="{ item }">
        <div class="suggestion-item">
          <el-icon :size="20" class="suggestion-icon">
            <component :is="getIconComponent(item.type)" />
          </el-icon>
          <div class="suggestion-content">
            <div class="suggestion-title">
              {{ item.name || item.title || item.accessionNumber }}
            </div>
            <div class="suggestion-subtitle">
              {{ getSuggestionSubtitle(item) }}
            </div>
          </div>
          <el-tag :type="getTypeColor(item.type)" size="small" class="suggestion-tag">
            {{ formatType(item.type) }}
          </el-tag>
        </div>
      </template>
    </el-autocomplete>

    <!-- Advanced Search Dialog -->
    <el-dialog
        v-model="showAdvancedDialog"
        title="Advanced Search"
        width="800px"
        :close-on-click-modal="false"
    >
      <AdvancedSearchForm
          @search="handleAdvancedSearch"
          @cancel="showAdvancedDialog = false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, User, Document, Files, Checked, FolderOpened } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '@/utils/http'
import AdvancedSearchForm from './search/AdvancedSearchForm.vue'

const router = useRouter()

// Props
const props = defineProps({
  placeholder: {
    type: String,
    default: 'Search patients, studies, reports... (Ctrl+K)'
  }
})

// State
const searchQuery = ref('')
const showAdvancedDialog = ref(false)
const isMac = ref(false)

// Detect OS
onMounted(() => {
  isMac.value = navigator.platform.toUpperCase().indexOf('MAC') >= 0
  setupKeyboardShortcut()
})

// Keyboard shortcut
const setupKeyboardShortcut = () => {
  const handleKeyDown = (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault()
      const input = document.querySelector('.search-autocomplete input')
      if (input) input.focus()
    }
  }

  document.addEventListener('keydown', handleKeyDown)

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeyDown)
  })
}

// Fetch suggestions from API
const fetchSuggestions = async (queryString, callback) => {
  if (!queryString || queryString.length < 2) {
    callback([])
    return
  }

  try {
    const response = await http.get('/search/suggestions', {
      params: { query: queryString, limit: 10 }
    })

    const suggestions = response.data.map(item => ({
      ...item,
      value: item.name || item.title || item.accessionNumber || queryString
    }))

    callback(suggestions)
  } catch (error) {
    console.error('Failed to fetch suggestions:', error)
    callback([])
  }
}

// Handle suggestion selection
const handleSelect = (item) => {
  if (!item || !item.type || !item.id) return

  const routes = {
    patient: `/doctor/patients/${item.id}`,
    study: `/studies/${item.id}`,
    report: `/reports/${item.id}`,
    treatment: `/doctor/treatment-plans/${item.id}`
  }

  if (routes[item.type]) {
    router.push(routes[item.type])
    searchQuery.value = ''
  }
}

// Handle Enter key
const handleEnter = () => {
  if (searchQuery.value.trim()) {
    performSearch()
  }
}

// Perform search
const performSearch = () => {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('Please enter a search query')
    return
  }

  router.push({
    path: '/search',
    query: { q: searchQuery.value }
  })
}

// Handle advanced search
const handleAdvancedSearch = (filters) => {
  showAdvancedDialog.value = false
  router.push({
    path: '/search',
    query: { advanced: JSON.stringify(filters) }
  })
}

// Get icon component
const getIconComponent = (type) => {
  const icons = {
    patient: User,
    study: FolderOpened,
    report: Files,
    treatment: Checked
  }
  return icons[type] || Document
}

// Get suggestion subtitle
const getSuggestionSubtitle = (item) => {
  switch (item.type) {
    case 'patient':
      return `ID: ${item.patientId || 'N/A'} | ${item.sex || 'Unknown'} | ${item.birthDate || 'N/A'}`
    case 'study':
      return `${item.modality || 'N/A'} | ${item.description || 'No description'} | ${formatDate(item.studyDate)}`
    case 'report':
      return `Status: ${item.status || 'N/A'} | ${formatDate(item.createdAt)}`
    case 'treatment':
      return `${item.status || 'N/A'} | Priority: ${item.priority || 'N/A'}`
    default:
      return 'Click to view details'
  }
}

// Get type color
const getTypeColor = (type) => {
  const colors = {
    patient: 'primary',
    study: 'success',
    report: 'warning',
    treatment: 'danger'
  }
  return colors[type] || ''
}

// Format type
const formatType = (type) => {
  const types = {
    patient: 'Patient',
    study: 'Study',
    report: 'Report',
    treatment: 'Treatment'
  }
  return types[type] || type
}

// Format date
const formatDate = (date) => {
  if (!date) return 'N/A'
  const d = new Date(date)
  return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.search-bar-container {
  width: 100%;
  max-width: 500px;
}

.search-autocomplete {
  width: 100%;
}

.search-autocomplete :deep(.el-input__wrapper) {
  border-radius: 20px;
  transition: all 0.3s;
}

.search-autocomplete :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset;
}

.search-suffix {
  display: flex;
  align-items: center;
  gap: 8px;
}

.keyboard-hint {
  font-size: 11px;
  opacity: 0.7;
}

.search-button {
  margin-left: 4px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.suggestion-icon {
  color: var(--el-color-primary);
  flex-shrink: 0;
}

.suggestion-content {
  flex: 1;
  min-width: 0;
}

.suggestion-title {
  font-weight: 500;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suggestion-subtitle {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suggestion-tag {
  flex-shrink: 0;
}
</style>

<style>
.search-suggestions-popper {
  width: 500px !important;
}

.search-suggestions-popper .el-autocomplete-suggestion__wrap {
  max-height: 400px;
}

.search-suggestions-popper .el-autocomplete-suggestion__list {
  padding: 8px 12px;
}

.search-suggestions-popper .el-autocomplete-suggestion li {
  padding: 0;
  margin: 4px 0;
  line-height: normal;
}

.search-suggestions-popper .el-autocomplete-suggestion li:hover {
  background-color: var(--el-fill-color-light);
  border-radius: 8px;
}
</style>