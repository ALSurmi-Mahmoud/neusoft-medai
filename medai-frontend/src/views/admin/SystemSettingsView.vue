<template>
  <div class="system-settings">
    <el-card v-loading="loading">
      <template #header>
        <span>System Settings</span>
      </template>

      <el-tabs v-model="activeTab">
        <!-- General Settings -->
        <el-tab-pane label="General" name="general">
          <el-form :model="settings" label-width="200px">
            <el-form-item label="Application Name">
              <el-input v-model="settings.appName" />
            </el-form-item>
            <el-form-item label="Application Version">
              <el-input v-model="settings.appVersion" disabled />
            </el-form-item>
            <el-form-item label="Session Timeout (minutes)">
              <el-input-number v-model="settings.sessionTimeoutMinutes" :min="5" :max="480" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Email Settings -->
        <el-tab-pane label="Email/SMTP" name="email">
          <el-form :model="settings" label-width="200px">
            <el-form-item label="Enable Email">
              <el-switch v-model="settings.smtpEnabled" />
            </el-form-item>
            <el-form-item label="SMTP Host">
              <el-input v-model="settings.smtpHost" placeholder="smtp.gmail.com" />
            </el-form-item>
            <el-form-item label="SMTP Port">
              <el-input-number v-model="settings.smtpPort" />
            </el-form-item>
            <el-form-item label="SMTP Username">
              <el-input v-model="settings.smtpUsername" />
            </el-form-item>
            <el-form-item label="SMTP Password">
              <el-input v-model="settings.smtpPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="From Email">
              <el-input v-model="settings.smtpFromEmail" type="email" />
            </el-form-item>
            <el-form-item label="Use TLS">
              <el-switch v-model="settings.smtpUseTls" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="testEmail">Test Email</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Security Settings -->
        <el-tab-pane label="Security" name="security">
          <el-form :model="settings" label-width="220px">
            <el-form-item label="Minimum Password Length">
              <el-input-number v-model="settings.passwordMinLength" :min="6" :max="20" />
            </el-form-item>
            <el-form-item label="Max Login Attempts">
              <el-input-number v-model="settings.maxLoginAttempts" :min="3" :max="10" />
            </el-form-item>
            <el-form-item label="Lockout Duration (minutes)">
              <el-input-number v-model="settings.lockoutDurationMinutes" :min="5" :max="120" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Storage Settings -->
        <el-tab-pane label="Storage" name="storage">
          <el-form :model="settings" label-width="220px">
            <el-form-item label="Max Upload Size (MB)">
              <el-input-number v-model="settings.maxUploadSizeMb" :min="10" :max="1000" />
            </el-form-item>
            <el-form-item label="Storage Quota (GB)">
              <el-input-number v-model="settings.storageQuotaGb" :min="100" :max="10000" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Maintenance -->
        <el-tab-pane label="Maintenance" name="maintenance">
          <el-form :model="settings" label-width="200px">
            <el-form-item label="Maintenance Mode">
              <el-switch
                  v-model="settings.maintenanceMode"
                  active-text="Enabled"
                  inactive-text="Disabled"
              />
            </el-form-item>
            <el-form-item label="Maintenance Message">
              <el-input
                  v-model="settings.maintenanceMessage"
                  type="textarea"
                  :rows="3"
                  placeholder="System is under maintenance. Please try again later."
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <!-- Save Button -->
      <div style="margin-top: 20px; text-align: center">
        <el-button type="primary" @click="saveSettings" :loading="saving" size="large">
          <el-icon><Check /></el-icon>
          Save Settings
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '@/utils/http'

export default {
  name: 'SystemSettingsView',
  components: { Check },
  setup() {
    const loading = ref(false)
    const activeTab = ref('general')
    const saving = ref(false)

    const settings = reactive({
      appName: '',
      appVersion: '',
      smtpEnabled: false,
      smtpHost: '',
      smtpPort: 587,
      smtpUsername: '',
      smtpPassword: '',
      smtpFromEmail: '',
      smtpUseTls: true,
      sessionTimeoutMinutes: 60,
      passwordMinLength: 8,
      maxLoginAttempts: 5,
      lockoutDurationMinutes: 30,
      maxUploadSizeMb: 100,
      storageQuotaGb: 500,
      maintenanceMode: false,
      maintenanceMessage: ''
    })

    const loadSettings = async () => {
      loading.value = true
      try {
        const res = await http.get('/admin/settings')
        Object.assign(settings, res.data)
      } catch (error) {
        console.error('Failed to load settings:', error)
        ElMessage.error('Failed to load settings')
      } finally {
        loading.value = false
      }
    }

    const saveSettings = async () => {
      saving.value = true
      try {
        await http.put('/admin/settings', settings)
        ElMessage.success('Settings saved successfully')
      } catch (error) {
        console.error('Failed to save settings:', error)
        ElMessage.error(error.response?.data?.message || 'Failed to save settings')
      } finally {
        saving.value = false
      }
    }

    const testEmail = () => {
      ElMessage.info('Email test functionality coming soon')
    }

    onMounted(() => {
      loadSettings()
    })

    return {
      loading,
      activeTab,
      saving,
      settings,
      saveSettings,
      testEmail
    }
  }
}
</script>

<style scoped>
.system-settings {
  padding: 20px;
}
</style>