<template>
  <div class="study-detail-view">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>Study Details</span>
      </template>
      <template #extra>
        <el-button-group>
          <el-button type="success" @click="openViewer">
            <el-icon><Picture /></el-icon>
            Open Viewer
          </el-button>
          <el-button type="primary" @click="createReport">
            <el-icon><Document /></el-icon>
            Create Report
          </el-button>
        </el-button-group>
      </template>
    </el-page-header>

    <el-card style="margin-top: 20px;" v-loading="loading">
      <el-descriptions title="Study Information" :column="3" border>
        <el-descriptions-item label="Study UID" :span="3">
          <span class="uid-text">{{ study.studyUid }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="Status">
          <el-tag :type="getStatusType(study.status)">{{ study.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Modality">
          <el-tag>{{ study.modality }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Study Date">
          {{ formatDate(study.studyDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="Description" :span="3">
          {{ study.description || 'No description' }}
        </el-descriptions-item>
        <el-descriptions-item label="Accession Number">
          {{ study.accessionNumber || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="Created">
          {{ formatDate(study.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="Updated">
          {{ formatDate(study.updatedAt) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <el-descriptions title="Patient Information" :column="3" border v-if="study.patient">
        <el-descriptions-item label="Patient ID">
          {{ study.patient.patientId }}
        </el-descriptions-item>
        <el-descriptions-item label="Name">
          {{ study.patient.name || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="Sex">
          {{ study.patient.sex || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="Birth Date">
          {{ study.patient.birthDate || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h4>Series ({{ study.series?.length || 0 }})</h4>
      <el-table :data="study.series" style="width: 100%">
        <el-table-column prop="seriesNumber" label="#" width="60" />
        <el-table-column prop="seriesUid" label="Series UID">
          <template #default="{ row }">
            <span class="uid-text">{{ row.seriesUid }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="modality" label="Modality" width="100" />
        <el-table-column prop="description" label="Description" />
        <el-table-column prop="manufacturer" label="Manufacturer" />
        <el-table-column prop="imageCount" label="Images" width="100" />
      </el-table>

      <el-divider />

      <h4>Reports</h4>
      <el-table :data="reports" style="width: 100%">
        <el-table-column prop="reportUid" label="Report UID" width="150" />
        <el-table-column prop="title" label="Title" />
        <el-table-column prop="authorName" label="Author" width="150" />
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.finalized ? 'success' : 'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="100">
          <template #default="{ row }">
            <el-button size="small" @click="viewReport(row)">View</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="reports.length === 0" description="No reports yet" />
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../utils/http'
import { Picture, Document } from '@element-plus/icons-vue'

export default {
  name: 'StudyDetailView',
  components: { Picture, Document },
  setup() {
    const route = useRoute()
    const router = useRouter()

    const loading = ref(false)
    const study = ref({})
    const reports = ref([])

    const loadStudy = async () => {
      loading.value = true
      try {
        const response = await http.get(`/studies/${route.params.id}`)
        study.value = response.data

        // Load reports for this study
        const reportsRes = await http.get(`/reports/study/${route.params.id}`)
        reports.value = reportsRes.data || []
      } catch (error) {
        console.error('Failed to load study:', error)
      } finally {
        loading.value = false
      }
    }

    const openViewer = () => {
      router.push(`/viewer/${route.params.id}`)
    }

    const createReport = () => {
      router.push(`/reports/new/${route.params.id}`)
    }

    const viewReport = (report) => {
      router.push(`/reports/${report.id}/edit`)
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleString()
    }

    const getStatusType = (status) => {
      const types = { 'uploaded': 'info', 'processed': 'success', 'archived': 'warning' }
      return types[status] || 'info'
    }

    onMounted(() => {
      loadStudy()
    })

    return {
      loading,
      study,
      reports,
      openViewer,
      createReport,
      viewReport,
      formatDate,
      getStatusType
    }
  }
}
</script>

<style scoped>
.study-detail-view {
  padding: 20px;
}

.uid-text {
  font-family: monospace;
  font-size: 12px;
}

h4 {
  margin: 20px 0 15px;
  color: #303133;
}
</style>