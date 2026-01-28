<template>
  <div class="prescriptions-tab">
    <!-- Header with Add Button -->
    <div class="tab-header">
      <div class="header-info">
        <h3>Prescriptions</h3>
        <el-tag type="success">{{ activePrescriptions.length }} Active</el-tag>
      </div>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon> Add Prescription
      </el-button>
    </div>

    <!-- Filter Tabs -->
    <el-tabs v-model="activeTab" @tab-click="filterPrescriptions" style="margin-top: 15px;">
      <el-tab-pane label="All" name="all">
        <el-badge :value="allPrescriptions.length" />
      </el-tab-pane>
      <el-tab-pane label="Active" name="active">
        <el-badge :value="activePrescriptions.length" type="success" />
      </el-tab-pane>
      <el-tab-pane label="Completed" name="completed">
        <el-badge :value="completedPrescriptions.length" type="info" />
      </el-tab-pane>
    </el-tabs>

    <!-- Loading State -->
    <div v-if="loading" v-loading="true" style="min-height: 200px;"></div>

    <!-- Empty State -->
    <div v-else-if="displayedPrescriptions.length === 0" class="empty-state">
      <el-empty description="No prescriptions found">
        <el-button type="primary" @click="showCreateDialog = true">
          Add First Prescription
        </el-button>
      </el-empty>
    </div>

    <!-- Prescriptions Table -->
    <el-table
        v-else
        :data="displayedPrescriptions"
        style="width: 100%; margin-top: 15px;"
        stripe
    >
      <el-table-column prop="prescribedDate" label="Date" width="120" sortable>
        <template #default="{ row }">
          {{ formatDate(row.prescribedDate) }}
        </template>
      </el-table-column>

      <el-table-column prop="medicationName" label="Medication" min-width="180">
        <template #default="{ row }">
          <div>
            <strong>{{ row.medicationName }}</strong>
            <div class="med-subtitle">{{ row.dosage }} {{ row.dosageForm }}</div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="frequency" label="Frequency" width="140" />

      <el-table-column prop="quantity" label="Qty" width="70" align="center" />

      <el-table-column prop="refills" label="Refills" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="getRefillTagType(row)">
            {{ row.refillsRemaining }}/{{ row.refillsAllowed }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="Status" width="110">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="doctorName" label="Doctor" width="130" />

      <el-table-column label="Actions" width="200" fixed="right">
        <template #default="{ row }">
          <el-button-group>
            <el-button size="small" @click="viewPrescription(row)">
              <el-icon><View /></el-icon>
            </el-button>
            <el-button
                size="small"
                type="success"
                @click="handleRefill(row)"
                :disabled="!canRefill(row)"
            >
              <el-icon><RefreshRight /></el-icon>
            </el-button>
            <el-button
                size="small"
                type="danger"
                @click="handleCancel(row)"
                :disabled="row.status !== 'active'"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create Prescription Dialog -->
    <CreatePrescriptionDialog
        v-model="showCreateDialog"
        :patient-id="patientId"
        @prescription-created="handlePrescriptionCreated"
    />

    <!-- Detail Dialog -->
    <PrescriptionDetailDialog
        v-model="showDetailDialog"
        :prescription="selectedPrescription"
        :show-admin-actions="true"
        @refilled="loadPrescriptions"
        @cancelled="loadPrescriptions"
    />
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import http from '../../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, View, RefreshRight, Close } from '@element-plus/icons-vue'
import CreatePrescriptionDialog from '../../components/CreatePrescriptionDialog.vue'
import PrescriptionDetailDialog from '../../components/PrescriptionDetailDialog.vue'

export default {
  name: 'PrescriptionsTab',
  components: {
    Plus, View, RefreshRight, Close,
    CreatePrescriptionDialog,
    PrescriptionDetailDialog
  },
  props: {
    patientId: {
      type: Number,
      required: true
    }
  },
  emits: ['prescription-added'],
  setup(props, { emit }) {
    const loading = ref(false)
    const activeTab = ref('all')
    const allPrescriptions = ref([])
    const displayedPrescriptions = ref([])
    const showCreateDialog = ref(false)
    const showDetailDialog = ref(false)
    const selectedPrescription = ref(null)

    const activePrescriptions = computed(() => {
      return allPrescriptions.value.filter(rx => rx.status === 'active')
    })

    const completedPrescriptions = computed(() => {
      return allPrescriptions.value.filter(rx => rx.status === 'completed')
    })

    const loadPrescriptions = async () => {
      loading.value = true
      try {
        const response = await http.get(`/prescriptions/patient/${props.patientId}`)
        allPrescriptions.value = response.data
        filterPrescriptions()
      } catch (error) {
        console.error('Failed to load prescriptions:', error)
        ElMessage.error('Failed to load prescriptions')
        allPrescriptions.value = []
      } finally {
        loading.value = false
      }
    }

    const filterPrescriptions = () => {
      switch (activeTab.value) {
        case 'active':
          displayedPrescriptions.value = activePrescriptions.value
          break
        case 'completed':
          displayedPrescriptions.value = completedPrescriptions.value
          break
        default:
          displayedPrescriptions.value = allPrescriptions.value
      }
    }

    const viewPrescription = (rx) => {
      selectedPrescription.value = rx
      showDetailDialog.value = true
    }

    const canRefill = (rx) => {
      return rx.status === 'active' && rx.refillsRemaining > 0
    }

    const handleRefill = async (rx) => {
      try {
        await ElMessageBox.confirm(
            `Process refill for ${rx.medicationName}?`,
            'Confirm Refill',
            {
              confirmButtonText: 'Process Refill',
              cancelButtonText: 'Cancel',
              type: 'info'
            }
        )

        await http.post(`/prescriptions/${rx.id}/refill`)
        ElMessage.success('Refill processed successfully')
        await loadPrescriptions()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Failed to process refill:', error)
          ElMessage.error(error.response?.data?.message || 'Failed to process refill')
        }
      }
    }

    const handleCancel = async (rx) => {
      try {
        const { value } = await ElMessageBox.prompt(
            `Cancel prescription for ${rx.medicationName}?`,
            'Cancel Prescription',
            {
              confirmButtonText: 'Cancel Prescription',
              cancelButtonText: 'Keep Active',
              inputPattern: /.+/,
              inputErrorMessage: 'Reason is required',
              inputPlaceholder: 'Enter reason for cancellation',
              type: 'warning'
            }
        )

        await http.put(`/prescriptions/${rx.id}/status`, {
          status: 'cancelled',
          reason: value
        })

        ElMessage.success('Prescription cancelled')
        await loadPrescriptions()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Failed to cancel prescription:', error)
          ElMessage.error('Failed to cancel prescription')
        }
      }
    }

    const handlePrescriptionCreated = async (prescription) => {
      ElMessage.success('Prescription created successfully!')
      await loadPrescriptions()
      emit('prescription-added', prescription)
    }

    const getStatusType = (status) => {
      const types = {
        active: 'success',
        completed: 'info',
        cancelled: 'danger',
        expired: 'warning'
      }
      return types[status] || 'info'
    }

    const getRefillTagType = (rx) => {
      if (rx.refillsRemaining === 0) return 'danger'
      if (rx.refillsRemaining <= 1) return 'warning'
      return 'success'
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    }

    // Watch for patient ID changes
    watch(() => props.patientId, () => {
      if (props.patientId) {
        loadPrescriptions()
      }
    }, { immediate: true })

    onMounted(() => {
      loadPrescriptions()
    })

    return {
      loading,
      activeTab,
      allPrescriptions,
      displayedPrescriptions,
      activePrescriptions,
      completedPrescriptions,
      showCreateDialog,
      showDetailDialog,
      selectedPrescription,
      loadPrescriptions,
      filterPrescriptions,
      viewPrescription,
      canRefill,
      handleRefill,
      handleCancel,
      handlePrescriptionCreated,
      getStatusType,
      getRefillTagType,
      formatDate
    }
  }
}
</script>

<style scoped>
.prescriptions-tab {
  padding: 20px 0;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-info h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.med-subtitle {
  font-size: 12px;
  color: #909399;
  margin-top: 3px;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.el-button-group {
  display: flex;
}
</style>