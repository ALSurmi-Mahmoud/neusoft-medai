<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>User Management</span>
          <el-button type="primary" @click="showAddUserDialog">
            <el-icon><Plus /></el-icon>
            Add User
          </el-button>
        </div>
      </template>

      <!-- Filters -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="Search">
          <el-input v-model="filters.search" placeholder="Username or email" clearable @keyup.enter="loadUsers" />
        </el-form-item>
        <el-form-item label="Role">
          <el-select v-model="filters.role" placeholder="All Roles" clearable>
            <el-option label="Admin" value="ADMIN" />
            <el-option label="Doctor" value="DOCTOR" />
            <el-option label="Nurse" value="NURSE" />
            <el-option label="Patient" value="PATIENT" />
            <el-option label="Researcher" value="RESEARCHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.enabled" placeholder="All" clearable>
            <el-option label="Active" :value="true" />
            <el-option label="Disabled" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUsers">
            <el-icon><Search /></el-icon>
            Search
          </el-button>
          <el-button @click="resetFilters">Reset</el-button>
        </el-form-item>
      </el-form>

      <!-- Users Table -->
      <el-table :data="users" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="Username" width="150" />
        <el-table-column prop="fullName" label="Full Name" width="180" />
        <el-table-column prop="email" label="Email" width="200" />
        <el-table-column prop="role" label="Role" width="120">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="Department" width="150" />
        <el-table-column prop="enabled" label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? 'Active' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLogin" label="Last Login" width="180">
          <template #default="{ row }">
            {{ formatDate(row.lastLogin) }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" type="primary" @click="editUser(row)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button size="small" :type="row.enabled ? 'warning' : 'success'" @click="toggleUserStatus(row)">
                <el-icon><Lock v-if="row.enabled" /><Unlock v-else /></el-icon>
              </el-button>
              <el-button size="small" type="info" @click="resetPassword(row)">
                <el-icon><Key /></el-icon>
              </el-button>
              <el-button size="small" type="danger" @click="deleteUser(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadUsers"
          @current-change="loadUsers"
          style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- ✅ IDENTICAL TO REGISTRATION FORM -->
    <el-dialog v-model="userDialogVisible" :title="isEditing ? 'Edit User' : 'Add New User'" width="700px">
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="160px">
        <!-- ✅ SAME AS REGISTRATION: Username + Email -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Username" prop="username">
              <el-input v-model="userForm.username" :disabled="isEditing" placeholder="Choose a username" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Email" prop="email">
              <el-input v-model="userForm.email" placeholder="Enter email" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ✅ SAME AS REGISTRATION: Password (only when creating) -->
        <el-row :gutter="20" v-if="!isEditing">
          <el-col :span="12">
            <el-form-item label="Password" prop="password">
              <el-input v-model="userForm.password" type="password" placeholder="Create password" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Confirm Password" prop="confirmPassword">
              <el-input v-model="userForm.confirmPassword" type="password" placeholder="Confirm password" show-password />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ✅ SAME AS REGISTRATION: Full Name -->
        <el-form-item label="Full Name" prop="fullName">
          <el-input v-model="userForm.fullName" placeholder="Enter full name" />
        </el-form-item>

        <!-- ✅ SAME AS REGISTRATION: Birth Date + Gender (ALWAYS VISIBLE FOR ALL ROLES) -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Date of Birth" prop="birthDate">
              <el-date-picker
                  v-model="userForm.birthDate"
                  type="date"
                  placeholder="Select date of birth"
                  style="width: 100%;"
                  :disabled-date="disabledDate"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Gender" prop="gender">
              <el-select v-model="userForm.gender" placeholder="Select gender" style="width: 100%;">
                <el-option label="Male" value="M" />
                <el-option label="Female" value="F" />
                <el-option label="Other" value="O" />
                <el-option label="Prefer not to say" value="U" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ✅ SAME AS REGISTRATION: Phone + Role -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Phone">
              <el-input v-model="userForm.phone" placeholder="Phone number" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Role" prop="role">
              <el-select v-model="userForm.role" placeholder="Select role" style="width: 100%;">
                <el-option label="Admin" value="ADMIN" />
                <el-option label="Doctor" value="DOCTOR" />
                <el-option label="Nurse" value="NURSE" />
                <el-option label="Patient" value="PATIENT" />
                <el-option label="Researcher" value="RESEARCHER" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ✅ SAME AS REGISTRATION: Professional fields for DOCTOR -->
        <template v-if="userForm.role === 'DOCTOR'">
          <el-divider>Professional Information</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Department">
                <el-input v-model="userForm.department" placeholder="e.g., Radiology" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Specialization">
                <el-input v-model="userForm.specialization" placeholder="e.g., Cardiology" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Title">
                <el-input v-model="userForm.title" placeholder="e.g., Radiologist" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="License Number">
                <el-input v-model="userForm.licenseNumber" placeholder="Medical license #" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- ✅ SAME AS REGISTRATION: Professional fields for NURSE -->
        <template v-if="userForm.role === 'NURSE'">
          <el-divider>Professional Information</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Department">
                <el-input v-model="userForm.department" placeholder="e.g., Imaging Center" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="License Number">
                <el-input v-model="userForm.licenseNumber" placeholder="Nursing license #" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="saveUser" :loading="saving">
          {{ isEditing ? 'Update' : 'Create' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="resetPasswordDialogVisible" title="Reset Password" width="400px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="140px">
        <el-form-item label="New Password" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="Confirm Password" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPasswordDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="confirmResetPassword" :loading="resettingPassword">
          Reset Password
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import http from '../../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Lock, Unlock, Key, Delete } from '@element-plus/icons-vue'

export default {
  name: 'UserManagementView',
  components: { Plus, Search, Edit, Lock, Unlock, Key, Delete },
  setup() {
    const loading = ref(false)
    const saving = ref(false)
    const resettingPassword = ref(false)
    const users = ref([])
    const userDialogVisible = ref(false)
    const resetPasswordDialogVisible = ref(false)
    const isEditing = ref(false)
    const selectedUserId = ref(null)
    const userFormRef = ref(null)
    const passwordFormRef = ref(null)

    const filters = reactive({
      search: '',
      role: '',
      enabled: null
    })

    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })

    const userForm = reactive({
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
      fullName: '',
      birthDate: null,
      gender: '',
      phone: '',
      role: 'PATIENT',
      department: '',
      title: '',
      specialization: '',
      licenseNumber: ''
    })

    const passwordForm = reactive({
      newPassword: '',
      confirmPassword: ''
    })

    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== userForm.password) {
        callback(new Error('Passwords do not match'))
      } else {
        callback()
      }
    }

    const userRules = {
      username: [
        { required: true, message: 'Username is required', trigger: 'blur' },
        { min: 3, max: 50, message: 'Username must be 3-50 characters', trigger: 'blur' }
      ],
      email: [
        { required: true, message: 'Email is required', trigger: 'blur' },
        { type: 'email', message: 'Invalid email format', trigger: 'blur' }
      ],
      password: [
        { required: true, message: 'Password is required', trigger: 'blur' },
        { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: 'Please confirm password', trigger: 'blur' },
        { validator: validateConfirmPassword, trigger: 'blur' }
      ],
      fullName: [
        { required: true, message: 'Full name is required', trigger: 'blur' }
      ],
      birthDate: [
        { required: true, message: 'Date of birth is required', trigger: 'change' }
      ],
      gender: [
        { required: true, message: 'Gender is required', trigger: 'change' }
      ],
      role: [
        { required: true, message: 'Role is required', trigger: 'change' }
      ]
    }

    const validatePasswordConfirm = (rule, value, callback) => {
      if (value !== passwordForm.newPassword) {
        callback(new Error('Passwords do not match'))
      } else {
        callback()
      }
    }

    const passwordRules = {
      newPassword: [
        { required: true, message: 'Password is required', trigger: 'blur' },
        { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: 'Please confirm password', trigger: 'blur' },
        { validator: validatePasswordConfirm, trigger: 'blur' }
      ]
    }

    const disabledDate = (date) => {
      const today = new Date()
      const minDate = new Date()
      minDate.setFullYear(today.getFullYear() - 120)
      return date > today || date < minDate
    }

    const loadUsers = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.page - 1,
          size: pagination.size
        }
        if (filters.search) params.search = filters.search
        if (filters.role) params.role = filters.role
        if (filters.enabled !== null) params.enabled = filters.enabled

        const response = await http.get('/admin/users', { params })
        users.value = response.data.content || []
        pagination.total = response.data.totalElements || 0
      } catch (error) {
        console.error('Failed to load users:', error)
        ElMessage.error('Failed to load users: ' + (error.response?.data?.message || error.message))
        users.value = []
      } finally {
        loading.value = false
      }
    }

    const resetFilters = () => {
      filters.search = ''
      filters.role = ''
      filters.enabled = null
      pagination.page = 1
      loadUsers()
    }

    const showAddUserDialog = () => {
      isEditing.value = false
      Object.assign(userForm, {
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
        fullName: '',
        birthDate: null,
        gender: '',
        phone: '',
        role: 'PATIENT',
        department: '',
        title: '',
        specialization: '',
        licenseNumber: ''
      })
      userDialogVisible.value = true
    }

    const editUser = (user) => {
      isEditing.value = true
      selectedUserId.value = user.id
      Object.assign(userForm, {
        username: user.username,
        email: user.email,
        fullName: user.fullName,
        birthDate: user.birthDate ? new Date(user.birthDate) : null,
        gender: user.gender || '',
        phone: user.phone,
        role: user.role,
        department: user.department,
        title: user.title,
        specialization: user.specialization,
        licenseNumber: user.licenseNumber
      })
      userDialogVisible.value = true
    }

    const saveUser = async () => {
      if (!userFormRef.value) return

      await userFormRef.value.validate(async (valid) => {
        if (valid) {
          saving.value = true
          try {
            const birthDateStr = userForm.birthDate
                ? new Date(userForm.birthDate).toISOString().split('T')[0]
                : null

            const payload = {
              username: userForm.username,
              email: userForm.email,
              fullName: userForm.fullName,
              birthDate: birthDateStr,
              gender: userForm.gender || null,
              phone: userForm.phone,
              role: userForm.role,
              department: userForm.department,
              title: userForm.title,
              specialization: userForm.specialization,
              licenseNumber: userForm.licenseNumber
            }

            if (!isEditing.value) {
              payload.password = userForm.password
            }

            if (isEditing.value) {
              await http.put(`/admin/users/${selectedUserId.value}`, payload)
              ElMessage.success('User updated successfully')
            } else {
              await http.post('/admin/users', payload)
              ElMessage.success('User created successfully')
            }
            userDialogVisible.value = false
            loadUsers()
          } catch (error) {
            ElMessage.error(error.response?.data?.message || 'Operation failed')
          } finally {
            saving.value = false
          }
        }
      })
    }

    const toggleUserStatus = async (user) => {
      try {
        await http.put(`/admin/users/${user.id}/status`, { enabled: !user.enabled })
        ElMessage.success(`User ${user.enabled ? 'disabled' : 'enabled'} successfully`)
        loadUsers()
      } catch (error) {
        ElMessage.error('Failed to update user status')
      }
    }

    const resetPassword = (user) => {
      selectedUserId.value = user.id
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      resetPasswordDialogVisible.value = true
    }

    const confirmResetPassword = async () => {
      if (!passwordFormRef.value) return

      await passwordFormRef.value.validate(async (valid) => {
        if (valid) {
          resettingPassword.value = true
          try {
            await http.put(`/admin/users/${selectedUserId.value}/password`, {
              password: passwordForm.newPassword
            })
            ElMessage.success('Password reset successfully')
            resetPasswordDialogVisible.value = false
          } catch (error) {
            ElMessage.error('Failed to reset password')
          } finally {
            resettingPassword.value = false
          }
        }
      })
    }

    const deleteUser = async (user) => {
      try {
        await ElMessageBox.confirm(
            `Are you sure you want to delete user "${user.username}"? This action cannot be undone.`,
            'Delete User',
            {
              confirmButtonText: 'Delete',
              cancelButtonText: 'Cancel',
              type: 'warning',
              confirmButtonClass: 'el-button--danger'
            }
        )

        const response = await http.delete(`/admin/users/${user.id}`)
        ElMessage.success(response.data.message || 'User deleted successfully')
        loadUsers()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Delete failed:', error)
          ElMessage.error(error.response?.data?.message || 'Failed to delete user')
        }
      }
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleString()
    }

    const getRoleType = (role) => {
      const types = {
        'ADMIN': 'danger',
        'DOCTOR': 'success',
        'NURSE': 'warning',
        'PATIENT': 'info',
        'RESEARCHER': ''
      }
      return types[role] || 'info'
    }

    onMounted(() => {
      loadUsers()
    })

    return {
      loading,
      saving,
      resettingPassword,
      users,
      filters,
      pagination,
      userDialogVisible,
      resetPasswordDialogVisible,
      isEditing,
      userForm,
      passwordForm,
      userRules,
      passwordRules,
      userFormRef,
      passwordFormRef,
      loadUsers,
      resetFilters,
      showAddUserDialog,
      editUser,
      saveUser,
      toggleUserStatus,
      resetPassword,
      confirmResetPassword,
      deleteUser,
      formatDate,
      getRoleType,
      disabledDate
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-form {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>