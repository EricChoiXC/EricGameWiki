<template>
  <div class="wiki-data-list">
    <!-- 筛选区：仅提供"关键字"筛选，图鉴类匹配名称/编号，文档类匹配标题（docs/wiki/图鉴类数据项页面.md / 文档类页面.md） -->
    <el-card class="wiki-data-list__card" shadow="never">
      <template #header>
        <span class="wiki-data-list__title">
          {{ dataItem?.fieldName || dataName }}
          <el-tag class="wiki-data-list__type" size="small" type="info">
            {{ dataTypeLabel(dataItem?.fieldDataType) }}
          </el-tag>
        </span>
      </template>
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="keyword"
            :placeholder="keywordPlaceholder"
            clearable
            style="width: 280px"
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区：不分页显示查询结果，行点击新标签打开详情页 -->
    <el-card class="wiki-data-list__card" shadow="never">
      <el-table
        v-loading="loading"
        :data="records"
        border
        @row-click="onRowClick"
      >
        <el-table-column prop="fieldName" :label="nameLabel" min-width="240" />
        <el-table-column v-if="isData" prop="fieldCode" label="数据项编号" min-width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { wikiRecordApi } from '@/api/wiki'
import { WIKI_DATA_TYPE, WIKI_DATA_TYPE_OPTIONS } from '@/utils/constants'

const route = useRoute()

const simpleName = computed(() => route.params.simpleName || '')
const dataName = computed(() => route.params.dataName || '')

const keyword = ref('')
const records = ref([])
const dataItem = ref(null)
const loading = ref(false)

const isData = computed(() => dataItem.value?.fieldDataType === WIKI_DATA_TYPE.DATA)
const isDoc = computed(() => dataItem.value?.fieldDataType === WIKI_DATA_TYPE.DOC)

const nameLabel = computed(() => (isDoc.value ? '文档标题' : '数据项名称'))

const keywordPlaceholder = computed(() => {
  if (isData.value) return '数据项名称 / 数据项编号'
  if (isDoc.value) return '文档标题'
  return '数据项编号'
})

function dataTypeLabel(type) {
  return WIKI_DATA_TYPE_OPTIONS.find((opt) => opt.value === type)?.label || type || '-'
}

// 记录列表：后端按数据项类型决定关键字匹配规则，不分页
async function loadRecords() {
  loading.value = true
  try {
    const res = await wikiRecordApi.list(simpleName.value, dataName.value, keyword.value)
    dataItem.value = res.data?.dataItem || null
    records.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

function onSearch() {
  loadRecords()
}

// 行点击打开浏览器新标签进入数据项详情页
function onRowClick(row) {
  if (row.fieldId) {
    window.open(`/wiki/${simpleName.value}/${dataName.value}/${row.fieldId}`, '_blank')
  }
}

onMounted(loadRecords)
</script>

<style lang="scss" scoped>
.wiki-data-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);

  &__card {
    :deep(.el-card__header) {
      font-weight: var(--font-weight-bold);
    }
  }

  &__title {
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-sm);
  }

  &__type {
    font-weight: var(--font-weight-regular);
  }
}
</style>
