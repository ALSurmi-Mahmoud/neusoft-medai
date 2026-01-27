<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h1>🏥 MedAI Platform</h1>
        <p>Create Your Account</p>
      </div>

      <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="rules"
          class="register-form"
          label-position="top"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Username" prop="username">
              <el-input
                  v-model="registerForm.username"
                  placeholder="Choose a username"
                  :prefix-icon="User"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Email" prop="email">
              <el-input
                  v-model="registerForm.email"
                  placeholder="Enter your email"
                  :prefix-icon="Message"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Password" prop="password">
              <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="Create password"
                  :prefix-icon="Lock"
                  show-password
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Confirm Password" prop="confirmPassword">
              <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="Confirm password"
                  :prefix-icon="Lock"
                  show-password
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Full Name" prop="fullName">
          <el-input
              v-model="registerForm.fullName"
              placeholder="Enter your full name"
              :prefix-icon="UserFilled"
          />
        </el-form-item>

        <!-- ✅ NEW: Birth Date and Gender -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Date of Birth" prop="birthDate">
              <el-date-picker
                  v-model="registerForm.birthDate"
                  type="date"
                  placeholder="Select date of birth"
                  style="width: 100%;"
                  :disabled-date="disabledDate"
                  :prefix-icon="Calendar"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="registerForm.role === 'PATIENT'">
            <el-form-item label="Gender" prop="gender">
              <el-select v-model="registerForm.gender" placeholder="Select gender" style="width: 100%;">
                <el-option label="Male" value="M" />
                <el-option label="Female" value="F" />
                <el-option label="Other" value="O" />
                <el-option label="Prefer not to say" value="U" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-else>
            <el-form-item label="Phone" prop="phone">
              <el-input
                  v-model="registerForm.phone"
                  placeholder="Phone number"
                  :prefix-icon="Phone"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12" v-if="registerForm.role === 'PATIENT'">
            <el-form-item label="Phone" prop="phone">
              <el-input
                  v-model="registerForm.phone"
                  placeholder="Phone number"
                  :prefix-icon="Phone"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Role" prop="role">
              <el-select v-model="registerForm.role" placeholder="Select your role" style="width: 100%;">
                <el-option
                    v-for="role in availableRoles"
                    :key="role.value"
                    :label="role.label"
                    :value="role.value"
                >
                  <div class="role-option">
                    <span>{{ role.label }}</span>
                    <span class="role-desc">{{ role.description }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Doctor-specific fields -->
        <template v-if="registerForm.role === 'DOCTOR'">
          <el-divider>Professional Information</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Department">
                <el-input v-model="registerForm.department" placeholder="e.g., Radiology" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Specialization">
                <el-input v-model="registerForm.specialization" placeholder="e.g., Cardiology" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Title">
                <el-input v-model="registerForm.title" placeholder="e.g., Radiologist" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="License Number">
                <el-input v-model="registerForm.licenseNumber" placeholder="Medical license #" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- Nurse-specific fields -->
        <template v-if="registerForm.role === 'NURSE'">
          <el-divider>Professional Information</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Department">
                <el-input v-model="registerForm.department" placeholder="e.g., Imaging Center" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="License Number">
                <el-input v-model="registerForm.licenseNumber" placeholder="Nursing license #" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-form-item>
          <el-button
              type="primary"
              :loading="loading"
              @click="handleRegister"
              class="register-btn"
          >
            {{ loading ? 'Creating Account...' : 'Create Account' }}
          </el-button>
        </el-form-item>

        <div class="login-link">
          Already have an account?
          <router-link to="/login">Login here</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, UserFilled, Phone, Calendar } from '@element-plus/icons-vue'
import http from '../utils/http'

export default {
  name: 'RegisterView',
  setup() {
    const router = useRouter()
    const authStore = useAuthStore()

    const registerFormRef = ref(null)
    const loading = ref(false)
    const availableRoles = ref([])

    const registerForm = reactive({
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
      fullName: '',
      birthDate: null, // ✅ NEW
      gender: '', // ✅ NEW
      phone: '',
      role: 'PATIENT',
      department: '',
      title: '',
      specialization: '',
      licenseNumber: ''
    })

    const validatePassword = (rule, value, callback) => {
      if (value !== registerForm.password) {
        callback(new Error('Passwords do not match'))
      } else {
        callback()
      }
    }

    const rules = {
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
        { validator: validatePassword, trigger: 'blur' }
      ],
      fullName: [
        { required: true, message: 'Full name is required', trigger: 'blur' }
      ],
      birthDate: [
        { required: true, message: 'Date of birth is required', trigger: 'change' }
      ],
      gender: [
        // ✅ Only required for PATIENT role
        {
          required: false,  // Changed from true
          message: 'Gender is required',
          trigger: 'change'
        }
      ],
      role: [
        { required: true, message: 'Please select a role', trigger: 'change' }
      ]
    }

    const disabledDate = (date) => {
      // Disable future dates and dates more than 120 years ago
      const today = new Date()
      const minDate = new Date()
      minDate.setFullYear(today.getFullYear() - 120)
      return date > today || date < minDate
    }

    const loadRoles = async () => {
      try {
        const response = await http.get('/auth/roles')
        availableRoles.value = response.data.roles || []
      } catch (error) {
        availableRoles.value = [
          { value: 'PATIENT', label: 'Patient', description: 'View own reports' },
          { value: 'DOCTOR', label: 'Doctor', description: 'Full clinical access' },
          { value: 'NURSE', label: 'Nurse', description: 'Upload and schedule' }
        ]
      }
    }

    const handleRegister = async () => {
      if (!registerFormRef.value) return

      await registerFormRef.value.validate(async (valid) => {
        if (valid) {
          loading.value = true

          try {
            // Format birth date to YYYY-MM-DD
            const birthDateStr = registerForm.birthDate
                ? new Date(registerForm.birthDate).toISOString().split('T')[0]
                : null

            const response = await http.post('/auth/register', {
              username: registerForm.username,
              email: registerForm.email,
              password: registerForm.password,
              fullName: registerForm.fullName,
              birthDate: birthDateStr, // ✅ NEW
              gender: registerForm.gender || null, // ✅ NEW
              phone: registerForm.phone,
              role: registerForm.role,
              department: registerForm.department,
              title: registerForm.title,
              specialization: registerForm.specialization,
              licenseNumber: registerForm.licenseNumber
            })

            const data = response.data
            localStorage.setItem('accessToken', data.accessToken)
            localStorage.setItem('refreshToken', data.refreshToken)
            localStorage.setItem('user', JSON.stringify(data.user))

            authStore.$patch({
              accessToken: data.accessToken,
              refreshToken: data.refreshToken,
              user: data.user
            })

            ElMessage.success('Account created successfully!')
            router.push('/dashboard')
          } catch (error) {
            ElMessage.error(error.response?.data?.message || 'Registration failed')
          } finally {
            loading.value = false
          }
        }
      })
    }

    onMounted(() => {
      loadRoles()
    })

    return {
      registerFormRef,
      registerForm,
      rules,
      loading,
      availableRoles,
      handleRegister,
      disabledDate,
      User,
      Lock,
      Message,
      UserFilled,
      Phone,
      Calendar
    }
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.register-box {
  width: 100%;
  max-width: 700px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
}

.register-header p {
  color: #666;
  font-size: 16px;
}

.register-form {
  margin-top: 20px;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
}

.login-link a {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}

.role-option {
  display: flex;
  flex-direction: column;
}

.role-desc {
  font-size: 12px;
  color: #999;
}
</style>