<template>
  <div class="user-detail">
    <el-card shadow="never">
      <template #header>
        <div class="user-detail__header">
          <span>用户详情</span>
          <el-button :icon="ArrowLeft" @click="onBack">返回</el-button>
        </div>
      </template>

      <!-- 表格：按表形式列出对象数据（规范第 14 章 1:3:1:3:1:3） -->
      <div class="u-form-table" v-loading="loading">
        <el-row v-for="(row, rIdx) in formRows" :key="rIdx">
          <template v-for="(item, cIdx) in row" :key="rIdx + '-' + cIdx">
            <el-col :span="1" class="u-form-table__label">{{ item.label }}</el-col>
            <el-col :span="3" class="u-form-table__value">{{ formatValue(item) }}</el-col>
          </template>
          <!-- 不足 3 列时补齐栅栏 -->
          <template v-if="row.length < 3">
            <el-col :span="1" class="u-form-table__label" />
            <el-col :span="3" class="u-form-table__value" />
            <el-col
              v-if="row.length < 2"
              :span="1"
              class="u-form-table__label"
            />
            <el-col
              v-if="row.length < 2"
              :span="3"
              class="u-form-table__value"
            />
          </template>
        </el-row>
      </div>
    </el-card>

    <!-- 明细表：用户登录记录（规范第 14 章） -->
    <el-card class="user-detail__log" shadow="never">
      <template #header>
        <span class="user-detail__section-title">登录记录</span>
      </template>

      <div class="u-detail-table">
        <el-table v-loading="logLoading" :data="logData" border stripe>
          <el-table-column prop="fieldLoginTime" label="登录时间" min-width="160" align="center" />
          <el-table-column prop="fieldLoginIp" label="登录IP" min-width="140" align="center" />
          <el-table-column label="登录成功标识" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.fieldLoginSuccess ? 'success' : 'danger'" size="small">
                {{ row.fieldLoginSuccess ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fieldMessage" label="消息" min-width="200" show-overflow-tooltip />
        </el-table>

        <el-pagination
          v-model:current-page="logPage.pageNum"
          v-model:page-size="logPage.pageSize"
          :total="logPage.total"
          :page-sizes="[15, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchLoginLog"
          @current-change="fetchLoginLog"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { userApi } from '@/api/org'
import { USER_STATUS_OPTIONS } from '@/utils/constants'

const route = useRoute()
const useRouterCls = useRouter()
const fieldId = computed(() => route.params.id || '')

const loading = ref(false)
const detail = ref({})

// 表格字段配置（11 个字段，每行 3 列）
const allFields = [
  { label: '名称', key: 'fieldName' },
  { label: '登录名', key: 'fieldLoginName' },
  { label: '手机号', key: 'fieldPhone' },
  { label: '邮箱', key: 'fieldEmail' },
  { label: '状态', key: 'fieldStatus', type: 'status' },
  { label: '创建时间', key: 'fieldCreateTime' },
  { label: '更新时间', key: 'fieldUpdateTime' },
  { label: '最后登录时间', key: 'fieldLastLoginTime' },
  { label: '最后登录IP', key: 'fieldLastLoginIp' },
  { label: '锁定标识', key: 'fieldLockFlag', type: 'lock' },
  { label: '解锁时间', key: 'fieldUnlockTime' }
]

// 按 3 列一行切分
const formRows = computed(() => {
  const rows = []
  for (let i = 0; i < allFields.length; i += 3) {
    rows.push(allFields.slice(i, i + 3))
  }
  return rows
})

function formatValue(item) {
  const val = detail.value[item.key]
  if (val == null || val === '') return '-'
  if (item.type === 'status') {
    return USER_STATUS_OPTIONS.find((i) => i.value === val)?.label || val
  }
  if (item.type === 'lock') {
    return val ? '已锁定' : '未锁定'
  }
  return val
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await userApi.load(fieldId.value)
    detail.value = res.data || {}
  } finally {
    loading.value = false
  }
}

// 登录记录明细表
const logLoading = ref(false)
const logData = ref([])
const logPage = reactive({ pageNum: 1, pageSize: 15, total: 0 })

async function fetchLoginLog() {
  logLoading.value = true
  try {
    const res = await userApi.listLoginLog(fieldId.value, {
      pageNum: logPage.pageNum,
      pageSize: logPage.pageSize
    })
    logData.value = res.list || []
    logPage.total = res.query?.total ?? 0
  } finally {
    logLoading.value = false
  }
}

function onBack() {
  useRouterCls.push('/admin/org/user')
}

onMounted(() => {
  loadDetail()
  fetchLoginLog()
})
</script>

<style lang="scss" scoped>
.user-detail {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: var(--font-weight-bold);
  }

  &__section-title {
    font-weight: var(--font-weight-bold);
  }

  &__log {
    :deep(.el-card__body) {
      padding: var(--spacing-md);
    }
  }

  .u-detail-table {
    .el-pagination {
      display: flex;
      justify-content: flex-end;
      margin-top: var(--spacing-md);
    }
  }
}
</style>
