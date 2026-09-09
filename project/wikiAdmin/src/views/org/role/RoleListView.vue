<template>
  <div class="u-list-page">
    <!-- 筛选区 -->
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
        <el-col :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">状态</span>
            <el-select v-model="filter.fieldStatus" placeholder="请选择状态" clearable>
              <el-option
                v-for="item in USER_STATUS_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
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
      </div>
      <div class="u-list-page__actions">
        <el-button :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="Refresh" @click="onReset">重置</el-button>
        <el-button
          v-if="hasRolePermission"
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
        <el-table-column prop="fieldName" label="名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="fieldCode" label="编号" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.fieldStatus)" size="small">
              {{ getStatusLabel(row.fieldStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fieldCreateTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onView(row)">查看</el-button>
            <el-button
              v-if="hasRolePermission"
              link
              type="primary"
              size="small"
              @click="onEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="hasRolePermission"
              link
              :type="row.fieldStatus === 'ENABLED' ? 'warning' : 'success'"
              size="small"
              @click="onToggleStatus(row)"
            >
              {{ row.fieldStatus === 'ENABLED' ? '停用' : '启用' }}
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
import { roleApi } from '@/api/org'
import { useUserStore } from '@/stores/user'
import { buildListQuery, eq, like } from '@/utils/query'
import { PERMISSION_CODE, USER_STATUS_OPTIONS } from '@/utils/constants'

const router = useRouter()
const userStore = useUserStore()

const hasRolePermission = userStore.hasPermission(PERMISSION_CODE.ROLE)

const filter = reactive({
  fieldName: '',
  fieldCode: '',
  fieldStatus: ''
})

const sortField = ref('fieldCreateTime')
const sortOrder = ref('desc')

const loading = ref(false)
const tableData = ref([])
const page = reactive({ pageNum: 1, pageSize: 15, total: 0 })

function getStatusLabel(status) {
  return USER_STATUS_OPTIONS.find((i) => i.value === status)?.label || status || '-'
}

function getStatusType(status) {
  return USER_STATUS_OPTIONS.find((i) => i.value === status)?.type || 'info'
}

function buildPayload() {
  return buildListQuery(
    [
      like('fieldName', filter.fieldName),
      like('fieldCode', filter.fieldCode),
      eq('fieldStatus', filter.fieldStatus)
    ],
    {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      sortField: sortField.value,
      sortOrder: sortOrder.value
    }
  )
}

async function fetchList() {
  loading.value = true
  try {
    const res = await roleApi.list(buildPayload())
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
  filter.fieldCode = ''
  filter.fieldStatus = ''
  sortField.value = 'fieldCreateTime'
  sortOrder.value = 'desc'
  onSearch()
}

function onCreate() {
  router.push('/admin/org/role/create')
}

function onEdit(row) {
  router.push(`/admin/org/role/edit/${row.fieldId}`)
}

function onView(row) {
  router.push(`/admin/org/role/detail/${row.fieldId}`)
}

async function onToggleStatus(row) {
  const next = row.fieldStatus === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const action = next === 'ENABLED' ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${action}角色「${row.fieldName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await roleApi.updateStatus(row.fieldId, next)
  ElMessage.success(`${action}成功`)
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
</style>
