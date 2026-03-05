<template>
  <div class="advanced-search-form">
    <el-form :model="form" label-position="top" @submit.prevent="handleSubmit">
      <!-- Entity Type Selection -->
      <el-form-item label="Search In">
        <el-radio-group v-model="form.entityType" size="large">
          <el-radio-button label="all">All</el-radio-button>
          <el-radio-button label="patients">Patients</el-radio-button>
          <el-radio-button label="studies">Studies</el-radio-button>
          <el-radio-button label="reports">Reports</el-radio-button>
          <el-radio-button label="treatments">Treatments</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- Search Query -->
      <el-form-item label="Keywords">
        <el-input
            v-model="form.query"
            placeholder="Enter search keywords..."
            :prefix-icon="Search"
            clearable
        />
      </el-form-item>

      <!-- Dynamic Filters based on Entity Type -->
      <el-divider content-position="left">Filters</el-divider>

      <!-- Patient Filters -->
      <template v-if="form.entityType === 'patients' || form.entityType === 'all'">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Gender">
              <el-select v-model="form.filters.sex" placeholder="Select" clearable>
                <el-option label="Male" value="M" />
                <el-option label="Female" value="F" />
                <el-option label="Other" value="O" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Blood Type">
              <el-select v-model="form.filters.bloodType" placeholder="Select" clearable>
                <el-option v-for="type in bloodTypes" :key="type" :label="type" :value="type" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Min Age">
              <el-input-number v-model="form.filters.minAge" :min="0" :max="150" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Max Age">
              <el-input-number v-model="form.filters.maxAge" :min="0" :max="150" />
            </el-form-item>
          </el-col>
        </el-row>
      </template>

      <!-- Study Filters -->
      <template v-if="form.entityType === 'studies' || form.entityType === 'all'">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Modality">
              <el-select v-model="form.filters.modality" placeholder="Select" clearable>
                <el-option v-for="mod in modalities" :key="mod" :label="mod" :value="mod" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Study Status">
              <el-select v-model="form.filters.studyStatus" placeholder="Select" clearable>
                <el-option label="Pending" value="pending" />
                <el-option label="In Progress" value="in_progress" />
                <el-option label="Completed" value="completed" />
                <el-option label="Cancelled" value="cancelled" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </template>

      <!-- Report Filters -->
      <template v-if="form.entityType === 'reports' || form.entityType === 'all'">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Report Status">
              <el-select v-model="form.filters.reportStatus" placeholder="Select" clearable>
                <el-option label="Draft" value="draft" />
                <el-option label="Finalized" value="finalized" />
                <el-option label="Amended" value="amended" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Finalized">
              <el-select v-model="form.filters.finalized" placeholder="Select" clearable>
                <el-option label="Yes" :value="true" />
                <el-option label="No" :value="false" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </template>

      <!-- Treatment Filters -->
      <template v-if="form.entityType === 'treatments' || form.entityType === 'all'">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Status">
              <el-select v-model="form.filters.treatmentStatus" placeholder="Select" clearable>
                <el-option label="Active" value="active" />
                <el-option label="Completed" value="completed" />
                <el-option label="Cancelled" value="cancelled" />
                <el-option label="On Hold" value="on_hold" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Priority">
              <el-select v-model="form.filters.priority" placeholder="Select" clearable>
                <el-option label="Low" value="low" />
                <el-option label="Medium" value="medium" />
                <el-option label="High" value="high" />
                <el-option label="Urgent" value="urgent" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </template>

      <!-- Date Range -->
      <el-form-item label="Date Range">
        <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            range-separator="To"
            start-placeholder="Start date"
            end-placeholder="End date"
            style="width: 100%"
        />
      </el-form-item>

      <!-- Actions -->
      <el-form-item>
        <el-space>
          <el-button type="primary" @click="handleSubmit" :icon="Search">
            Search
          </el-button>
          <el-button @click="handleReset" :icon="RefreshLeft">
            Reset
          </el-button>
          <el-button @click="handleCancel">
            Cancel
          </el-button>
        </el-space>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Search, RefreshLeft } from '@element-plus/icons-vue'

const emit = defineEmits(['search', 'cancel'])

const bloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']
const modalities = ['CT', 'MR', 'US', 'XR', 'PT', 'NM', 'DX', 'CR', 'MG', 'RF']

const form = reactive({
  entityType: 'all',
  query: '',
  dateRange: null,
  filters: {
    // Patient filters
    sex: null,
    bloodType: null,
    minAge: null,
    maxAge: null,
    // Study filters
    modality: null,
    studyStatus: null,
    // Report filters
    reportStatus: null,
    finalized: null,
    // Treatment filters
    treatmentStatus: null,
    priority: null
  }
})

const handleSubmit = () => {
  // Clean up filters - remove null/undefined values
  const cleanFilters = {}
  Object.keys(form.filters).forEach(key => {
    if (form.filters[key] !== null && form.filters[key] !== undefined) {
      cleanFilters[key] = form.filters[key]
    }
  })

  // Add date range if present
  if (form.dateRange && form.dateRange.length === 2) {
    cleanFilters.startDate = form.dateRange[0].toISOString()
    cleanFilters.endDate = form.dateRange[1].toISOString()
  }

  emit('search', {
    entityType: form.entityType,
    query: form.query,
    filters: cleanFilters
  })
}

const handleReset = () => {
  form.entityType = 'all'
  form.query = ''
  form.dateRange = null
  form.filters = {
    sex: null,
    bloodType: null,
    minAge: null,
    maxAge: null,
    modality: null,
    studyStatus: null,
    reportStatus: null,
    finalized: null,
    treatmentStatus: null,
    priority: null
  }
}

const handleCancel = () => {
  emit('cancel')
}
</script>

<style scoped>
.advanced-search-form {
  padding: 20px 0;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-input-number {
  width: 100%;
}
</style>