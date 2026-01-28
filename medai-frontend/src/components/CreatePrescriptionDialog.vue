<template>
  <el-dialog
      v-model="visible"
      title="Create New Prescription"
      width="900px"
      :close-on-click-modal="false"
      @close="handleClose"
  >
    <el-form
        ref="prescriptionFormRef"
        :model="form"
        :rules="rules"
        label-width="150px"
        v-loading="loading"
    >
      <!-- Step Indicator -->
      <el-steps :active="currentStep" align-center style="margin-bottom: 30px;">
        <el-step title="Medication" />
        <el-step title="Dosage" />
        <el-step title="Instructions" />
        <el-step title="Review" />
      </el-steps>

      <!-- STEP 1: MEDICATION SELECTION -->
      <div v-show="currentStep === 0">
        <h3>Select Medication</h3>

        <!-- Medication Search -->
        <el-form-item label="Search Medication" prop="medicationId">
          <MedicationSearchAutocomplete
              v-model="selectedMedication"
              @select="handleMedicationSelect"
              placeholder="Search by generic or brand name..."
          />
        </el-form-item>

        <!-- Selected Medication Info -->
        <el-alert
            v-if="selectedMedication"
            :title="selectedMedication.name"
            type="success"
            :closable="false"
            style="margin-bottom: 20px;"
        >
          <div><strong>Generic:</strong> {{ selectedMedication.genericName }}</div>
          <div><strong>Class:</strong> {{ selectedMedication.drugClass }}</div>
          <div><strong>Description:</strong> {{ selectedMedication.description }}</div>
        </el-alert>

        <!-- Drug Safety Warnings (show if any) -->
        <el-alert
            v-if="interactionResult && interactionResult.warnings && interactionResult.warnings.length > 0"
            :title="interactionResult.summary"
            :type="interactionResult.hasCritical ? 'error' : interactionResult.hasWarning ? 'warning' : 'info'"
            style="margin-bottom: 20px;"
        >
          <div v-for="(warning, index) in interactionResult.warnings" :key="index" style="margin-bottom: 10px;">
            <strong>{{ warning.message }}</strong>
            <p style="margin: 5px 0;">{{ warning.detail }}</p>
            <p style="margin: 5px 0; color: #909399;"><em>Action: {{ warning.action }}</em></p>
          </div>
        </el-alert>

        <!-- Manual Entry Option (optional fallback) -->
        <el-divider>Or Enter Manually</el-divider>
        <el-form-item label="Medication Name">
          <el-input
              v-model="form.medicationName"
              placeholder="Enter medication name manually (if not found in search)"
          />
        </el-form-item>
      </div>

      <!-- STEP 2: DOSAGE & FREQUENCY -->
      <div v-show="currentStep === 1">
        <h3>Dosage & Frequency</h3>

        <!-- Suggested Dosage -->
        <el-alert
            v-if="dosageCalculation && dosageCalculation.suggestedDosage"
            title="Suggested Dosage"
            type="info"
            :closable="false"
            style="margin-bottom: 20px;"
        >
          <div><strong>{{ dosageCalculation.suggestedDosage }}</strong></div>
          <ul v-if="dosageCalculation.adjustmentNotes && dosageCalculation.adjustmentNotes.length > 0">
            <li v-for="(note, index) in dosageCalculation.adjustmentNotes" :key="index">{{ note }}</li>
          </ul>
        </el-alert>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Dosage" prop="dosage">
              <el-input v-model="form.dosage" placeholder="e.g., 500mg" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Dosage Form" prop="dosageForm">
              <el-select v-model="form.dosageForm" placeholder="Select form" style="width: 100%;">
                <el-option
                    v-for="formOption in availableDosageForms"
                    :key="formOption"
                    :label="formOption"
                    :value="formOption"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Route" prop="route">
              <el-select v-model="form.route" placeholder="Select route" style="width: 100%;">
                <el-option
                    v-for="route in availableRoutes"
                    :key="route"
                    :label="route"
                    :value="route"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Frequency" prop="frequency">
              <el-select v-model="form.frequency" placeholder="Select frequency" style="width: 100%;" @change="updateFrequencyCode">
                <el-option label="Once daily (QD)" value="once daily" />
                <el-option label="Twice daily (BID)" value="twice daily" />
                <el-option label="Three times daily (TID)" value="three times daily" />
                <el-option label="Four times daily (QID)" value="four times daily" />
                <el-option label="Every 6 hours (Q6H)" value="every 6 hours" />
                <el-option label="Every 8 hours (Q8H)" value="every 8 hours" />
                <el-option label="Every 12 hours (Q12H)" value="every 12 hours" />
                <el-option label="As needed (PRN)" value="as needed" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Timing -->
        <el-form-item label="Take When">
          <el-checkbox-group v-model="timingSelected">
            <el-checkbox label="morning">Morning</el-checkbox>
            <el-checkbox label="afternoon">Afternoon</el-checkbox>
            <el-checkbox label="evening">Evening</el-checkbox>
            <el-checkbox label="bedtime">Bedtime</el-checkbox>
            <el-checkbox label="asNeeded">As Needed</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="Quantity" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" :max="1000" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Duration (days)">
              <el-input-number v-model="form.durationDays" :min="1" :max="365" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Refills">
              <el-input-number v-model="form.refillsAllowed" :min="0" :max="12" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <!-- STEP 3: INSTRUCTIONS -->
      <div v-show="currentStep === 2">
        <h3>Instructions & Details</h3>

        <el-form-item label="Instructions" prop="instructions">
          <el-input
              v-model="form.instructions"
              type="textarea"
              :rows="3"
              placeholder="e.g., Take with food, Avoid alcohol"
          />
        </el-form-item>

        <el-form-item label="Patient Instructions">
          <el-input
              v-model="form.patientInstructions"
              type="textarea"
              :rows="3"
              placeholder="Simple instructions for patient"
          />
        </el-form-item>

        <el-form-item label="Doctor Notes">
          <el-input
              v-model="form.notes"
              type="textarea"
              :rows="2"
              placeholder="Private notes"
          />
        </el-form-item>

        <el-divider>Pharmacy</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Pharmacy Name">
              <el-input v-model="form.pharmacyName" placeholder="Pharmacy name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Pharmacy Phone">
              <el-input v-model="form.pharmacyPhone" placeholder="Phone number" />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <!-- STEP 4: REVIEW -->
      <div v-show="currentStep === 3">
        <h3>Review Prescription</h3>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="Medication">
            {{ form.medicationName || selectedMedication?.name }}
          </el-descriptions-item>
          <el-descriptions-item label="Dosage">
            {{ form.dosage }} {{ form.dosageForm }}
          </el-descriptions-item>
          <el-descriptions-item label="Route">
            {{ form.route }}
          </el-descriptions-item>
          <el-descriptions-item label="Frequency">
            {{ form.frequency }}
          </el-descriptions-item>
          <el-descriptions-item label="Quantity">
            {{ form.quantity }}
          </el-descriptions-item>
          <el-descriptions-item label="Duration">
            {{ form.durationDays }} days
          </el-descriptions-item>
        </el-descriptions>

        <!-- Override Warnings -->
        <el-alert
            v-if="interactionResult?.hasCritical"
            title="⚠️ CRITICAL WARNINGS OVERRIDE"
            type="error"
            style="margin-top: 20px;"
        >
          <el-form-item label="Override Reason" required>
            <el-input
                v-model="form.allergyOverrideReason"
                type="textarea"
                :rows="2"
                placeholder="Document why you are overriding"
            />
          </el-form-item>
          <el-checkbox v-model="form.allergyOverride">
            I take full responsibility for overriding these warnings
          </el-checkbox>
        </el-alert>
      </div>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">Cancel</el-button>
        <el-button v-if="currentStep > 0" @click="previousStep">Previous</el-button>
        <el-button v-if="currentStep < 3" type="primary" @click="nextStep">Next</el-button>
        <el-button
            v-if="currentStep === 3"
            type="success"
            @click="submitPrescription"
            :loading="submitting"
        >
          Create Prescription
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { ref, reactive, computed, watch } from 'vue'
import http from '@/utils/http'
import { ElMessage } from 'element-plus'
import MedicationSearchAutocomplete from './MedicationSearchAutocomplete.vue'

export default {
  name: 'CreatePrescriptionDialog',
  props: {
    modelValue: Boolean,
    patientId: {
      type: Number,
      required: true
    }
  },
  components: {
    MedicationSearchAutocomplete
  },
  emits: ['update:modelValue', 'prescription-created'],
  setup(props, { emit }) {
    const visible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    const loading = ref(false)
    const submitting = ref(false)
    const currentStep = ref(0)
    const prescriptionFormRef = ref(null)

    // const medicationSearch = ref('')
    const selectedMedication = ref(null)
    const interactionResult = ref(null)
    const dosageCalculation = ref(null)
    const timingSelected = ref([])

    const form = reactive({
      patientId: props.patientId,
      medicationId: null,
      medicationName: '',
      dosage: '',
      dosageForm: 'tablet',
      route: 'oral',
      frequency: '',
      frequencyCode: '',
      quantity: 30,
      durationDays: 30,
      refillsAllowed: 0,
      instructions: '',
      patientInstructions: '',
      notes: '',
      pharmacyName: '',
      pharmacyPhone: '',
      allergyOverride: false,
      allergyOverrideReason: ''
    })

    const rules = {
      dosage: [{ required: true, message: 'Dosage is required', trigger: 'blur' }],
      dosageForm: [{ required: true, message: 'Dosage form is required', trigger: 'change' }],
      route: [{ required: true, message: 'Route is required', trigger: 'change' }],
      frequency: [{ required: true, message: 'Frequency is required', trigger: 'change' }],
      quantity: [{ required: true, message: 'Quantity is required', trigger: 'blur' }],
      instructions: [{ required: true, message: 'Instructions are required', trigger: 'blur' }]
    }

    const availableDosageForms = computed(() => {
      if (selectedMedication.value?.dosageForms) {
        return selectedMedication.value.dosageForms
      }
      return ['tablet', 'capsule', 'syrup', 'injection', 'cream']
    })

    const availableRoutes = computed(() => {
      if (selectedMedication.value?.routes) {
        return selectedMedication.value.routes
      }
      return ['oral', 'IV', 'IM', 'topical', 'inhalation']
    })

    const searchMedications = async (queryString, cb) => {
      if (!queryString || queryString.length < 2) {
        cb([])
        return
      }

      try {
        const response = await http.get('/prescriptions/medications/search', {
          params: { query: queryString }
        })
        cb(response.data)
      } catch (error) {
        console.error('Search failed:', error)
        cb([])
      }
    }

    const handleMedicationSelect = async (medication) => {
      if (!medication) {
        // Clear selection
        form.medicationId = null
        form.medicationName = ''
        selectedMedication.value = null
        interactionResult.value = null
        dosageCalculation.value = null
        return
      }
      // Medication details are already loaded by MedicationSearchAutocomplete
      selectedMedication.value = medication
      form.medicationId = medication.id
      form.medicationName = medication.name

      // Pre-fill dosage if available
      if (medication.defaultDosage) {
        form.dosage = medication.defaultDosage
      }

      // Check drug interactions and calculate dosage
      await checkDrugInteractions()
      await calculateDosage()
    }


      const checkDrugInteractions = async () => {
      if (!form.medicationId && !form.medicationName) return

      try {
        const response = await http.post('/prescriptions/check-interactions', {
          patientId: props.patientId,
          medicationId: form.medicationId,
          medicationName: form.medicationName
        })
        interactionResult.value = response.data
      } catch (error) {
        console.error('Interaction check failed:', error)
      }
    }

    const calculateDosage = async () => {
      if (!form.medicationId) return

      try {
        const response = await http.post('/prescriptions/calculate-dosage', {
          patientId: props.patientId,
          medicationId: form.medicationId
        })
        dosageCalculation.value = response.data
      } catch (error) {
        console.error('Dosage calculation failed:', error)
      }
    }

    const updateFrequencyCode = (value) => {
      const codeMap = {
        'once daily': 'QD',
        'twice daily': 'BID',
        'three times daily': 'TID',
        'four times daily': 'QID',
        'every 6 hours': 'Q6H',
        'every 8 hours': 'Q8H',
        'every 12 hours': 'Q12H',
        'as needed': 'PRN'
      }
      form.frequencyCode = codeMap[value] || ''
    }

    watch(timingSelected, (newVal) => {
      form.timingMorning = newVal.includes('morning')
      form.timingAfternoon = newVal.includes('afternoon')
      form.timingEvening = newVal.includes('evening')
      form.timingBedtime = newVal.includes('bedtime')
      form.timingAsNeeded = newVal.includes('asNeeded')
    })

    const nextStep = async () => {
      if (currentStep.value === 0) {
        if (!form.medicationId && !form.medicationName) {
          ElMessage.warning('Please select or enter a medication')
          return
        }
      }

      if (currentStep.value < 3) {
        currentStep.value++
      }
    }

    const previousStep = () => {
      if (currentStep.value > 0) {
        currentStep.value--
      }
    }

    const submitPrescription = async () => {
      if (!prescriptionFormRef.value) return

      try {
        await prescriptionFormRef.value.validate()
      } catch (e) {
        ElMessage.error('Please fill in all required fields')
        return
      }

      if (interactionResult.value?.hasCritical && !form.allergyOverride) {
        ElMessage.error('You must override critical warnings to proceed')
        return
      }

      submitting.value = true

      try {
        const response = await http.post('/prescriptions', {
          ...form,
          interactionWarnings: interactionResult.value
        })

        ElMessage.success('Prescription created successfully!')
        emit('prescription-created', response.data)
        handleClose()
      } catch (error) {
        console.error('Failed to create prescription:', error)
        ElMessage.error(error.response?.data?.message || 'Failed to create prescription')
      } finally {
        submitting.value = false
      }
    }

    const handleClose = () => {
      currentStep.value = 0
      selectedMedication.value = null
      interactionResult.value = null
      dosageCalculation.value = null  // ✅ Also clear this
      timingSelected.value = []        // ✅ Clear timing selections

      // Reset form
      Object.assign(form, {
        medicationId: null,
        medicationName: '',
        dosage: '',
        dosageForm: 'tablet',
        route: 'oral',
        frequency: '',
        frequencyCode: '',
        quantity: 30,
        durationDays: 30,
        refillsAllowed: 0,
        instructions: '',
        patientInstructions: '',
        notes: '',
        pharmacyName: '',
        pharmacyPhone: '',
        allergyOverride: false,
        allergyOverrideReason: ''
      })

      visible.value = false
    }

    return {
      visible,
      loading,
      submitting,
      currentStep,
      prescriptionFormRef,
      // medicationSearch,
      selectedMedication,
      interactionResult,
      dosageCalculation,
      timingSelected,
      form,
      rules,
      availableDosageForms,
      availableRoutes,
      // searchMedications,
      handleMedicationSelect,
      updateFrequencyCode,
      nextStep,
      previousStep,
      submitPrescription,
      handleClose
    }
  }
}
</script>

<style scoped>
.medication-item {
  padding: 8px 0;
}

.med-name {
  margin-bottom: 5px;
}

.med-details {
  font-size: 12px;
  color: #909399;
}

.med-class {
  margin-right: 15px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
}
</style>