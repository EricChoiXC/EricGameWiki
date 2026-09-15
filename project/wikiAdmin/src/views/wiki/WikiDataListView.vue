<template>
  <div class="u-list-page">
    <!-- 筛选区：图鉴类/文档类按名称/编号模糊；关联项按关联数据模糊 -->
    <el-card v-if="joinFilterDetails.length > 0" class="u-list-page__filter" shadow="never">
      <el-row :gutter="0">
        <el-col v-for="detail in joinFilterDetails" :key="detail.dataName" :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">{{ detail.name }}</span>
            <el-input
              v-model="filter[detail.dataName]"
              :placeholder="`请输入${detail.name}`"
              clearable
              @keyup.enter="onSearch"
            />
          </div>
        </el-col>
      </el-row>
    </el-card>
    <el-card v-else-if="isDataOrDoc" class="u-list-page__filter" shadow="never">
      <el-row :gutter="0">
        <el-col :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">名称</span>
            <el-input
              v-model="filter.fieldName"
              placeholder="请输入名称"
              clearable
              @keyup.enter="onSearch"
            />
          </div>
        </el-col>
        <el-col :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">编号</span>
            <el-input
              v-model="filter.fieldCode"
              placeholder="请输入编号"
              clearable
              @keyup.enter="onSearch"
            />
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 排序和按钮区 -->
    <div class="u-list-page__toolbar">
      <div class="u-list-page__sort">
        <span v-if="dataTitle" class="u-list-page__title">{{ dataTitle }}（{{ typeLabel }}）</span>
        <span class="u-list-page__sort-label">{{ sortLabel }}</span>
      </div>
      <div class="u-list-page__actions">
        <el-button :icon="ArrowLeft" @click="onBack">返回数据项</el-button>
        <el-button :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="Refresh" @click="onReset">重置</el-button>

        <template v-if="isDataOrDoc">
          <el-button type="primary" :icon="Plus" @click="onCreate">新建</el-button>
        </template>

        <template v-else-if="dataType === 'join'">
          <el-button type="primary" :icon="Upload" @click="openImport">导入</el-button>
          <el-button :icon="Download" @click="onExport">导出</el-button>
          <el-button
            type="danger"
            :icon="Delete"
            :disabled="selectedIds.length === 0"
            @click="onBatchDelete"
          >
            批量删除
          </el-button>
        </template>
      </div>
    </div>

    <!-- 列表区 -->
    <el-card class="u-list-page__table" shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        @selection-change="onSelectionChange"
      >
        <!-- 关联项：多选框 -->
        <el-table-column v-if="dataType === 'join'" type="selection" width="50" align="center" />

        <!-- 图鉴类/文档类：名称 + 编号 -->
        <template v-if="isDataOrDoc">
          <el-table-column prop="fieldName" label="名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="fieldCode" label="编号" min-width="160" show-overflow-tooltip />
        </template>

        <!-- 关联项：所有数据明细字段（长文本除外） -->
        <template v-else>
          <el-table-column
            v-for="detail in tableDetails"
            :key="detail.dataName"
            :label="detail.name"
            :min-width="columnWidth(detail)"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <template v-if="detail.type === 'join'">
                <span :title="displayJoinCode(row, detail)">
                  {{ displayJoinName(row, detail) }}
                </span>
              </template>
              <span v-else-if="detail.type === 'boolean'">
                {{ row.fieldData?.[columnKey(detail)] == null ? '-' : (Number(row.fieldData[columnKey(detail)]) === 1 ? '是' : '否') }}
              </span>
              <span v-else>{{ displayCell(row, detail) }}</span>
            </template>
          </el-table-column>
        </template>

        <el-table-column label="操作" :width="isDataOrDoc ? 140 : 120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.pageNum"
        v-model:page-size="page.pageSize"
        :total="page.total"
        :page-sizes="[15, 30, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <!-- 关联项导入弹窗 -->
    <WikiDataImportDialog
      v-model="importVisible"
      :field-data-id="dataId"
      :simple-name="simpleName"
      :data-name="meta?.fieldDataName || ''"
      :title="dataTitle"
      @imported="fetchList"
    />
  </div>
</template>

<script setup>
import { computed, onActivated, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Delete,
  Download,
  Plus,
  Refresh,
  Search,
  Upload
} from '@element-plus/icons-vue'
import { wikiDataApi, wikiMainApi, wikiMainDataApi } from '@/api/wiki'
import { buildListQuery, like } from '@/utils/query'
import WikiDataImportDialog from './WikiDataImportDialog.vue'

const route = useRoute()
const router = useRouter()

const fieldMainId = computed(() => route.params.mainId || '')
const dataId = computed(() => route.params.dataId || '')

const loading = ref(false)
const tableData = ref([])
const page = reactive({ pageNum: 1, pageSize: 15, total: 0 })
const selectedIds = ref([])

/** 数据项元数据 */
const meta = ref(null)
const dataType = computed(() => meta.value?.fieldDataType || '')
const isDataOrDoc = computed(() => dataType.value === 'data' || dataType.value === 'doc')
const dataTitle = computed(() => meta.value?.fieldName || '')

/** 项目简称（导入附件 field_model_name = ${项目简称}::${数据项简称}） */
const simpleName = ref('')

/** 筛选条件：图鉴类/文档类固定 fieldName/fieldCode；关联项按 join 明细 dataName 生成 */
const filter = reactive({ fieldName: '', fieldCode: '' })

/** 关联项筛选用 join 明细列 */
const joinFilterDetails = computed(() =>
  dataType.value === 'join' ? detailsOfType('join') : []
)

/** 关联项列表展示列（长文本/附件除外） */
const tableDetails = computed(() =>
  dataType.value === 'join' ? detailRows.value.filter((d) => !['blob', 'attachment'].includes(d.type)) : []
)

/** 全部明细行（用于 join 筛选与列渲染） */
const detailRows = ref([])

const importVisible = ref(false)

const sortLabel = computed(() => {
  if (dataType.value === 'join') return '排序：field_id 降序'
  return '排序：编号降序（field_code DESC）'
})

const typeLabel = computed(() => {
  switch (dataType.value) {
    case 'data': return '图鉴类'
    case 'join': return '关联项'
    case 'doc': return '文档类'
    default: return ''
  }
})

function detailsOfType(type) {
  return detailRows.value.filter((d) => d.type === type)
}

/** 动态列在 fieldData 中的 key（小驼峰）：join 类型带 Id 后缀 */
function columnKey(detail) {
  const prefix = 'field' + upperFirst(detail.dataName)
  return detail.type === 'join' ? prefix + 'Id' : prefix
}

function upperFirst(str) {
  if (!str) return str
  return str.charAt(0).toUpperCase() + str.slice(1)
}

function columnWidth(detail) {
  if (detail.type === 'text' || detail.type === 'enum') return 140
  if (detail.type === 'join') return 140
  return 120
}

function displayCell(row, detail) {
  const value = row.fieldData?.[columnKey(detail)]
  if (value == null || value === '') return '-'
  return String(value)
}

function displayJoinName(row, detail) {
  const key = 'field' + upperFirst(detail.dataName) + 'Name'
  const value = row.fieldData?.[key]
  return value == null || value === '' ? '-' : String(value)
}

function displayJoinCode(row, detail) {
  const key = 'field' + upperFirst(detail.dataName) + 'Code'
  const value = row.fieldData?.[key]
  return value == null || value === '' ? '' : String(value)
}

/**
 * 构造列表查询报文。
 * 图鉴类/文档类：名称/编号 or 模糊；关联项：每个 join 明细列对 fieldName/fieldCode 联表模糊。
 */
function buildPayload() {
  const conditions = []
  if (isDataOrDoc.value) {
    if (filter.fieldName) {
      conditions.push({ or: [{ like: { fieldName: filter.fieldName } }] })
    }
    if (filter.fieldCode) {
      conditions.push({ or: [{ like: { fieldCode: filter.fieldCode } }] })
    }
  } else if (dataType.value === 'join') {
    for (const detail of joinFilterDetails.value) {
      const value = filter[detail.dataName]
      if (!value) continue
      const keyName = 'field' + upperFirst(detail.dataName) + 'Name'
      const keyCode = 'field' + upperFirst(detail.dataName) + 'Code'
      conditions.push({ or: [like(keyName, value), like(keyCode, value)] })
    }
  }
  const query = buildListQuery(conditions, {
    pageNum: page.pageNum,
    pageSize: page.pageSize,
    needPage: true,
    sortField: dataType.value === 'join' ? 'fieldId' : 'fieldCode',
    sortOrder: 'desc'
  })
  return { data: { fieldDataId: dataId.value }, query: query.query }
}

async function fetchList() {
  if (!dataId.value) return
  loading.value = true
  try {
    const res = await wikiDataApi.list(buildPayload())
    tableData.value = res.list || []
    page.total = res.query?.total ?? 0
  } finally {
    loading.value = false
  }
}

function onSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.fieldId)
}

function onSearch() {
  page.pageNum = 1
  fetchList()
}

function onReset() {
  filter.fieldName = ''
  filter.fieldCode = ''
  for (const detail of detailRows.value) {
    filter[detail.dataName] = ''
  }
  onSearch()
}

function onCreate() {
  router.push(`/admin/wiki/${fieldMainId.value}/data-item/${dataId.value}/create`)
}

function onEdit(row) {
  router.push(`/admin/wiki/${fieldMainId.value}/data-item/${dataId.value}/edit/${row.fieldId}`)
}

async function onDelete(row) {
  const label = row.fieldName || row.fieldCode || '该记录'
  try {
    await ElMessageBox.confirm(`确定删除「${label}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await wikiDataApi.remove(row.fieldId, dataId.value)
  ElMessage.success('删除成功')
  fetchList()
}

async function onBatchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条记录吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await wikiDataApi.batchDelete({
    data: { fieldDataId: dataId.value, fieldIds: selectedIds.value }
  })
  ElMessage.success('批量删除成功')
  fetchList()
}

function openImport() {
  importVisible.value = true
}

/**
 * 导出：全量数据为 xlsx 文件流下载。
 */
async function onExport() {
  try {
    const res = await wikiDataApi.export({ data: { fieldDataId: dataId.value } })
    downloadBlob(res, `${meta.value?.fieldDataName || 'wiki'}_导出.xlsx`)
  } catch {
    // 错误提示由响应拦截器统一处理
  }
}

function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

function onBack() {
  router.push(`/admin/wiki/${fieldMainId.value}/data`)
}

/**
 * 加载数据项元数据：类型、明细定义；并加载项目简称（导入附件 model name 使用）。
 */
async function loadMeta() {
  const [dataRes, mainRes] = await Promise.all([
    wikiMainDataApi.load(dataId.value),
    fieldMainId.value ? wikiMainApi.load(fieldMainId.value) : Promise.resolve({ data: {} })
  ])
  meta.value = dataRes.data || {}
  simpleName.value = mainRes.data?.fieldSimpleName || ''
  detailRows.value = meta.value.fieldDataJson || []
}

onMounted(async () => {
  await loadMeta()
  await fetchList()
})

onActivated(async () => {
  if (meta.value) {
    await fetchList()
  }
})
</script>

<style lang="scss" scoped>
.u-list-page__filter {
  :deep(.el-card__body) {
    padding: 0;
  }

  .u-list-page__filter-item {
    :deep(.el-select) {
      width: 100%;
    }
  }
}

.u-list-page__table {
  :deep(.el-card__body) {
    padding: var(--spacing-md);
  }
}

.u-list-page__sort-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.u-list-page__title {
  font-size: 14px;
  font-weight: var(--font-weight-bold);
  margin-right: var(--spacing-sm);
}
</style>
