<template>
  <div class="saved-searches-view">
    <el-page-header @back="$router.back()" title="Back">
      <template #content>
        <h2>
          <el-icon><Star /></el-icon>
          Saved Searches
        </h2>
      </template>
    </el-page-header>

    <!-- Quick Filters Section -->
    <el-card class="quick-filters-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>Quick Filters</span>
          <el-tag type="info">{{ quickFilters.length }} filters</el-tag>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col
            v-for="filter in quickFilters"
            :key="filter.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
        >
          <el-card
              class="quick-filter-card"
              shadow="hover"
              @click="executeQuickFilter(filter)"
          >
            <div class="filter-content">
              <el-icon :size="32" :color="getEntityColor(filter.entityType)">
                <component :is="getEntityIcon(filter.entityType)" />
              </el-icon>
              <div class="filter-info">
                <div class="filter-name">{{ filter.name }}</div>
                <div class="filter-description">{{ filter.description }}</div>
              </div>
            </div>
            <div class="filter-meta">
              <el-tag size="small" :type="getEntityTagType(filter.entityType)">
                {{ formatEntityType(filter.entityType) }}
              </el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- My Saved Searches -->
    <el-card class="saved-searches-card">
      <template #header>
        <div class="card-header">
          <span>My Saved Searches</span>
          <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
            Create New
          </el-button>
        </div>
      </template>

      <el-table
          :data="savedSearches"
          v-loading="loading"
          stripe
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="Entity Type">
                  <el-tag :type="getEntityTagType(row.entityType)">
                    {{ formatEntityType(row.entityType) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="Execution Count">
                  {{ row.executionCount || 0 }}
                </el-descriptions-item>
                <el-descriptions-item label="Created">
                  {{ formatDateTime(row.createdAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="Last Executed">
                  {{ row.lastExecutedAt ? formatDateTime(row.lastExecutedAt) : 'Never' }}
                </el-descriptions-item>
                <el-descriptions-item label="Description" :span="2">
                  {{ row.description || 'No description' }}
                </el-descriptions-item>
                <el-descriptions-item label="Criteria" :span="2">
                  <el-tag v-for="(value, key) in parseCriteria(row.criteria)" :key="key" class="criteria-tag">
                    {{ key }}: {{ value }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Name" min-width="200">
          <template #default="{ row }">
            <div class="search-name">
              <el-icon><Search /></el-icon>
              {{ row.name }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Type" width="120">
          <template #default="{ row }">
            <el-tag :type="getEntityTagType(row.entityType)" size="small">
              {{ formatEntityType(row.entityType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Visibility" width="100">
          <template #default="{ row }">
            <el-icon v-if="row.isPublic" color="green" :size="18">
              <View />
            </el-icon>
            <el-icon v-else color="grey" :size="18">
              <Hide />
            </el-icon>
          </template>
        </el-table-column>

        <el-table-column label="Executions" width="100" align="center">
          <template #default="{ row }">
            <el-text type="info">{{ row.executionCount || 0 }}</el-text>
          </template>
        </el-table-column>

        <el-table-column label="Last Used" width="140">
          <template #default="{ row }">
            {{ row.lastExecutedAt ? formatDate(row.lastExecutedAt) : 'Never' }}
          </template>
        </el-table-column>

        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button
                  size="small"
                  type="primary"
                  @click="executeSearch(row)"
                  :icon="VideoPlay"
                  :loading="executing === row.id"
              >
                Execute
              </el-button>
              <el-button
                  size="small"
                  @click="editSearch(row)"
                  :icon="Edit"
                  circle
              />
              <el-button
                  size="small"
                  type="danger"
                  @click="confirmDelete(row)"
                  :icon="Delete"
                  circle
              />
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="savedSearches.length === 0 && !loading" class="empty-state">
        <el-empty description="No saved searches yet">
          <el-button type="primary" @click="showCreateDialog = true">
            Create Your First Search
          </el-button>
        </el-empty>
      </div>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog
        v-model="showCreateDialog"
        :title="editingSearch ? 'Edit Search' : 'Create Saved Search'"
        width="600px"
        :close-on-click-modal="false"
    >
      <el-form :model="searchForm" label-position="top" ref="formRef">
        <el-form-item label="Search Name" required>
          <el-input
              v-model="searchForm.name"
              placeholder="Enter a descriptive name"
              maxlength="100"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="Description">
          <el-input
              v-model="searchForm.description"
              type="textarea"
              :rows="3"
              placeholder="Describe what this search does"
              maxlength="500"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="Entity Type" required>
          <el-select v-model="searchForm.entityType" placeholder="Select" style="width: 100%">
            <el-option label="All" value="all" />
            <el-option label="Patients" value="patients" />
            <el-option label="Studies" value="studies" />
            <el-option label="Reports" value="reports" />
            <el-option label="Treatments" value="treatments" />
          </el-select>
        </el-form-item>

        <el-form-item label="Search Criteria (JSON)">
          <el-input
              v-model="searchForm.criteria"
              type="textarea"
              :rows="5"
              placeholder='{"query": "example", "filters": {...}}'
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="searchForm.isPublic">
            Make this search public
          </el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-space>
          <el-button @click="showCreateDialog = false">Cancel</el-button>
          <el-button
              type="primary"
              @click="saveSearch"
              :loading="saving"
          >
            {{ editingSearch ? 'Update' : 'Create' }}
          </el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Star, Plus, Search, VideoPlay, Edit, Delete, View, Hide,
  User, FolderOpened, Files, Checked
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/utils/http'

const router = useRouter()

// State
const loading = ref(false)
const saving = ref(false)
const executing = ref(null)
const showCreateDialog = ref(false)
const editingSearch = ref(null)
const savedSearches = ref([])
const quickFilters = ref([])

const searchForm = reactive({
  name: '',
  description: '',
  entityType: 'all',
  criteria: '',
  isPublic: false
})

// Load data
onMounted(() => {
  loadSavedSearches()
  loadQuickFilters()
})

const loadSavedSearches = async () => {
  loading.value = true
  try {
    const response = await http.get('/saved-searches')
    savedSearches.value = response.data
  } catch (error) {
    console.error('Failed to load saved searches:', error)
    ElMessage.error('Failed to load saved searches')
  } finally {
    loading.value = false
  }
}

const loadQuickFilters = async () => {
  try {
    const response = await http.get('/saved-searches/quick-filters')
    quickFilters.value = response.data
  } catch (error) {
    console.error('Failed to load quick filters:', error)
  }
}

const executeSearch = async (search) => {
  executing.value = search.id
  try {
    const response = await http.post(`/saved-searches/${search.id}/execute`)

    // Navigate to search results with data
    router.push({
      path: '/search',
      query: {
        savedSearchId: search.id,
        results: JSON.stringify(response.data)
      }
    })
  } catch (error) {
    console.error('Execute failed:', error)
    ElMessage.error('Failed to execute search')
  } finally {
    executing.value = null
  }
}

const executeQuickFilter = (filter) => {
  executeSearch(filter)
}

const editSearch = (search) => {
  editingSearch.value = search
  searchForm.name = search.name
  searchForm.description = search.description
  searchForm.entityType = search.entityType
  searchForm.criteria = search.criteria
  searchForm.isPublic = search.isPublic
  showCreateDialog.value = true
}

const saveSearch = async () => {
  if (!searchForm.name.trim()) {
    ElMessage.warning('Please enter a search name')
    return
  }

  saving.value = true
  try {
    const data = {
      name: searchForm.name,
      description: searchForm.description,
      entityType: searchForm.entityType,
      criteria: searchForm.criteria,
      isPublic: searchForm.isPublic
    }

    if (editingSearch.value) {
      await http.put(`/saved-searches/${editingSearch.value.id}`, data)
      ElMessage.success('Search updated successfully')
    } else {
      await http.post('/saved-searches', data)
      ElMessage.success('Search created successfully')
    }

    showCreateDialog.value = false
    resetForm()
    loadSavedSearches()
  } catch (error) {
    console.error('Save failed:', error)
    ElMessage.error('Failed to save search')
  } finally {
    saving.value = false
  }
}

const confirmDelete = (search) => {
  ElMessageBox.confirm(
      `Are you sure you want to delete "${search.name}"?`,
      'Delete Saved Search',
      {
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }
  ).then(async () => {
    try {
      await http.delete(`/saved-searches/${search.id}`)
      ElMessage.success('Search deleted successfully')
      loadSavedSearches()
    } catch (error) {
      console.error('Delete failed:', error)
      ElMessage.error('Failed to delete search')
    }
  })
}

const resetForm = () => {
  editingSearch.value = null
  searchForm.name = ''
  searchForm.description = ''
  searchForm.entityType = 'all'
  searchForm.criteria = ''
  searchForm.isPublic = false
}

// Utility functions
const getEntityIcon = (type) => {
  const icons = {
    patients: User,
    studies: FolderOpened,
    reports: Files,
    treatments: Checked
  }
  return icons[type] || Search
}

const getEntityColor = (type) => {
  const colors = {
    patients: '#409eff',
    studies: '#67c23a',
    reports: '#e6a23c',
    treatments: '#f56c6c'
  }
  return colors[type] || '#909399'
}

const getEntityTagType = (type) => {
  const types = {
    patients: 'primary',
    studies: 'success',
    reports: 'warning',
    treatments: 'danger'
  }
  return types[type] || ''
}

const formatEntityType = (type) => {
  return type ? type.charAt(0).toUpperCase() + type.slice(1) : 'All'
}

const parseCriteria = (criteria) => {
  try {
    const parsed = JSON.parse(criteria)
    return parsed.filters || parsed
  } catch {
    return {}
  }
}

const formatDateTime = (date) => {
  if (!date) return 'N/A'
  return new Date(date).toLocaleString()
}

const formatDate = (date) => {
  if (!date) return 'Never'
  return new Date(date).toLocaleDateString()
}
</script>

<style scoped>
.saved-searches-view {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.el-page-header {
  margin-bottom: 20px;
}

.el-page-header h2 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.quick-filters-card,
.saved-searches-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quick-filter-card {
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 16px;
}

.quick-filter-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.filter-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.filter-info {
  flex: 1;
  min-width: 0;
}

.filter-name {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 4px;
  color: var(--el-text-color-primary);
}

.filter-description {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.filter-meta {
  display: flex;
  justify-content: flex-end;
}

.search-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.expand-content {
  padding: 16px;
  background: var(--el-fill-color-lighter);
}

.criteria-tag {
  margin-right: 8px;
  margin-bottom: 4px;
}

.empty-state {
  padding: 60px 20px;
}
</style>