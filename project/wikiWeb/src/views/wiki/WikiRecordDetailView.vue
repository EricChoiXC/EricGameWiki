<template>
  <div v-loading="loading" class="wiki-record-detail">
    <el-empty v-if="!loading && !record" description="记录不存在或已删除" />

    <template v-else>
      <!-- 文档类：显示标题和正文（docs/wiki/文档类页面.md 详情页） -->
      <el-card v-if="isDoc" class="wiki-record-detail__card" shadow="never">
        <h1 class="wiki-record-detail__doc-title">{{ record.fieldName }}</h1>
        <div class="wiki-record-detail__doc-content" v-html="record.fieldContext" />
      </el-card>

      <!-- 图鉴类：按 wiki 页面配置以表格显示数据（docs/wiki/图鉴类数据项页面.md 详情页） -->
      <template v-else-if="isData">
        <el-card
          v-for="(block, idx) in renderBlocks"
          :key="idx"
          class="wiki-record-detail__card"
          shadow="never"
        >
          <template #header>
            <span class="wiki-record-detail__block-title">{{ block.title }}</span>
          </template>

          <!-- self：本记录字段值表 -->
          <el-descriptions v-if="block.type === 'self'" :column="1" border>
            <el-descriptions-item
              v-for="field in block.fields"
              :key="field"
              :label="fieldLabel(field, block)"
            >
              {{ resolveRecordField(record, field) }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- join：引用本记录的关联项记录表 -->
          <el-table
            v-else
            :data="relatedRecords[block.fieldDataId] || []"
            border
          >
            <el-table-column
              v-for="field in block.fields"
              :key="field"
              :label="fieldLabel(field, block)"
              min-width="140"
            >
              <template #default="{ row }">
                {{ resolveRecordField(row, field) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { wikiRecordApi } from '@/api/wiki'
import {
  humanizeFieldKey,
  resolveRecordField,
  WIKI_DATA_TYPE,
  PAGE_SOURCE_TYPE
} from '@/utils/constants'

const route = useRoute()

const simpleName = computed(() => route.params.simpleName || '')
const dataName = computed(() => route.params.dataName || '')
const fieldId = computed(() => route.params.fieldId || '')

const loading = ref(false)
const dataItem = ref(null)
const record = ref(null)
const pageConfig = ref(null)
const relatedRecords = ref({})
const details = ref([])
const joinItems = ref({})

const isDoc = computed(() => dataItem.value?.fieldDataType === WIKI_DATA_TYPE.DOC)
const isData = computed(() => dataItem.value?.fieldDataType === WIKI_DATA_TYPE.DATA)

/**
 * 渲染块列表：
 *  - self：按配置 fields 显示本记录字段值；未配置页面时兜底展示全部可见字段
 *  - join：按配置显示引用本记录的关联项记录（displayFields 有序字段优先）
 */
const renderBlocks = computed(() => {
  const infos = pageConfig.value?.displayInfos || []
  const displayFields = pageConfig.value?.displayFields || []

  const blocks = []

  const selfInfos = infos.filter((i) => i.type === PAGE_SOURCE_TYPE.SELF)
  if (selfInfos.length > 0) {
    for (const info of selfInfos) {
      blocks.push({ type: PAGE_SOURCE_TYPE.SELF, title: dataItem.value?.fieldName || '基本信息', fields: info.fields || [] })
    }
  } else {
    blocks.push({ type: PAGE_SOURCE_TYPE.SELF, title: dataItem.value?.fieldName || '基本信息', fields: buildDefaultFields() })
  }

  for (const info of infos.filter((i) => i.type === PAGE_SOURCE_TYPE.JOIN)) {
    const ordered = displayFields.find((f) => f.fieldDataId === info.fieldDataId)
    const fields = ordered?.fields?.length ? ordered.fields : (info.fields || [])
    if (fields.length > 0) {
      blocks.push({
        type: PAGE_SOURCE_TYPE.JOIN,
        fieldDataId: info.fieldDataId,
        title: joinItems.value[info.fieldDataId]?.fieldName || '关联信息',
        fields
      })
    }
  }

  return blocks
})

/**
 * 字段显示名：优先取数据项字段元数据（field_data_json 的 name），
 * 未命中时回退到 humanizeFieldKey 兜底。
 * @param {string} fieldKey 属性键
 * @param {Object} block 渲染块
 * @returns {string}
 */
function fieldLabel(fieldKey, block) {
  const source = block?.type === PAGE_SOURCE_TYPE.JOIN
    ? joinItems.value[block.fieldDataId]?.details
    : details.value
  const detail = source?.find((d) => d.fieldKey === fieldKey)
  return detail?.name || humanizeFieldKey(fieldKey)
}

// 未配置页面配置时的兜底字段：名称/编号 + 全部动态列
function buildDefaultFields() {
  const fields = []
  if (record.value?.fieldName != null) fields.push('fieldName')
  if (record.value?.fieldCode != null) fields.push('fieldCode')
  for (const key of Object.keys(record.value?.fieldData || {})) {
    fields.push(key)
  }
  return fields
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await wikiRecordApi.load(simpleName.value, dataName.value, fieldId.value)
    const data = res.data || {}
    dataItem.value = data.dataItem || null
    record.value = data.record || null
    pageConfig.value = data.pageConfig || null
    relatedRecords.value = data.relatedRecords || {}
    details.value = data.details || []
    joinItems.value = data.joinItems || {}
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<style lang="scss" scoped>
.wiki-record-detail {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);

  &__card {
    :deep(.el-card__header) {
      font-weight: var(--font-weight-bold);
    }
  }

  &__block-title {
    font-size: var(--font-size-body);
  }

  &__doc-title {
    margin-bottom: var(--spacing-lg);
    font-size: var(--font-size-h1);
    color: var(--color-text-primary);
  }

  &__doc-content {
    line-height: var(--line-height-loose);
    color: var(--color-text-regular);

    // 富文本正文基础样式
    :deep(p) {
      margin: var(--spacing-sm) 0;
    }

    :deep(img) {
      max-width: 100%;
    }
  }
}
</style>
