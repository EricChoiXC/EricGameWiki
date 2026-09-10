<template>
  <div class="u-list-page">
    <!-- 筛选区：名称模糊查询（or 同时匹配 field_name/field_en_name/field_jp_name） -->
    <el-card class="u-list-page__filter" shadow="never">
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
      </el-row>
    </el-card>

    <!-- 排序和按钮区 -->
    <div class="u-list-page__toolbar">
      <div class="u-list-page__sort">
        <button
          class="u-list-page__sort-btn"
          :class="{ 'is-active': sortField === 'fieldCreateTime' }"
          @click="toggleSort('fieldCreateTime')"
        >
          创建时间
          <el-icon v-if="sortField === 'fieldCreateTime'">
            <CaretTop v-if="sortOrder === 'asc'" />
            <CaretBottom v-else />
          </el-icon>
        </button>
        <button
          class="u-list-page__sort-btn"
          :class="{ 'is-active': sortField === 'fieldPublishDate' }"
          @click="toggleSort('fieldPublishDate')"
        >
          发布日期
          <el-icon v-if="sortField === 'fieldPublishDate'">
            <CaretTop v-if="sortOrder === 'asc'" />
            <CaretBottom v-else />
          </el-icon>
        </button>
      </div>
      <div class="u-list-page__actions">
        <el-button :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="Refresh" @click="onReset">重置</el-button>
        <el-button
          v-if="hasAdminPermission"
          type="primary"
          :icon="Plus"
          @click="onCreate"
        >
          新建
        </el-button>
      </div>
    </div>

    <!-- 列表区 -->
    <el-card class="u-list-page__table" shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="名称" min-width="200">
          <template #default="{ row }">
            <div class="wiki-name">
              <span class="wiki-name__main">{{ row.fieldName || '-' }}</span>
              <span v-if="row.fieldEnName" class="wiki-name__en">{{ row.fieldEnName }}</span>
              <span v-if="row.fieldJpName" class="wiki-name__jp">{{ row.fieldJpName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fieldSimpleName" label="简称" width="120" show-overflow-tooltip />
        <el-table-column label="发布时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.fieldPublishDate) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.fieldCreateTime) }}</template>
        </el-table-column>
        <el-table-column label="开启状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.fieldStatus === 1 ? 'success' : 'info'" size="small">
              {{ row.fieldStatus === 1 ? '开启' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="hasAdminPermission"
              link
              type="primary"
              size="small"
              @click="onToggleStatus(row)"
            >
              {{ row.fieldStatus === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button
              v-if="hasAdminPermission"
              link
              type="primary"
              size="small"
              @click="onEdit(row)"
            >
              编辑
            </el-button>
            <el-button link type="primary" size="small" @click="onMaintain(row)">
              维护内容
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
import { onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CaretBottom, CaretTop, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { wikiMainApi } from '@/api/wiki'
import { useUserStore } from '@/stores/user'
import { PERMISSION_CODE } from '@/utils/constants'

const router = useRouter()
const userStore = useUserStore()

// 按钮权限预判（不可替代后端最终鉴权）
const hasAdminPermission = userStore.hasPermission(PERMISSION_CODE.WIKI_ADMIN)

const filter = reactive({ fieldName: '' })

const sortField = ref('fieldCreateTime')
const sortOrder = ref('desc')

const loading = ref(false)
const tableData = ref([])
const page = reactive({ pageNum: 1, pageSize: 15, total: 0 })

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').split('.')[0]
}

/**
 * 构造列表查询报文：名称模糊查询使用 or 同时匹配 field_name/field_en_name/field_jp_name。
 */
function buildPayload() {
  const query = {
    pageNum: page.pageNum,
    pageSize: page.pageSize,
    needPage: true,
    sortField: sortField.value,
    sortOrder: sortOrder.value
  }
  if (filter.fieldName) {
    query.data = {
      or: [
        { like: { fieldName: filter.fieldName } },
        { like: { fieldEnName: filter.fieldName } },
        { like: { fieldJpName: filter.fieldName } }
      ]
    }
  }
  return { query }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await wikiMainApi.list(buildPayload())
    tableData.value = res.list || []
    page.total = res.query?.total ?? 0
  } finally {
    loading.value = false
  }
}

function toggleSort(field) {
  if (sortField.value === field) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortOrder.value = 'desc'
  }
  onSearch()
}

function onSearch() {
  page.pageNum = 1
  fetchList()
}

function onReset() {
  filter.fieldName = ''
  sortField.value = 'fieldCreateTime'
  sortOrder.value = 'desc'
  onSearch()
}

function onCreate() {
  router.push('/admin/wiki/create')
}

function onEdit(row) {
  router.push(`/admin/wiki/edit/${row.fieldId}`)
}

function onMaintain(row) {
  router.push(`/admin/wiki/${row.fieldId}/data`)
}

async function onToggleStatus(row) {
  const target = row.fieldStatus === 1 ? 0 : 1
  const text = target === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${text}项目「${row.fieldName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await wikiMainApi.updateStatus(row.fieldId, target)
  ElMessage.success(`${text}成功`)
  fetchList()
}

onMounted(fetchList)
onActivated(fetchList)
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

.wiki-name {
  display: flex;
  flex-direction: column;

  &__main {
    font-weight: var(--font-weight-bold);
  }

  &__en,
  &__jp {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
