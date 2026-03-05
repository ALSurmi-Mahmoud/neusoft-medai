<template>
  <div class="global-search-view">
    <!-- Search Header -->
    <el-card class="search-header-card" shadow="never">
      <div class="search-header">
        <h2>
          <el-icon><Search /></el-icon>
          Search Results
        </h2>
        <el-space>
          <el-button @click="showAdvancedDialog = true" :icon="Filter">
            Advanced Filters
          </el-button>
          <el-button
              v-if="currentQuery"
              @click="saveCurrentSearch"
              :icon="Star"
              :loading="saving"
          >
            Save Search
          </el-button>
        </el-space>
      </div>

      <!-- Search Input -->
      <el-input
          v-model="searchQuery"
          size="large"
          placeholder="Search patients, studies, reports, treatments..."
          :prefix-icon="Search"
          clearable
          @keydown.enter="performSearch"
          class="main-search-input"
      >
        <template #append>
          <el-button
              :loading="searching"
              @click="performSearch"
              type="primary"
          >
            Search
          </el-button>
        </template>
      </el-input>

      <!-- Applied Filters -->
      <div v-if="appliedFilters.length > 0" class="applied-filters">
        <el-space wrap>
          <span class="filter-label">Applied Filters:</span>
          <el-tag
              v-for="(filter, index) in appliedFilters"
              :key="index"
              closable
              @close="removeFilter(filter)"
              type="info"
          >
            {{ filter.label }}: {{ filter.value }}
          </el-tag>
          <el-button size="small" text @click="clearAllFilters">
            Clear All
          </el-button>
        </el-space>
      </div>
    </el-card>

    <!-- Results Summary -->
    <el-card v-if="searched" class="results-summary-card" shadow="never">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-box" @click="activeTab = 'all'">
            <div class="stat-value">{{ totalResults }}</div>
            <div class="stat-label">Total Results</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box" :class="{ active: activeTab === 'patients' }" @click="activeTab = 'patients'">
            <div class="stat-value">{{ patientCount }}</div>
            <div class="stat-label">Patients</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box" :class="{ active: activeTab === 'studies' }" @click="activeTab = 'studies'">
            <div class="stat-value">{{ studyCount }}</div>
            <div class="stat-label">Studies</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box" :class="{ active: activeTab === 'reports' }" @click="activeTab = 'reports'">
            <div class="stat-value">{{ reportCount }}</div>
            <div class="stat-label">Reports</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- Results Tabs -->
    <el-card v-if="searched" class="results-card">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <!-- All Results Tab -->
        <el-tab-pane label="All Results" name="all">
          <div v-if="totalResults === 0" class="empty-state">
            <el-empty description="No results found">
              <el-button type="primary" @click="performSearch">
                Try Again
              </el-button>
            </el-empty>
          </div>

          <div v-else>
            <!-- Patients Section -->
            <div v-if="results.patients && results.patients.length > 0" class="results-section">
              <div class="section-header">
                <h3>
                  <el-icon><User /></el-icon>
                  Patients ({{ results.patients.length }})
                </h3>
                <el-button text @click="activeTab = 'patients'">
                  View All
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
              <PatientResultsList :patients="results.patients.slice(0, 5)" />
            </div>

            <!-- Studies Section -->
            <div v-if="results.studies && results.studies.length > 0" class="results-section">
              <div class="section-header">
                <h3>
                  <el-icon><FolderOpened /></el-icon>
                  Studies ({{ results.studies.length }})
                </h3>
                <el-button text @click="activeTab = 'studies'">
                  View All
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
              <StudyResultsList :studies="results.studies.slice(0, 5)" />
            </div>

            <!-- Reports Section -->
            <div v-if="results.reports && results.reports.length > 0" class="results-section">
              <div class="section-header">
                <h3>
                  <el-icon><Files /></el-icon>
                  Reports ({{ results.reports.length }})
                </h3>
                <el-button text @click="activeTab = 'reports'">
                  View All
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
              <ReportResultsList :reports="results.reports.slice(0, 5)" />
            </div>

            <!-- Treatments Section -->
            <div v-if="results.treatments && results.treatments.length > 0" class="results-section">
              <div class="section-header">
                <h3>
                  <el-icon><Checked /></el-icon>
                  Treatment Plans ({{ results.treatments.length }})
                </h3>
                <el-button text @click="activeTab = 'treatments'">
                  View All
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
              <TreatmentResultsList :treatments="results.treatments.slice(0, 5)" />
            </div>
          </div>
        </el-tab-pane>

        <!-- Individual Entity Tabs -->
        <el-tab-pane :label="`Patients (${patientCount})`" name="patients">
          <PatientResultsList
              v-if="results.patients && results.patients.length > 0"
              :patients="results.patients"
              show-all
          />
          <el-empty v-else description="No patients found" />
        </el-tab-pane>

        <el-tab-pane :label="`Studies (${studyCount})`" name="studies">
          <StudyResultsList
              v-if="results.studies && results.studies.length > 0"
              :studies="results.studies"
              show-all
          />
          <el-empty v-else description="No studies found" />
        </el-tab-pane>

        <el-tab-pane :label="`Reports (${reportCount})`" name="reports">
          <ReportResultsList
              v-if="results.reports && results.reports.length > 0"
              :reports="results.reports"
              show-all
          />
          <el-empty v-else description="No reports found" />
        </el-tab-pane>

        <el-tab-pane :label="`Treatments (${treatmentCount})`" name="treatments">
          <TreatmentResultsList
              v-if="results.treatments && results.treatments.length > 0"
              :treatments="results.treatments"
              show-all
          />
          <el-empty v-else description="No treatment plans found" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Loading State -->
    <el-card v-if="searching" v-loading="searching" element-loading-text="Searching...">
      <div style="height: 300px"></div>
    </el-card>

    <!-- Initial State -->
    <el-card v-if="!searched && !searching" class="initial-state">
      <el-empty description="Enter a search query to get started">
        <el-space direction="vertical" alignment="center" :size="20">
          <el-text>Quick tips:</el-text>
          <el-space wrap>
            <el-tag>Patient names</el-tag>
            <el-tag>Study descriptions</el-tag>
            <el-tag>Report titles</el-tag>
            <el-tag>Accession numbers</el-tag>
          </el-space>
        </el-space>
      </el-empty>
    </el-card>

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

    <!-- Save Search Dialog -->
    <el-dialog
        v-model="showSaveDialog"
        title="Save Search"
        width="500px"
    >
      <el-form :model="saveForm" label-position="top">
        <el-form-item label="Search Name" required>
          <el-input
              v-model="saveForm.name"
              placeholder="Enter a name for this search"
              maxlength="100"
              show-word-limit
          />
        </el-form-item>
        <el-form-item label="Description">
          <el-input
              v-model="saveForm.description"
              type="textarea"
              :rows="3"
              placeholder="Optional description"
              maxlength="500"
              show-word-limit
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="saveForm.isPublic">
            Make this search public (visible to all users)
          </el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-space>
          <el-button @click="showSaveDialog = false">Cancel</el-button>
          <el-button
              type="primary"
              @click="confirmSaveSearch"
              :loading="saving"
              :disabled="!saveForm.name"
          >
            Save
          </el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Search, Filter, Star, User, FolderOpened, Files, Checked, ArrowRight
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '@/utils/http'
import AdvancedSearchForm from '@/components/search/AdvancedSearchForm.vue'
import PatientResultsList from '@/components/search/PatientResultsList.vue'
import StudyResultsList from '@/components/search/StudyResultsList.vue'
import ReportResultsList from '@/components/search/ReportResultsList.vue'
import TreatmentResultsList from '@/components/search/TreatmentResultsList.vue'

const route = useRoute()
const router = useRouter()

// State
const searchQuery = ref('')
const currentQuery = ref('')
const searching = ref(false)
const searched = ref(false)
const saving = ref(false)
const showAdvancedDialog = ref(false)
const showSaveDialog = ref(false)
const activeTab = ref('all')

const results = reactive({
  patients: [],
  studies: [],
  reports: [],
  treatments: []
})

const appliedFilters = ref([])

const saveForm = reactive({
  name: '',
  description: '',
  isPublic: false
})

// Computed
const totalResults = computed(() => {
  return patientCount.value + studyCount.value + reportCount.value + treatmentCount.value
})

const patientCount = computed(() => results.patients?.length || 0)
const studyCount = computed(() => results.studies?.length || 0)
const reportCount = computed(() => results.reports?.length || 0)
const treatmentCount = computed(() => results.treatments?.length || 0)

// Methods
const performSearch = async () => {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('Please enter a search query')
    return
  }

  searching.value = true
  searched.value = false
  currentQuery.value = searchQuery.value

  try {
    const response = await http.get('/search/global', {
      params: {
        query: searchQuery.value,
        limit: 50
      }
    })

    results.patients = response.data.patients || []
    results.studies = response.data.studies || []
    results.reports = response.data.reports || []
    results.treatments = response.data.treatments || []

    searched.value = true

    // Update URL
    router.push({
      path: '/search',
      query: { q: searchQuery.value }
    })
  } catch (error) {
    console.error('Search failed:', error)
    ElMessage.error(error.response?.data?.message || 'Search failed')
  } finally {
    searching.value = false
  }
}

const handleAdvancedSearch = async (searchParams) => {
  showAdvancedDialog.value = false
  searching.value = true
  searched.value = false

  try {
    const response = await http.post('/search/advanced', {
      entityType: searchParams.entityType,
      query: searchParams.query,
      filters: searchParams.filters,
      page: 0,
      size: 50
    })

    // Update results based on entity type
    if (searchParams.entityType === 'all') {
      results.patients = response.data.patients?.results || []
      results.studies = response.data.studies?.results || []
      results.reports = response.data.reports?.results || []
      results.treatments = response.data.treatments?.results || []
    } else {
      // Clear other results
      results.patients = []
      results.studies = []
      results.reports = []
      results.treatments = []

      // Set the specific entity results
      const entityKey = searchParams.entityType
      if (response.data.results) {
        results[entityKey] = response.data.results
      }

      // Switch to specific tab
      activeTab.value = entityKey
    }

    // Update applied filters
    updateAppliedFilters(searchParams.filters)

    searched.value = true
    searchQuery.value = searchParams.query
    currentQuery.value = searchParams.query
  } catch (error) {
    console.error('Advanced search failed:', error)
    ElMessage.error(error.response?.data?.message || 'Search failed')
  } finally {
    searching.value = false
  }
}

const updateAppliedFilters = (filters) => {
  appliedFilters.value = []
  Object.keys(filters).forEach(key => {
    if (filters[key] !== null && filters[key] !== undefined) {
      appliedFilters.value.push({
        key,
        label: formatFilterLabel(key),
        value: formatFilterValue(filters[key])
      })
    }
  })
}

const formatFilterLabel = (key) => {
  const labels = {
    sex: 'Gender',
    bloodType: 'Blood Type',
    minAge: 'Min Age',
    maxAge: 'Max Age',
    modality: 'Modality',
    studyStatus: 'Study Status',
    reportStatus: 'Report Status',
    finalized: 'Finalized',
    treatmentStatus: 'Treatment Status',
    priority: 'Priority',
    startDate: 'Start Date',
    endDate: 'End Date'
  }
  return labels[key] || key
}

const formatFilterValue = (value) => {
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No'
  }
  if (value instanceof Date) {
    return value.toLocaleDateString()
  }
  return value.toString()
}

const removeFilter = (filter) => {
  appliedFilters.value = appliedFilters.value.filter(f => f.key !== filter.key)
  // Could re-trigger search here
}

const clearAllFilters = () => {
  appliedFilters.value = []
  performSearch()
}

const saveCurrentSearch = () => {
  saveForm.name = `Search: ${currentQuery.value.substring(0, 50)}`
  saveForm.description = ''
  saveForm.isPublic = false
  showSaveDialog.value = true
}

const confirmSaveSearch = async () => {
  if (!saveForm.name.trim()) {
    ElMessage.warning('Please enter a search name')
    return
  }

  saving.value = true

  try {
    const criteria = {
      query: currentQuery.value,
      filters: appliedFilters.value.reduce((acc, filter) => {
        acc[filter.key] = filter.value
        return acc
      }, {})
    }

    await http.post('/saved-searches', {
      name: saveForm.name,
      description: saveForm.description,
      entityType: activeTab.value === 'all' ? 'all' : activeTab.value,
      criteria: JSON.stringify(criteria),
      isPublic: saveForm.isPublic
    })

    ElMessage.success('Search saved successfully')
    showSaveDialog.value = false
  } catch (error) {
    console.error('Save search failed:', error)
    ElMessage.error(error.response?.data?.message || 'Failed to save search')
  } finally {
    saving.value = false
  }
}

const handleTabClick = (tab) => {
  // Could add analytics here
}

// Initialize from route query
onMounted(() => {
  if (route.query.q) {
    searchQuery.value = route.query.q
    performSearch()
  } else if (route.query.advanced) {
    try {
      const params = JSON.parse(route.query.advanced)
      handleAdvancedSearch(params)
    } catch (e) {
      console.error('Failed to parse advanced search params:', e)
    }
  }
})

// Watch route changes
watch(() => route.query.q, (newQuery) => {
  if (newQuery && newQuery !== searchQuery.value) {
    searchQuery.value = newQuery
    performSearch()
  }
})
</script>

<style scoped>
.global-search-view {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.search-header-card {
  margin-bottom: 20px;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-header h2 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
}

.main-search-input {
  margin-bottom: 16px;
}

.applied-filters {
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.filter-label {
  font-weight: 500;
  color: var(--el-text-color-secondary);
}

.results-summary-card {
  margin-bottom: 20px;
}

.stat-box {
  text-align: center;
  padding: 20px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-box:hover,
.stat-box.active {
  background: var(--el-color-primary-light-9);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: var(--el-color-primary);
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.results-card {
  margin-bottom: 20px;
}

.results-section {
  margin-bottom: 32px;
}

.results-section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--el-border-color-light);
}

.section-header h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  color: var(--el-text-color-primary);
}

.empty-state,
.initial-state {
  padding: 60px 20px;
}
</style>