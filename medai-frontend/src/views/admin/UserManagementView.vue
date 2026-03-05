<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="header">
          <span>User Management</span>
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            Create User
          </el-button>
        </div>
      </template>

      <!-- Filters -->
      <el-form :inline="true">
        <el-form-item label="Search">
          <el-input
              v-model="searchQuery"
              placeholder="Username or email"
              clearable
              @clear="loadUsers"
              style="width: 250px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUsers">
            <el-icon><Search /></el-icon>
            Search
          </el-button>
        </el-form-item>
      </el-form>

      <!-- Users Table -->
      <el-table :data="users" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="Username" width="150" />
        <el-table-column prop="fullName" label="Full Name" width="180" />
        <el-table-column prop="email" label="Email" width="200" />
        <el-table-column prop="phone" label="Phone" width="150" />
        <el-table-column label="Roles" width="200">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" style="margin-right: 5px">
              {{ role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editUser(row)">Edit</el-button>
            <el-button size="small" type="warning" @click="resetPassword(row)">
              Reset Password
            </el-button>
            <el-button size="small" type="danger" @click="deleteUser(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadUsers"
          style="margin-top: 20px"
      />
    </el-card>

    <!-- Create/Edit User Dialog -->
    <el-dialog
        v-model="showCreateDialog"
        :title="editingUser ? 'Edit User' : 'Create User'"
        width="600px"
    >
      <el-form :model="userForm" label-width="120px">
        <el-form-item label="Username" required>
          <el-input v-model="userForm.username" :disabled="editingUser" />
        </el-form-item>
        <el-form-item label="Password" :required="!editingUser">
          <el-input v-model="userForm.password" type="password"
                    :placeholder="editingUser ? 'Leave blank to keep current' : ''" />
        </el-form-item>
        <el-form-item label="Full Name" required>
          <el-input v-model="userForm.fullName" />
        </el-form-item>
        <el-form-item label="Email" required>
          <el-input v-model="userForm.email" type="email" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="Roles" required>
          <el-select v-model="userForm.roles" multiple placeholder="Select roles" style="width: 100%">
            <el-option label="Admin" value="ADMIN" />
            <el-option label="Doctor" value="DOCTOR" />
            <el-option label="Nurse" value="NURSE" />
            <el-option label="Patient" value="PATIENT" />
            <el-option label="Researcher" value="RESEARCHER" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">Cancel</el-button>
        <el-button type="primary" @click="saveUser" :loading="saving">
          {{ editingUser ? 'Update' : 'Create' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="showResetDialog" title="Reset Password" width="400px">
      <el-form :model="resetForm" label-width="150px">
        <el-form-item label="New Password" required>
          <el-input v-model="resetForm.newPassword" type="password" />
        </el-form-item>
        <el-form-item label="Confirm Password" required>
          <el-input v-model="resetForm.confirmPassword" type="password" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showResetDialog = false">Cancel</el-button>
        <el-button type="primary" @click="confirmResetPassword">Reset</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/utils/http'

export default {
  name: 'UserManagementView',
  components: { Plus, Search },
  setup() {
    const loading = ref(false)
    const saving = ref(false)
    const users = ref([])
    const currentPage = ref(1)
    const pageSize = ref(20)
    const total = ref(0)
    const searchQuery = ref('')

    const showCreateDialog = ref(false)
    const showResetDialog = ref(false)
    const editingUser = ref(null)
    const selectedUser = ref(null)

    const userForm = reactive({
      username: '',
      password: '',
      fullName: '',
      email: '',
      phone: '',
      roles: []
    })

    const resetForm = reactive({
      newPassword: '',
      confirmPassword: ''
    })

    const loadUsers = async () => {
      loading.value = true
      try {
        const res = await http.get('/admin/user-management', {
          params: {
            page: currentPage.value - 1,
            size: pageSize.value
          }
        })
        users.value = res.data.content
        total.value = res.data.totalElements
      } catch (error) {
        console.error('Failed to load users:', error)
        ElMessage.error('Failed to load users')
      } finally {
        loading.value = false
      }
    }

    const editUser = (user) => {
      editingUser.value = user
      Object.assign(userForm, {
        username: user.username,
        fullName: user.fullName,
        email: user.email,
        phone: user.phone,
        roles: user.roles || []
      })
      userForm.password = ''
      showCreateDialog.value = true
    }

    const saveUser = async () => {
      // Validation
      if (!userForm.username || !userForm.fullName || !userForm.email) {
        ElMessage.error('Please fill in all required fields')
        return
      }

      if (!editingUser.value && !userForm.password) {
        ElMessage.error('Password is required for new users')
        return
      }

      if (userForm.roles.length === 0) {
        ElMessage.error('Please select at least one role')
        return
      }

      saving.value = true
      try {
        if (editingUser.value) {
          await http.put(`/admin/user-management/${editingUser.value.id}`, userForm)
          ElMessage.success('User updated successfully')
        } else {
          await http.post('/admin/user-management', userForm)
          ElMessage.success('User created successfully')
        }
        showCreateDialog.value = false
        resetUserForm()
        loadUsers()
      } catch (error) {
        console.error('Failed to save user:', error)
        ElMessage.error(error.response?.data?.message || 'Operation failed')
      } finally {
        saving.value = false
      }
    }

    const deleteUser = async (user) => {
      try {
        await ElMessageBox.confirm(
            `Are you sure you want to delete user "${user.username}"?`,
            'Confirm Delete',
            { type: 'warning' }
        )

        await http.delete(`/admin/user-management/${user.id}`)
        ElMessage.success('User deleted successfully')
        loadUsers()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Failed to delete user:', error)
          ElMessage.error('Failed to delete user')
        }
      }
    }

    const resetPassword = (user) => {
      selectedUser.value = user
      resetForm.newPassword = ''
      resetForm.confirmPassword = ''
      showResetDialog.value = true
    }

    const confirmResetPassword = async () => {
      if (!resetForm.newPassword || !resetForm.confirmPassword) {
        ElMessage.error('Please fill in both password fields')
        return
      }

      if (resetForm.newPassword !== resetForm.confirmPassword) {
        ElMessage.error('Passwords do not match')
        return
      }

      if (resetForm.newPassword.length < 6) {
        ElMessage.error('Password must be at least 6 characters')
        return
      }

      try {
        await http.post(`/admin/user-management/${selectedUser.value.id}/reset-password`, {
          newPassword: resetForm.newPassword
        })
        ElMessage.success('Password reset successfully')
        showResetDialog.value = false
      } catch (error) {
        console.error('Failed to reset password:', error)
        ElMessage.error('Failed to reset password')
      }
    }

    const resetUserForm = () => {
      editingUser.value = null
      Object.assign(userForm, {
        username: '',
        password: '',
        fullName: '',
        email: '',
        phone: '',
        roles: []
      })
    }

    const formatDate = (date) => {
      if (!date) return ''
      return new Date(date).toLocaleDateString()
    }

    onMounted(() => {
      loadUsers()
    })

    return {
      loading,
      saving,
      users,
      currentPage,
      pageSize,
      total,
      searchQuery,
      showCreateDialog,
      showResetDialog,
      editingUser,
      userForm,
      resetForm,
      loadUsers,
      editUser,
      saveUser,
      deleteUser,
      resetPassword,
      confirmResetPassword,
      formatDate
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>