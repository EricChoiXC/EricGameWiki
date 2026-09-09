<template>
  <div class="u-list-page">
    <!-- 筛选区 -->
    <el-card class="u-list-page__filter" shadow="never">
      <el-row :gutter="0">
        <el-col :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">用户</span>
            <el-input
              v-model="filter.fieldUserName"
              placeholder="请输入用户名称"
              clearable
              @keyup.enter="onSearch"
            />
          </div>
        </el-col>
        <el-col :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">登录时间</span>
            <el-date-picker
              v-model="filter.loginTimeRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </div>
        </el-col>
        <el-col :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">登录IP</span>
            <el-input
              v-model="filter.fieldLoginIp"
              placeholder="请输入登录IP"
              clearable
              @keyup.enter="onSearch"
            />
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="0">
        <el-col :span="8">
          <div class="u-list-page__filter-item">
            <span class="u-list-page__filter-label">登录成功标识</span>
            <el-select v-model="filter.fieldLoginSuccess" placeholder="请选择" clearable>
              <el-option
                v-for="item in LOGIN_SUCCESS_OPTIONS"
                :key="String(item.value)"
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
          :class="{ 'is-active': sortField === 'fieldLoginTime' }"
          @click="toggleSort('fieldLoginTime')"
        >
          登录时间
          <el-icon v-if="sortField === 'fieldLoginTime'">
            <CaretTop v-if="sortOrder === 'asc'" />
            <CaretBottom v-else />
          </el-icon>
        </button>
      </div>
      <div class="u-list-page__actions">
        <el-button :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="Refresh" @click="onReset">重置</el-button>
      </div>
    </div>

    <!-- 列表区 -->
    <el-card class="u-list-page__table" shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="用户" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.fieldUserName || row.fieldUserId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="fieldLoginTime" label="登录时间" min-width="160" />
        <el-table-column prop="fieldLoginIp" label="登录IP" min-width="140" show-overflow-tooltip />
        <el-table-column label="登录成功标识" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.fieldLoginSuccess ? 'success' : 'danger'" size="small">
              {{ row.fieldLoginSuccess ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fieldMessage" label="消息" min-width="240" show-overflow-tooltip />
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
import { CaretBottom, CaretTop, Refresh, Search } from '@element-plus/icons-vue'
import { loginLogApi } from '@/api/org'
import { buildListQuery, dateRange, eq, like } from '@/utils/query'
import { LOGIN_SUCCESS_OPTIONS } from '@/utils/constants'

const filter = reactive({
  fieldUserName: '',
  loginTimeRange: [],
  fieldLoginIp: '',
  fieldLoginSuccess: ''
})

const sortField = ref('fieldLoginTime')
const sortOrder = ref('desc')

const loading = ref(false)
const tableData = ref([])
const page = reactive({ pageNum: 1, pageSize: 15, total: 0 })

function buildPayload() {
  return buildListQuery(
    [
      like('fieldUserName', filter.fieldUserName),
      dateRange('fieldLoginTime', filter.loginTimeRange),
      like('fieldLoginIp', filter.fieldLoginIp),
      filter.fieldLoginSuccess === '' || filter.fieldLoginSuccess == null
        ? null
        : eq('fieldLoginSuccess', filter.fieldLoginSuccess)
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
    const res = await loginLogApi.list(buildPayload())
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
  filter.fieldUserName = ''
  filter.loginTimeRange = []
  filter.fieldLoginIp = ''
  filter.fieldLoginSuccess = ''
  sortField.value = 'fieldLoginTime'
  sortOrder.value = 'desc'
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
