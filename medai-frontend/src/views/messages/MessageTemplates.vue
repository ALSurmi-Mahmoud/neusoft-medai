<template>
  <el-dialog v-model="dialogVisible" title="Message Templates" width="600px">
    <el-input v-model="search" placeholder="Search templates..." clearable style="margin-bottom: 16px;">
      <template #prefix><el-icon><Search /></el-icon></template>
    </el-input>

    <div class="templates-list">
      <div
          v-for="template in filteredTemplates"
          :key="template.id"
          class="template-item"
          @click="selectTemplate(template)"
      >
        <div class="template-header">
          <h4>{{ template.name }}</h4>
          <el-tag v-if="template.isSystem" size="small" type="info">System</el-tag>
          <el-tag size="small">{{ template.category }}</el-tag>
        </div>
        <p class="template-content">{{ template.content }}</p>
      </div>

      <el-empty v-if="filteredTemplates.length === 0" description="No templates found" />
    </div>
  </el-dialog>
</template>

<script>
import { ref, computed, watch, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import http from '../../utils/http'

export default {
  name: 'MessageTemplates',
  components: { Search },
  props: {
    modelValue: Boolean
  },
  emits: ['update:modelValue', 'select'],
  setup(props, { emit }) {
    const templates = ref([])
    const search = ref('')

    const dialogVisible = computed({
      get: () => props.modelValue,
      set: (val) => emit('update:modelValue', val)
    })

    const filteredTemplates = computed(() => {
      if (!search.value) return templates.value
      const q = search.value.toLowerCase()
      return templates.value.filter(t =>
          t.name?.toLowerCase().includes(q) ||
          t.content?.toLowerCase().includes(q)
      )
    })

    const loadTemplates = async () => {
      try {
        const res = await http.get('/message-templates')
        templates.value = res.data
      } catch (e) {
        console.error('Failed to load templates')
      }
    }

    const selectTemplate = (template) => {
      emit('select', template)
    }

    watch(dialogVisible, (val) => {
      if (val) loadTemplates()
    })

    return { dialogVisible, templates, search, filteredTemplates, selectTemplate }
  }
}
</script>

<style scoped>
.templates-list { max-height: 400px; overflow-y: auto; }
.template-item { padding: 12px; border: 1px solid #dcdfe6; border-radius: 4px; margin-bottom: 8px; cursor: pointer; transition: all 0.2s; }
.template-item:hover { background: #f5f7fa; border-color: #409eff; }
.template-header { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.template-header h4 { margin: 0; flex: 1; }
.template-content { margin: 0; font-size: 13px; color: #606266; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
</style>