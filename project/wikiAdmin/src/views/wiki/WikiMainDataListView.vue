<template>
  <div class="u-list-page">
    <!-- 排序和按钮区 -->
    <div class="u-list-page__toolbar">
      <div class="u-list-page__sort">
        <span class="u-list-page__sort-label">排序：field_id 降序</span>
      </div>
      <div class="u-list-page__actions">
        <el-button :icon="ArrowLeft" @click="onBack">返回项目</el-button>
        <el-button type="primary" :icon="Plus" @click="onCreate">新建</el-button>
      </div>
    </div>

    <!-- 列表区 -->
    <el-card class="u-list-page__table" shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="fieldName" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="fieldDataName" label="简称" width="140" show-overflow-tooltip />
        <el-table-column label="数据项类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="dataTypeTagType(row.fieldDataType)" size="small">
              {{ dataTypeLabel(row.fieldDataType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
            <el-button link type="primary" size="small" @click="onDataMaintain(row)">数据维护</el-button>
            <el-button
              v-if="canConfigWikiPage(row)"
              link
              type="primary"
              size="small"
              @click="onWikiPage(row)"
            >
              wiki页面维护
            </el-button>
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
  </div>
</template>

<script setup>
import { computed, onActivated, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { wikiMainDataApi } from '@/api/wiki'

const route = useRoute()
const router = useRouter()

const fieldMainId = computed(() => route.params.mainId || '')

const loading = ref(false)
const tableData = ref([])
const page = reactive({ pageNum: 1, pageSize: 15, total: 0 })

/** 数据项类型标签 */
function dataTypeLabel(type) {
  switch (type) {
    case 'data': return '图鉴类'
    case 'join': return '关联项'
    case 'doc': return '文档类'
    default: return type || '-'
  }
}

function dataTypeTagType(type) {
  switch (type) {
    case 'data': return 'success'
    case 'join': return 'warning'
    case 'doc': return 'info'
    default: return ''
  }
}

/** 仅图鉴类/文档类可配置 wiki 页面 */
function canConfigWikiPage(row) {
  return row.fieldDataType === 'data' || row.fieldDataType === 'doc'
}

async function fetchList() {
  if (!fieldMainId.value) return
  loading.value = true
  try {
    const res = await wikiMainDataApi.listByMain(fieldMainId.value, {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      needPage: true
    })
    tableData.value = res.list || []
    page.total = res.query?.total ?? 0
  } finally {
    loading.value = false
  }
}

function onCreate() {
  router.push(`/admin/wiki/${fieldMainId.value}/data/create`)
}

function onEdit(row) {
  router.push(`/admin/wiki/${fieldMainId.value}/data/edit/${row.fieldId}`)
}

function onDataMaintain(row) {
  // 数据维护入口（TASK-W2-03 范围，此处仅路由占位）
  router.push(`/admin/wiki/${fieldMainId.value}/data`)
}

function onWikiPage(row) {
  router.push(`/admin/wiki/${fieldMainId.value}/data`)
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除数据项「${row.fieldName}」吗？删除将同时删除动态表，且不可恢复。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await wikiMainDataApi.remove(row.fieldId)
  ElMessage.success('删除成功')
  fetchList()
}

function onBack() {
  router.push('/admin/wiki')
}

onMounted(fetchList)
onActivated(fetchList)
</script>

<style lang="scss" scoped>
.u-list-page__filter {
  :deep(.el-card__body) {
    padding: 0;
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
</style>
