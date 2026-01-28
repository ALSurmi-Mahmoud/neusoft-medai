<template>
  <div class="medication-search">
    <el-autocomplete
        v-model="searchQuery"
        :fetch-suggestions="searchMedications"
        :placeholder="placeholder"
        :trigger-on-focus="false"
        :loading="loading"
        clearable
        @select="handleSelect"
        style="width: 100%;"
    >
      <template #prefix>
        <el-icon v-if="!loading"><Search /></el-icon>
        <el-icon v-else class="is-loading"><Loading /></el-icon>
      </template>
      <template #default="{ item }">
        <div class="medication-item">
          <div class="med-header">
            <strong>{{ item.name }}</strong>
            <el-tag size="small" type="info">{{ item.drugClass }}</el-tag>
          </div>
          <div class="med-details">
            <span v-if="item.genericName" class="generic">{{ item.genericName }}</span>
            <span v-if="item.brandName" class="brand">{{ item.brandName }}</span>
          </div>
          <div class="med-dosage" v-if="item.defaultDosage">
            <el-icon><FirstAidKit /></el-icon>
            {{ item.defaultDosage }}
          </div>
        </div>
      </template>
      <!-- loading template -->
      <template #loading>
        <div class="loading-text">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>Searching medications...</span>
        </div>
      </template>
      <!-- Add empty template -->
      <template #empty>
        <div class="empty-text">
          <el-icon><Search /></el-icon>
          <span>No medications found</span>
        </div>
      </template>
    </el-autocomplete>

    <!-- Selected Medication Details -->
    <el-card
        v-if="selectedMedication"
        class="med-detail-card"
        shadow="hover"
        style="margin-top: 10px;"
        v-loading="loadingDetails"
    >
      <div class="selected-med-info">
        <div class="med-title">
          <h4>{{ selectedMedication.name }}</h4>
          <el-tag type="success" size="small">Selected</el-tag>
        </div>

        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="Generic Name">
            {{ selectedMedication.genericName || 'N/A' }}
          </el-descriptions-item>
          <el-descriptions-item label="Brand Name">
            {{ selectedMedication.brandName || 'N/A' }}
          </el-descriptions-item>
          <el-descriptions-item label="Drug Class" :span="2">
            {{ selectedMedication.drugClass }}
          </el-descriptions-item>
          <el-descriptions-item label="Default Dosage" :span="2">
            {{ selectedMedication.defaultDosage }}
          </el-descriptions-item>
        </el-descriptions>

        <el-collapse accordion style="margin-top: 10px;">
          <el-collapse-item title="Available Forms & Strengths" name="1">
            <div class="forms-strengths">
              <div v-if="selectedMedication.dosageForms">
                <strong>Forms:</strong>
                <el-tag v-for="form in selectedMedication.dosageForms" :key="form" size="small" style="margin: 2px;">
                  {{ form }}
                </el-tag>
              </div>
              <div v-if="selectedMedication.strengths" style="margin-top: 8px;">
                <strong>Strengths:</strong>
                <el-tag v-for="strength in selectedMedication.strengths" :key="strength" size="small" type="info" style="margin: 2px;">
                  {{ strength }}
                </el-tag>
              </div>
            </div>
          </el-collapse-item>

          <el-collapse-item title="Drug Information" name="2">
            <div class="drug-info">
              <p v-if="selectedMedication.description"><strong>Description:</strong> {{ selectedMedication.description }}</p>
              <p v-if="selectedMedication.indications"><strong>Indications:</strong> {{ selectedMedication.indications }}</p>
            </div>
          </el-collapse-item>

          <el-collapse-item title="Warnings & Contraindications" name="3">
            <div class="warnings">
              <el-alert
                  v-if="selectedMedication.warnings"
                  type="warning"
                  :title="selectedMedication.warnings"
                  :closable="false"
                  show-icon
                  style="margin-bottom: 10px;"
              />
              <p v-if="selectedMedication.contraindications">
                <strong>Contraindications:</strong> {{ selectedMedication.contraindications }}
              </p>
              <p v-if="selectedMedication.sideEffects">
                <strong>Common Side Effects:</strong> {{ selectedMedication.sideEffects }}
              </p>
            </div>
          </el-collapse-item>
        </el-collapse>

        <div class="med-actions" style="margin-top: 10px; text-align: right;">
          <el-button size="small" @click="clearSelection">
            <el-icon><Close /></el-icon> Clear Selection
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, watch } from 'vue'
import http from '../utils/http'
import { Memo, Close, Search, Loading } from '@element-plus/icons-vue'

export default {
  name: 'MedicationSearchAutocomplete',
  components: { Memo, Close, Search, Loading },
  props: {
    modelValue: {
      type: Object,
      default: null
    },
    placeholder: {
      type: String,
      default: 'Search medications (generic or brand name)...'
    }
  },
  emits: ['update:modelValue', 'select'],
  setup(props, { emit }) {
    const searchQuery = ref('')
    const selectedMedication = ref(props.modelValue)
    const loading = ref(false)
    const loadingDetails = ref(false)

    // Watch for external changes
    watch(() => props.modelValue, (newVal) => {
      selectedMedication.value = newVal
      if (newVal) {
        searchQuery.value = newVal.name
      }
    })

    const searchMedications = async (queryString, callback) => {
      if (!queryString || queryString.length < 2) {
        callback([])
        return
      }

      try {
        loading.value = true
        const response = await http.get('/prescriptions/medications/search', {
          params: { query: queryString }
        })

        const suggestions = response.data.map(med => ({
          ...med,
          value: med.name // Required for el-autocomplete
        }))

        callback(suggestions)
      } catch (error) {
        console.error('Failed to search medications:', error)
        callback([])
      } finally {
        loading.value = false
      }
    }

    const handleSelect = async (item) => {
      try {
        loadingDetails.value = true
        // Fetch full medication details
        const response = await http.get(`/prescriptions/medications/${item.id}`)
        selectedMedication.value = response.data

        emit('update:modelValue', response.data)
        emit('select', response.data)
      } catch (error) {
        console.error('Failed to fetch medication details:', error)
      }finally {
        loadingDetails.value = false  // ✅ Hide loading
      }
    }

    const clearSelection = () => {
      selectedMedication.value = null
      searchQuery.value = ''
      emit('update:modelValue', null)
      emit('select', null)
    }

    return {
      searchQuery,
      selectedMedication,
      loading,
      loadingDetails,
      searchMedications,
      handleSelect,
      clearSelection
    }
  }
}
</script>

<style scoped>
.medication-search {
  width: 100%;
}

.medication-item {
  padding: 8px 0;
}

.med-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.med-details {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.med-details .generic {
  margin-right: 10px;
}

.med-details .brand {
  font-style: italic;
  color: #999;
}

.med-dosage {
  font-size: 12px;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ✅  loading styles */
.loading-text,
.empty-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  color: #909399;
  font-size: 13px;
}

.med-detail-card {
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.selected-med-info .med-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.selected-med-info h4 {
  margin: 0;
  color: #333;
}

.forms-strengths, .drug-info, .warnings {
  font-size: 13px;
}

.warnings .el-alert {
  font-size: 12px;
}
</style>