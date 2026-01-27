<template>
  <div class="my-patients-view">
    <!-- Header with Stats -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card class="stat-card total">
          <el-statistic title="Total Patients" :value="stats.totalPatients">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card active">
          <el-statistic title="Active Patients" :value="stats.activePatients">
            <template #prefix>
              <el-icon><CircleCheck /></el-icon>
            </template>
            <template #suffix>
              <el-tag type="success" size="small">Last 3 months</el-tag>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card new">
          <el-statistic title="New This Month" :value="stats.newPatientsThisMonth">
            <template #prefix>
              <el-icon><Plus /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card followup">
          <el-statistic title="Need Follow-up" :value="stats.needingFollowUp">
            <template #prefix>
              <el-icon><Bell /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- Main Content Card -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="header-title">
            <el-icon><UserFilled /></el-icon>
            My Patients
          </span>
          <div class="header-actions">
            <el-button @click="loadPatients" :loading="loading">
              <el-icon><Refresh /></el-icon>
              Refresh
            </el-button>
            <el-button type="primary" @click="showAddPatientDialog">
              <el-icon><Plus /></el-icon>
              Add Patient
            </el-button>
          </div>
        </div>
      </template>

      <!-- Search and Filters -->
      <el-row :gutter="20" class="filter-section">
        <el-col :span="8">
          <el-input
              v-model="filters.search"
              placeholder="Search by name, ID, or email..."
              clearable
              @keyup.enter="loadPatients"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="16">
          <el-space wrap>
            <el-select v-model="filters.status" placeholder="Status" clearable style="width: 140px;">
              <el-option label="All Status" value="" />
              <el-option label="Active" value="active" />
              <el-option label="Inactive" value="inactive" />
            </el-select>

            <el-select v-model="filters.gender" placeholder="Gender" clearable style="width: 120px;">
              <el-option label="All Gender" value="" />
              <el-option label="Male" value="M" />
              <el-option label="Female" value="F" />
              <el-option label="Other" value="O" />
            </el-select>

            <el-input-number
                v-model="filters.ageMin"
                placeholder="Min Age"
                :min="0"
                :max="120"
                controls-position="right"
                style="width: 120px;"
            />
            <span style="margin: 0 5px;">to</span>
            <el-input-number
                v-model="filters.ageMax"
                placeholder="Max Age"
                :min="0"
                :max="120"
                controls-position="right"
                style="width: 120px;"
            />

            <el-select v-model="filters.sortBy" placeholder="Sort By" style="width: 150px;">
              <el-option label="Name (A-Z)" value="name" />
              <el-option label="Last Visit" value="lastVisit" />
              <el-option label="Age" value="age" />
            </el-select>

            <el-select v-model="filters.sortDirection" style="width: 100px;">
              <el-option label="Asc" value="asc" />
              <el-option label="Desc" value="desc" />
            </el-select>

            <el-button type="primary" @click="loadPatients">
              <el-icon><Search /></el-icon>
              Apply Filters
            </el-button>
            <el-button @click="resetFilters">
              <el-icon><Refresh /></el-icon>
              Reset
            </el-button>
          </el-space>
        </el-col>
      </el-row>

      <!-- View Toggle -->
      <el-radio-group v-model="viewMode" style="margin: 20px 0;">
        <el-radio-button label="grid">
          <el-icon><Grid /></el-icon> Grid View
        </el-radio-button>
        <el-radio-button label="table">
          <el-icon><List /></el-icon> Table View
        </el-radio-button>
      </el-radio-group>

      <!-- Grid View -->
      <div v-if="viewMode === 'grid'" v-loading="loading">
        <div v-if="patients.length === 0 && !loading" class="empty-state">
          <el-empty description="No patients found">
            <el-button type="primary" @click="resetFilters">Clear Filters</el-button>
          </el-empty>
        </div>
        <div v-else class="patients-grid">
          <el-card
              v-for="patient in patients"
              :key="patient.id"
              class="patient-card"
              :class="{ 'inactive': patient.status === 'inactive' }"
              shadow="hover"
              @click="viewPatientDetail(patient)"
          >
            <!-- Patient Header -->
            <div class="patient-header">
              <el-avatar :size="60" class="patient-avatar">
                {{ getInitials(patient.name) }}
              </el-avatar>
              <div class="patient-info">
                <h3>{{ patient.name }}</h3>
                <div class="patient-meta">
                  <el-tag size="small">{{ patient.patientId }}</el-tag>
                  <el-tag :type="patient.status === 'active' ? 'success' : 'info'" size="small">
                    {{ patient.status }}
                  </el-tag>
                </div>
              </div>
            </div>

            <!-- Patient Details -->
            <el-divider />
            <div class="patient-details">
              <div class="detail-item">
                <el-icon><Calendar /></el-icon>
                <span>Age: {{ patient.age || 'N/A' }}</span>
              </div>
              <div class="detail-item">
                <el-icon><Male v-if="patient.sex === 'M'" /><Female v-else /></el-icon>
                <span>{{ getSexLabel(patient.sex) }}</span>
              </div>
              <div class="detail-item">
                <el-icon><Clock /></el-icon>
                <span>Last Visit: {{ formatDate(patient.lastVisit) }}</span>
              </div>
              <div class="detail-item" v-if="patient.nextAppointment">
                <el-icon><Calendar /></el-icon>
                <span>Next: {{ formatDate(patient.nextAppointment) }}</span>
              </div>
            </div>

            <!-- Patient Stats -->
            <el-divider />
            <el-row :gutter="10" class="patient-stats">
              <el-col :span="8">
                <div class="stat-item">
                  <div class="stat-value">{{ patient.appointmentCount }}</div>
                  <div class="stat-label">Appointments</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-item">
                  <div class="stat-value">{{ patient.studyCount }}</div>
                  <div class="stat-label">Studies</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-item">
                  <div class="stat-value">{{ patient.reportCount }}</div>
                  <div class="stat-label">Reports</div>
                </div>
              </el-col>
            </el-row>

            <!-- Quick Actions -->
            <el-divider />
            <div class="patient-actions">
              <el-button size="small" @click.stop="scheduleAppointment(patient)">
                <el-icon><Calendar /></el-icon>
                Schedule
              </el-button>
              <el-button size="small" type="primary" @click.stop="createReport(patient)">
                <el-icon><Document /></el-icon>
                Report
              </el-button>
              <el-dropdown @command="handleAction($event, patient)" @click.stop>
                <el-button size="small">
                  More<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="message">
                      <el-icon><ChatDotRound /></el-icon>
                      Send Message
                    </el-dropdown-item>
                    <el-dropdown-item command="history">
                      <el-icon><FolderOpened /></el-icon>
                      View History
                    </el-dropdown-item>
                    <el-dropdown-item command="notes">
                      <el-icon><Edit /></el-icon>
                      Add Note
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </el-card>
        </div>
      </div>

      <!-- Table View -->
      <div v-else v-loading="loading">
        <el-table
            :data="patients"
            style="width: 100%"
            @row-click="viewPatientDetail"
            row-class-name="clickable-row"
        >
          <el-table-column prop="patientId" label="Patient ID" width="130" />
          <el-table-column prop="name" label="Name" width="180" />
          <el-table-column prop="age" label="Age" width="80" align="center" />
          <el-table-column prop="sex" label="Gender" width="100">
            <template #default="{ row }">
              {{ getSexLabel(row.sex) }}
            </template>
          </el-table-column>
          <el-table-column prop="lastVisit" label="Last Visit" width="130">
            <template #default="{ row }">
              {{ formatDate(row.lastVisit) }}
            </template>
          </el-table-column>
          <el-table-column prop="nextAppointment" label="Next Appt" width="130">
            <template #default="{ row }">
              {{ formatDate(row.nextAppointment) }}
            </template>
          </el-table-column>
          <el-table-column prop="appointmentCount" label="Appts" width="80" align="center" />
          <el-table-column prop="studyCount" label="Studies" width="90" align="center" />
          <el-table-column prop="reportCount" label="Reports" width="90" align="center" />
          <el-table-column prop="status" label="Status" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click.stop="scheduleAppointment(row)">
                <el-icon><Calendar /></el-icon>
              </el-button>
              <el-button size="small" type="primary" @click.stop="createReport(row)">
                <el-icon><Document /></el-icon>
              </el-button>
              <el-button size="small" @click.stop="viewPatientDetail(row)">
                <el-icon><View /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Pagination -->
      <el-pagination
          v-if="pagination.total > 0"
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[12, 24, 36, 48]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadPatients"
          @current-change="loadPatients"
          style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- Add Patient Dialog  -->
    <el-dialog
        v-model="addPatientDialogVisible"
        title="Add New Patient"
        width="800px"
        :close-on-click-modal="false"
    >
      <el-form :model="newPatientForm" :rules="patientRules" ref="patientFormRef" label-width="160px">
        <!-- Personal Information -->
        <el-divider content-position="left">Personal Information</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Full Name" prop="name">
              <el-input v-model="newPatientForm.name" placeholder="Enter full name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Date of Birth" prop="birthDate">
              <el-date-picker
                  v-model="newPatientForm.birthDate"
                  type="date"
                  placeholder="Select date of birth"
                  style="width: 100%;"
                  :disabled-date="disabledBirthDate"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Gender" prop="sex">
              <el-select v-model="newPatientForm.sex" placeholder="Select gender" style="width: 100%;">
                <el-option label="Male" value="M" />
                <el-option label="Female" value="F" />
                <el-option label="Other" value="O" />
                <el-option label="Unknown" value="U" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Blood Type">
              <el-select v-model="newPatientForm.bloodType" placeholder="Select blood type" style="width: 100%;" clearable>
                <el-option label="A+" value="A+" />
                <el-option label="A-" value="A-" />
                <el-option label="B+" value="B+" />
                <el-option label="B-" value="B-" />
                <el-option label="O+" value="O+" />
                <el-option label="O-" value="O-" />
                <el-option label="AB+" value="AB+" />
                <el-option label="AB-" value="AB-" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Contact Information -->
        <el-divider content-position="left">Contact Information</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Email" prop="email">
              <el-input v-model="newPatientForm.email" placeholder="patient@example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Phone" prop="phone">
              <el-input v-model="newPatientForm.phone" placeholder="+1234567890" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Address">
          <el-input v-model="newPatientForm.address" placeholder="Street address" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="City">
              <el-input v-model="newPatientForm.city" placeholder="City" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="State">
              <el-input v-model="newPatientForm.state" placeholder="State" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Zip Code">
              <el-input v-model="newPatientForm.zipCode" placeholder="12345" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Emergency Contact -->
        <el-divider content-position="left">Emergency Contact</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Contact Name">
              <el-input v-model="newPatientForm.emergencyContactName" placeholder="Emergency contact name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Contact Phone">
              <el-input v-model="newPatientForm.emergencyContactPhone" placeholder="+1234567890" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Relationship">
          <el-input v-model="newPatientForm.emergencyContactRelationship" placeholder="e.g., Spouse, Parent, Sibling" />
        </el-form-item>

        <!-- Medical Information -->
        <el-divider content-position="left">Medical Information</el-divider>
        <el-form-item label="Allergies">
          <el-input
              v-model="newPatientForm.allergies"
              type="textarea"
              :rows="2"
              placeholder="List any known allergies (medications, food, environmental)"
          />
        </el-form-item>

        <el-form-item label="Medical Conditions">
          <el-input
              v-model="newPatientForm.medicalConditions"
              type="textarea"
              :rows="2"
              placeholder="List chronic conditions or diagnoses"
          />
        </el-form-item>

        <el-form-item label="Current Medications">
          <el-input
              v-model="newPatientForm.currentMedications"
              type="textarea"
              :rows="2"
              placeholder="List current medications and dosages"
          />
        </el-form-item>

        <!-- User Account Creation -->
        <el-divider content-position="left">User Account</el-divider>
        <el-form-item>
          <el-checkbox v-model="newPatientForm.createUserAccount">
            Create login account for this patient
          </el-checkbox>
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            If checked, patient will be able to login and view their own records
          </div>
        </el-form-item>
      </el-form>

      <!-- Credentials Display (after successful creation) -->
      <el-alert
          v-if="newPatientCredentials"
          type="success"
          :closable="false"
          style="margin-top: 20px;"
      >
        <template #title>
          <strong>Patient Created Successfully!</strong>
        </template>
        <div style="margin-top: 10px;">
          <p><strong>Login Credentials:</strong></p>
          <p>Username: <strong>{{ newPatientCredentials.username }}</strong></p>
          <p>Temporary Password: <strong>{{ newPatientCredentials.password }}</strong></p>
          <p style="color: #e6a23c; margin-top: 10px;">
            ⚠️ Please provide these credentials to the patient. They should change the password after first login.
          </p>
        </div>
      </el-alert>

      <template #footer>
        <el-button @click="closeAddPatientDialog">Cancel</el-button>
        <el-button type="primary" @click="createPatient" :loading="creatingPatient">
          Create Patient
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'
import {
  User, CircleCheck, Plus, Bell, UserFilled, Refresh, Search, Grid, List,
  Calendar, Clock, Male, Female, Document, ChatDotRound, FolderOpened,
  Edit, ArrowDown, View
} from '@element-plus/icons-vue'

export default {
  name: 'MyPatientsView',
  components: {
    User, CircleCheck, Plus, Bell, UserFilled, Refresh, Search, Grid, List,
    Calendar, Clock, Male, Female, Document, ChatDotRound, FolderOpened,
    Edit, ArrowDown, View
  },
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const patients = ref([])
    const viewMode = ref('grid')
    const addPatientDialogVisible = ref(false)
    const creatingPatient = ref(false)
    const patientFormRef = ref(null)
    const newPatientCredentials = ref(null)

    const stats = reactive({
      totalPatients: 0,
      activePatients: 0,
      newPatientsThisMonth: 0,
      needingFollowUp: 0
    })

    const filters = reactive({
      search: '',
      status: '',
      gender: '',
      ageMin: null,
      ageMax: null,
      sortBy: 'name',
      sortDirection: 'asc'
    })

    const pagination = reactive({
      page: 1,
      size: 12,
      total: 0
    })



    const loadPatients = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.page - 1,
          size: pagination.size,
          sortBy: filters.sortBy,
          sortDirection: filters.sortDirection
        }

        if (filters.search) params.search = filters.search
        if (filters.status) params.status = filters.status
        if (filters.gender) params.gender = filters.gender
        if (filters.ageMin !== null) params.ageMin = filters.ageMin
        if (filters.ageMax !== null) params.ageMax = filters.ageMax

        const response = await http.get('/patient/doctor/my-patients', { params })
        patients.value = response.data.content || []
        pagination.total = response.data.totalElements || 0
      } catch (error) {
        console.error('Failed to load patients:', error)
        ElMessage.error('Failed to load patients')
        patients.value = []
      } finally {
        loading.value = false
      }
    }

    const loadStats = async () => {
      try {
        const response = await http.get('/patient/doctor/patient-stats')
        Object.assign(stats, response.data)
      } catch (error) {
        console.error('Failed to load stats:', error)
      }
    }

    const resetFilters = () => {
      filters.search = ''
      filters.status = ''
      filters.gender = ''
      filters.ageMin = null
      filters.ageMax = null
      filters.sortBy = 'name'
      filters.sortDirection = 'asc'
      pagination.page = 1
      loadPatients()
    }

    const viewPatientDetail = (patient) => {
      // Navigate to patient detail view (Phase 4.2)
      router.push(`/patients/${patient.id}`)
    }

    const scheduleAppointment = (patient) => {
      // Pre-fill patient in appointment form
      router.push(`/appointments?patientId=${patient.id}`)
    }

    const createReport = (patient) => {
      router.push(`/reports/new?patientId=${patient.id}&patientName=${patient.name}`)
    }

    const handleAction = (command, patient) => {
      switch (command) {
        case 'message':
          ElMessage.info('Messaging feature coming in Phase 4.6')
          break
        case 'history':
          viewPatientDetail(patient)
          break
        case 'notes':
          ElMessage.info('Clinical notes feature coming in Phase 4.4')
          break
      }
    }

    const showAddPatientDialog = () => {
      // Reset form
      newPatientForm.name = ''
      newPatientForm.birthDate = null
      newPatientForm.sex = ''
      newPatientForm.bloodType = ''
      newPatientForm.email = ''
      newPatientForm.phone = ''
      newPatientForm.address = ''
      newPatientForm.city = ''
      newPatientForm.state = ''
      newPatientForm.zipCode = ''
      newPatientForm.emergencyContactName = ''
      newPatientForm.emergencyContactPhone = ''
      newPatientForm.emergencyContactRelationship = ''
      newPatientForm.allergies = ''
      newPatientForm.medicalConditions = ''
      newPatientForm.currentMedications = ''
      newPatientForm.createUserAccount = true

      newPatientCredentials.value = null
      addPatientDialogVisible.value = true
    }

    const disabledBirthDate = (date) => {
      const today = new Date()
      const minDate = new Date()
      minDate.setFullYear(today.getFullYear() - 120)
      return date > today || date < minDate
    }

    const createPatient = async () => {
      if (!patientFormRef.value) return

      try {
        await patientFormRef.value.validate()
      } catch (e) {
        return
      }

      creatingPatient.value = true
      try {
        // Format birthdate
        const birthDateStr = newPatientForm.birthDate
            ? new Date(newPatientForm.birthDate).toISOString().split('T')[0]
            : null

        const response = await http.post('/patient/doctor/create', {
          name: newPatientForm.name,
          birthDate: birthDateStr,
          sex: newPatientForm.sex,
          bloodType: newPatientForm.bloodType,
          email: newPatientForm.email,
          phone: newPatientForm.phone,
          address: newPatientForm.address,
          city: newPatientForm.city,
          state: newPatientForm.state,
          zipCode: newPatientForm.zipCode,
          emergencyContactName: newPatientForm.emergencyContactName,
          emergencyContactPhone: newPatientForm.emergencyContactPhone,
          emergencyContactRelationship: newPatientForm.emergencyContactRelationship,
          allergies: newPatientForm.allergies,
          medicalConditions: newPatientForm.medicalConditions,
          currentMedications: newPatientForm.currentMedications,
          createUserAccount: newPatientForm.createUserAccount
        })

        ElMessage.success('Patient created successfully!')

        // If user account was created, show credentials
        if (response.data.credentials) {
          newPatientCredentials.value = response.data.credentials
        } else {
          // Close dialog and reload if no credentials to show
          closeAddPatientDialog()
        }

        // Reload patient list
        loadPatients()
        loadStats()
      } catch (error) {
        console.error('Failed to create patient:', error)
        ElMessage.error(error.response?.data?.message || 'Failed to create patient')
      } finally {
        creatingPatient.value = false
      }
    }

    const closeAddPatientDialog = () => {
      addPatientDialogVisible.value = false
      newPatientCredentials.value = null
    }

    const getInitials = (name) => {
      if (!name) return '??'
      return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
    }

    const getSexLabel = (sex) => {
      const labels = { M: 'Male', F: 'Female', O: 'Other', U: 'Unknown' }
      return labels[sex] || 'N/A'
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return 'N/A'
      return new Date(dateStr).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    }

    // add patient
    const newPatientForm = reactive({
      name: '',
      birthDate: null,
      sex: '',
      bloodType: '',
      email: '',
      phone: '',
      address: '',
      city: '',
      state: '',
      zipCode: '',
      emergencyContactName: '',
      emergencyContactPhone: '',
      emergencyContactRelationship: '',
      allergies: '',
      medicalConditions: '',
      currentMedications: '',
      createUserAccount: true
    })

    const patientRules = {
      name: [{ required: true, message: 'Full name is required', trigger: 'blur' }],
      birthDate: [{ required: true, message: 'Date of birth is required', trigger: 'change' }],
      sex: [{ required: true, message: 'Gender is required', trigger: 'change' }],
      email: [
        { required: true, message: 'Email is required', trigger: 'blur' },
        { type: 'email', message: 'Invalid email format', trigger: 'blur' }
      ],
      phone: [{ required: true, message: 'Phone is required', trigger: 'blur' }]
    }

    onMounted(() => {
      loadPatients()
      loadStats()
    })

    return {
      loading, patients, viewMode, addPatientDialogVisible,
      stats, filters, pagination,
      loadPatients, loadStats, resetFilters,
      viewPatientDetail, scheduleAppointment, createReport, handleAction,
      showAddPatientDialog, getInitials, getSexLabel, formatDate,
      creatingPatient,
      patientFormRef,
      newPatientCredentials,
      newPatientForm,
      patientRules,
      disabledBirthDate,
      createPatient,
      closeAddPatientDialog
    }
  }
}
</script>

<style scoped>
.my-patients-view {
  padding: 20px;
}

.stat-card {
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-3px);
}

.stat-card.total { border-top: 4px solid #409eff; }
.stat-card.active { border-top: 4px solid #67c23a; }
.stat-card.new { border-top: 4px solid #e6a23c; }
.stat-card.followup { border-top: 4px solid #f56c6c; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.filter-section {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.empty-state {
  padding: 40px;
}

.patients-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.patient-card {
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.patient-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.patient-card.inactive {
  opacity: 0.7;
}

.patient-header {
  display: flex;
  align-items: center;
  gap: 15px;
}

.patient-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: bold;
  font-size: 20px;
}

.patient-info h3 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #333;
}

.patient-meta {
  display: flex;
  gap: 8px;
}

.patient-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}

.patient-stats {
  text-align: center;
}

.stat-item {
  padding: 5px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.patient-actions {
  display: flex;
  gap: 8px;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: #ecf5ff !important;
}
</style>