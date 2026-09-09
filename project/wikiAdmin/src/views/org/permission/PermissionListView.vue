<template>
  <div class="u-list-page">
    <!-- 筛选区 -->
    <el-card class="u-list-page__filter" shadow="never">
      <el-row :gutter="0">
        <el-col :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">权限名</span>
            <el-input
              v-model="filter.fieldCode"
              placeholder="请输入权限名"
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

    <!-- 排序和按钮区（权限页无排序、无操作按钮） -->
    <div class="u-list-page__toolbar">
      <div class="u-list-page__sort" />
      <div class="u-list-page__actions">
        <el-button :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="Refresh" @click="onReset">重置</el-button>
      </div>
    </div>

    <!-- 列表区 -->
    <el-card class="u-list-page__table" shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="fieldName" label="中文名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fieldCode" label="权限名" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.fieldStatus)" size="small">
              {{ getStatusLabel(row.fieldStatus) }}
            </el-tag>
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
import { Refresh, Search } from '@element-plus/icons-vue'
import { permissionApi } from '@/api/org'
import { buildListQuery, eq, like } from '@/utils/query'
import { USER_STATUS_OPTIONS } from '@/utils/constants'

const filter = reactive({
  fieldCode: '',
  fieldStatus: ''
})

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
    [like('fieldCode', filter.fieldCode), eq('fieldStatus', filter.fieldStatus)],
    {
      pageNum: page.pageNum,
      pageSize: page.pageSize
    }
  )
}

async function fetchList() {
  loading.value = true
  try {
    const res = await permissionApi.list(buildPayload())
    tableData.value = res.list || []
    page.total = res.query?.total ?? 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.pageNum = 1
  fetchList()
}

function onReset() {
  filter.fieldCode = ''
  filter.fieldStatus = ''
  onSearch()
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
