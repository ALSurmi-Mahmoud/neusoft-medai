<template>
  <div class="pacs-view">
    <el-row :gutter="20">
      <!-- PACS Status -->
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>PACS Connection Status</span>
              <el-button type="primary" @click="checkStatus" :loading="checkingStatus">
                <el-icon><Refresh /></el-icon>
                Refresh Status
              </el-button>
            </div>
          </template>

          <el-descriptions :column="3" border v-loading="checkingStatus">
            <el-descriptions-item label="Status">
              <el-tag :type="pacsStatus.available ? 'success' : 'danger'">
                {{ pacsStatus.available ? 'Online' : 'Offline' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Enabled">
              <el-tag :type="pacsStatus.configuration?.enabled ? 'success' : 'info'">
                {{ pacsStatus.configuration?.enabled ? 'Yes' : 'No' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="URL">
              {{ pacsStatus.configuration?.url || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="AET">
              {{ pacsStatus.configuration?.aet || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="Timeout">
              {{ pacsStatus.configuration?.timeoutSeconds || '-' }}s
            </el-descriptions-item>
            <el-descriptions-item label="Message">
              {{ pacsStatus.systemInfo?.message || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Pull from PACS -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>📥 Pull Study from PACS</span>
          </template>

          <el-form :model="pullForm" label-width="140px">
            <el-form-item label="Study Instance UID" required>
              <el-input
                  v-model="pullForm.studyInstanceUid"
                  placeholder="1.2.840.113619.2.55..."
              />
            </el-form-item>
            <el-form-item label="Patient ID">
              <el-input v-model="pullForm.patientId" placeholder="Optional" />
            </el-form-item>
            <el-form-item>
              <el-button
                  type="primary"
                  @click="pullStudy"
                  :loading="pulling"
                  :disabled="!pullForm.studyInstanceUid"
              >
                <el-icon><Download /></el-icon>
                Pull Study
              </el-button>
            </el-form-item>
          </el-form>

          <el-alert
              v-if="pullResult"
              :title="pullResult.status"
              :type="pullResult.status === 'queued' ? 'success' : 'warning'"
              :description="pullResult.message"
              show-icon
              style="margin-top: 15px;"
          />
        </el-card>
      </el-col>

      <!-- Push to PACS -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>📤 Push Study to PACS</span>
          </template>

          <el-form :model="pushForm" label-width="100px">
            <el-form-item label="Study ID" required>
              <el-select v-model="pushForm.studyId" placeholder="Select study" style="width: 100%;">
                <el-option
                    v-for="study in availableStudies"
                    :key="study.id"
                    :label="`${study.patientId} - ${study.modality} (ID: ${study.id})`"
                    :value="study.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                  type="success"
                  @click="pushStudy"
                  :loading="pushing"
                  :disabled="!pushForm.studyId"
              >
                <el-icon><Upload /></el-icon>
                Push Study
              </el-button>
            </el-form-item>
          </el-form>

          <el-alert
              v-if="pushResult"
              :title="pushResult.status"
              :type="pushResult.status === 'queued' ? 'success' : 'warning'"
              :description="pushResult.message"
              show-icon
              style="margin-top: 15px;"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- Demo Dataset Info -->
    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>📚 Demo Dataset Information</span>
              <el-button @click="showDemoInfo = !showDemoInfo">
                {{ showDemoInfo ? 'Hide' : 'Show' }} Details
              </el-button>
            </div>
          </template>

          <el-collapse-transition>
            <div v-show="showDemoInfo">
              <div v-if="demoInfo" class="demo-info">
                <h4>{{ demoInfo.name }}</h4>
                <p>{{ demoInfo.description }}</p>

                <el-divider />

                <h5>Why This Dataset?</h5>
                <ul>
                  <li v-for="(value, key) in demoInfo.selectionRationale" :key="key">
                    <strong>{{ formatKey(key) }}:</strong> {{ value }}
                  </li>
                </ul>

                <el-divider />

                <h5>Download Instructions</h5>
                <ol>
                  <li>{{ demoInfo.downloadInstructions?.step1 }}</li>
                  <li>{{ demoInfo.downloadInstructions?.step2 }}</li>
                  <li>{{ demoInfo.downloadInstructions?.step3 }}</li>
                  <li>{{ demoInfo.downloadInstructions?.step4 }}</li>
                </ol>

                <el-divider />

                <h5>Sample Study UIDs for Testing</h5>
                <el-tag
                    v-for="uid in demoInfo.sampleStudyUids"
                    :key="uid"
                    style="margin: 5px; cursor: pointer;"
                    @click="pullForm.studyInstanceUid = uid"
                >
                  {{ uid }}
                </el-tag>
              </div>
            </div>
          </el-collapse-transition>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import http from '../utils/http'
import { ElMessage } from 'element-plus'
import { Refresh, Download, Upload } from '@element-plus/icons-vue'

export default {
  name: 'PacsView',
  components: { Refresh, Download, Upload },
  setup() {
    const checkingStatus = ref(false)
    const pulling = ref(false)
    const pushing = ref(false)
    const showDemoInfo = ref(false)

    const pacsStatus = ref({})
    const demoInfo = ref(null)
    const availableStudies = ref([])
    const pullResult = ref(null)
    const pushResult = ref(null)

    const pullForm = reactive({
      studyInstanceUid: '',
      patientId: ''
    })

    const pushForm = reactive({
      studyId: null
    })

    const checkStatus = async () => {
      checkingStatus.value = true
      try {
        const response = await http.get('/pacs/status')
        pacsStatus.value = response.data
      } catch (error) {
        console.error('Failed to check PACS status:', error)
        ElMessage.error('Failed to check PACS status')
      } finally {
        checkingStatus.value = false
      }
    }

    const loadDemoInfo = async () => {
      try {
        const response = await http.get('/pacs/demo-dataset-info')
        demoInfo.value = response.data
      } catch (error) {
        console.error('Failed to load demo info:', error)
      }
    }

    const loadStudies = async () => {
      try {
        const response = await http.get('/studies', { params: { size: 100 } })
        availableStudies.value = response.data.content || []
      } catch (error) {
        console.error('Failed to load studies:', error)
      }
    }

    const pullStudy = async () => {
      pulling.value = true
      pullResult.value = null
      try {
        const response = await http.post('/pacs/pull', {
          studyInstanceUid: pullForm.studyInstanceUid,
          patientId: pullForm.patientId || null
        })
        pullResult.value = response.data
        ElMessage.success('Pull request submitted')
      } catch (error) {
        console.error('Failed to pull study:', error)
        ElMessage.error('Failed to pull study')
      } finally {
        pulling.value = false
      }
    }

    const pushStudy = async () => {
      pushing.value = true
      pushResult.value = null
      try {
        const response = await http.post('/pacs/push', {
          studyId: pushForm.studyId.toString()
        })
        pushResult.value = response.data
        ElMessage.success('Push request submitted')
      } catch (error) {
        console.error('Failed to push study:', error)
        ElMessage.error('Failed to push study')
      } finally {
        pushing.value = false
      }
    }

    const formatKey = (key) => {
      return key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())
    }

    onMounted(() => {
      checkStatus()
      loadDemoInfo()
      loadStudies()
    })

    return {
      checkingStatus,
      pulling,
      pushing,
      showDemoInfo,
      pacsStatus,
      demoInfo,
      availableStudies,
      pullResult,
      pushResult,
      pullForm,
      pushForm,
      checkStatus,
      pullStudy,
      pushStudy,
      formatKey
    }
  }
}
</script>

<style scoped>
.pacs-view {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.demo-info {
  padding: 15px;
}

.demo-info h4 {
  color: #409eff;
  margin-bottom: 10px;
}

.demo-info h5 {
  color: #606266;
  margin: 15px 0 10px;
}

.demo-info ul, .demo-info ol {
  padding-left: 20px;
}

.demo-info li {
  margin: 8px 0;
  line-height: 1.6;
}
</style>