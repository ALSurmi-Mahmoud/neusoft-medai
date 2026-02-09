<template>
  <el-dialog v-model="dialogVisible" title="Notification Settings" width="500px">
    <el-form :model="preferences" label-width="150px">
      <el-divider content-position="left">Channels</el-divider>

      <el-form-item label="In-App Notifications">
        <el-switch v-model="preferences.enableInApp" />
      </el-form-item>

      <el-form-item label="Email Notifications">
        <el-switch v-model="preferences.enableEmail" />
      </el-form-item>

      <el-form-item label="Push Notifications">
        <el-switch v-model="preferences.enablePush" />
      </el-form-item>

      <el-divider content-position="left">Quiet Hours</el-divider>

      <el-form-item label="Enable Quiet Hours">
        <el-switch v-model="preferences.quietHoursEnabled" />
      </el-form-item>

      <el-form-item v-if="preferences.quietHoursEnabled" label="Start Time">
        <el-time-picker v-model="preferences.quietHoursStart" format="HH:mm" />
      </el-form-item>

      <el-form-item v-if="preferences.quietHoursEnabled" label="End Time">
        <el-time-picker v-model="preferences.quietHoursEnd" format="HH:mm" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">Cancel</el-button>
      <el-button type="primary" @click="savePreferences">Save</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ref, reactive, computed, watch } from 'vue'
import http from '../../utils/http'
import { ElMessage } from 'element-plus'

export default {
  name: 'NotificationSettings',
  props: {
    modelValue: Boolean
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const preferences = reactive({
      enableInApp: true,
      enableEmail: true,
      enablePush: false,
      quietHoursEnabled: false,
      quietHoursStart: null,
      quietHoursEnd: null
    })

    const dialogVisible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    const loadPreferences = async () => {
      try {
        const res = await http.get('/notifications/preferences')
        if (res.data) {
          Object.assign(preferences, res.data)
        }
      } catch (e) {
        console.error('Failed to load preferences')
      }
    }

    const savePreferences = async () => {
      try {
        await http.put('/notifications/preferences', preferences)
        ElMessage.success('Settings saved')
        dialogVisible.value = false
      } catch (e) {
        ElMessage.error('Failed to save settings')
      }
    }

    watch(dialogVisible, (val) => {
      if (val) loadPreferences()
    })

    return { dialogVisible, preferences, savePreferences }
  }
}
</script>