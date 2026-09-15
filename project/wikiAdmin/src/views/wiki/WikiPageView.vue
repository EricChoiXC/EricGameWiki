<template>
  <div class="wiki-page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="wiki-page__header">
          <span>{{ pageTitle }}</span>
          <el-button :icon="ArrowLeft" @click="onBack">返回数据项</el-button>
        </div>
      </template>

      <!-- 显示信息 -->
      <div class="wiki-page__section">
        <div class="wiki-page__section-title">显示信息</div>
        <div class="wiki-page__section-tip">
          显示信息可选源为本数据项的数据明细，和所有包含该数据项的「关联类」数据项。
        </div>
        <el-table :data="sources" border>
          <el-table-column label="显示来源" min-width="200">
            <template #default="{ row }">
              <span>{{ row.type === 'self' ? '本数据项' : '关联类数据项' }}（{{ row.name }}）</span>
            </template>
          </el-table-column>
          <el-table-column label="显示" width="90" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" @change="onToggle(row)" />
            </template>
          </el-table-column>
          <el-table-column label="显示字段" min-width="400">
            <template #default="{ row }">
              <el-checkbox-group
                v-if="row.enabled"
                v-model="row.fields"
                @change="onJoinFieldsChange(row)"
              >
                <el-checkbox v-for="detail in row.details" :key="detail.fieldKey" :value="detail.fieldKey">
                  {{ detail.name }}
                </el-checkbox>
              </el-checkbox-group>
              <span v-else class="wiki-page__placeholder">未启用</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 显示字段：关联类数据项按序选择 -->
      <div v-if="enabledJoinSources.length > 0" class="wiki-page__section">
        <div class="wiki-page__section-title">显示字段</div>
        <div class="wiki-page__section-tip">
          选择「关联类」数据项后，按序选择要显示的该关联类数据项的数据明细项。
        </div>
        <div
          v-for="source in enabledJoinSources"
          :key="source.fieldDataId"
          class="wiki-page__field-row"
        >
          <span class="wiki-page__field-label">{{ source.name }}</span>
          <el-select
            v-model="orderedFields[source.fieldDataId]"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="按序选择要显示的字段"
            style="width: 100%"
          >
            <el-option
              v-for="detail in source.details"
              :key="detail.fieldKey"
              :label="detail.name"
              :value="detail.fieldKey"
            />
          </el-select>
        </div>
      </div>

      <div class="wiki-page__actions">
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
        <el-button @click="onBack">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { wikiPageApi } from '@/api/wiki'

const route = useRoute()
const router = useRouter()

const fieldMainId = computed(() => route.params.mainId || '')
const dataId = computed(() => route.params.dataId || '')

const loading = ref(false)
const saving = ref(false)

/** 页面配置初始化数据：{ dataItem, joinItems } */
const initData = ref(null)

/** 显示信息源：本数据项(self) + 包含该数据项的关联类数据项(join) */
const sources = ref([])

/** 显示字段：关联类数据项 fieldDataId → 按序选择的字段 key 数组 */
const orderedFields = reactive({})

const pageTitle = computed(() => {
  const name = initData.value?.dataItem?.fieldName || ''
  return `wiki 页面维护${name ? '：' + name : ''}`
})

const enabledJoinSources = computed(() =>
  sources.value.filter((s) => s.enabled && s.type === 'join')
)

/**
 * 加载初始化数据（API-W303）+ 已保存配置（API-W301）。
 * 未配置过页面时 load 返回 NOT_FOUND，按空配置处理。
 */
async function loadPage() {
  loading.value = true
  try {
    const initRes = await wikiPageApi.init(dataId.value)
    initData.value = initRes.data || {}

    let config = null
    try {
      const pageRes = await wikiPageApi.load(dataId.value)
      config = pageRes.data?.fieldWikiPage || null
    } catch {
      config = null
    }
    buildSources(config)
  } finally {
    loading.value = false
  }
}

/**
 * 由初始化数据 + 已保存配置构建页面状态：
 *  - displayInfos → 各源的 enabled 与字段勾选
 *  - displayFields → 关联类数据项的有序字段
 */
function buildSources(config) {
  const infos = {}
  for (const info of config?.displayInfos || []) {
    infos[info.fieldDataId] = info
  }
  const fieldMap = {}
  for (const field of config?.displayFields || []) {
    fieldMap[field.fieldDataId] = field.fields || []
  }

  const dataItem = initData.value.dataItem || {}
  const selfSource = {
    fieldDataId: dataItem.fieldId,
    type: 'self',
    name: dataItem.fieldName || '',
    details: dataItem.details || [],
    enabled: !!infos[dataItem.fieldId],
    fields: infos[dataItem.fieldId]?.fields ? [...infos[dataItem.fieldId].fields] : []
  }

  const joinSources = (initData.value.joinItems || []).map((item) => ({
    fieldDataId: item.fieldId,
    type: 'join',
    name: item.fieldName || '',
    details: item.details || [],
    enabled: !!infos[item.fieldId],
    fields: infos[item.fieldId]?.fields ? [...infos[item.fieldId].fields] : []
  }))

  sources.value = [selfSource, ...joinSources]

  for (const item of initData.value.joinItems || []) {
    orderedFields[item.fieldId] =
      (fieldMap[item.fieldId] && fieldMap[item.fieldId].length)
        ? [...fieldMap[item.fieldId]]
        : (infos[item.fieldId]?.fields ? [...infos[item.fieldId].fields] : [])
  }
}

/** 开关变化：关闭时清空已选字段 */
function onToggle(source) {
  if (!source.enabled) {
    source.fields = []
    if (source.type === 'join') {
      orderedFields[source.fieldDataId] = []
    }
  }
}

/** 关联类数据项字段勾选变化：若"显示字段"尚未有序选择，则按勾选顺序默认填充 */
function onJoinFieldsChange(source) {
  if (source.type !== 'join' || !source.enabled) return
  const ordered = orderedFields[source.fieldDataId] || []
  if (ordered.length === 0) {
    orderedFields[source.fieldDataId] = [...source.fields]
  }
}

/**
 * 保存（API-W302）：
 *  - displayInfos：启用且勾选了字段的来源
 *  - displayFields：启用且按序选择了字段的关联类数据项
 */
async function onSave() {
  const displayInfos = sources.value
    .filter((s) => s.enabled && s.fields.length > 0)
    .map((s) => ({ type: s.type, fieldDataId: s.fieldDataId, fields: s.fields }))

  const displayFields = sources.value
    .filter((s) => s.enabled && s.type === 'join' && (orderedFields[s.fieldDataId] || []).length > 0)
    .map((s) => ({ fieldDataId: s.fieldDataId, fields: orderedFields[s.fieldDataId] }))

  saving.value = true
  try {
    await wikiPageApi.save({
      data: {
        fieldDataId: dataId.value,
        fieldWikiPage: { displayInfos, displayFields }
      }
    })
    ElMessage.success('保存成功')
    router.push(`/admin/wiki/${fieldMainId.value}/data`)
  } finally {
    saving.value = false
  }
}

function onBack() {
  router.push(`/admin/wiki/${fieldMainId.value}/data`)
}

onMounted(loadPage)
</script>

<style lang="scss" scoped>
.wiki-page {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: var(--font-weight-bold);
  }

  &__section {
    margin-bottom: var(--spacing-lg);

    &-title {
      font-weight: var(--font-weight-bold);
      margin-bottom: var(--spacing-xs);
    }

    &-tip {
      margin-bottom: var(--spacing-md);
      font-size: var(--font-size-sm);
      color: var(--el-text-color-secondary);
    }
  }

  &__field-row {
    display: flex;
    align-items: flex-start;
    gap: var(--spacing-md);
    margin-bottom: var(--spacing-md);

    .el-select {
      flex: 1;
    }
  }

  &__field-label {
    flex-shrink: 0;
    min-width: 160px;
    line-height: 32px;
    font-size: var(--font-size-sm);
  }

  &__placeholder {
    color: var(--el-text-color-placeholder);
    font-size: var(--font-size-sm);
  }

  &__actions {
    margin-top: var(--spacing-md);
  }
}
</style>
