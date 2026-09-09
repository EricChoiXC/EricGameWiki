<template>
  <div class="role-detail">
    <el-card shadow="never">
      <template #header>
        <div class="role-detail__header">
          <span>角色详情</span>
          <el-button :icon="ArrowLeft" @click="onBack">返回</el-button>
        </div>
      </template>

      <div class="u-form-table" v-loading="loading">
        <el-row>
          <el-col :span="1" class="u-form-table__label">名称</el-col>
          <el-col :span="3" class="u-form-table__value">{{ detail.fieldName || '-' }}</el-col>
          <el-col :span="1" class="u-form-table__label">编号</el-col>
          <el-col :span="3" class="u-form-table__value">{{ detail.fieldCode || '-' }}</el-col>
          <el-col :span="1" class="u-form-table__label">状态</el-col>
          <el-col :span="3" class="u-form-table__value">{{ getStatusLabel(detail.fieldStatus) }}</el-col>
        </el-row>

        <!-- 权限（占整行） -->
        <el-row>
          <el-col :span="1" class="u-form-table__label">权限</el-col>
          <el-col :span="11" class="u-form-table__value">
            <el-tag
              v-for="item in permissionList"
              :key="item.fieldRoleId"
              size="small"
              class="role-detail__tag"
            >
              {{ item.fieldRoleName || item.fieldRoleCode || '-' }}
            </el-tag>
            <span v-if="permissionList.length === 0">-</span>
          </el-col>
        </el-row>

        <!-- 用户（占整行） -->
        <el-row>
          <el-col :span="1" class="u-form-table__label">用户</el-col>
          <el-col :span="11" class="u-form-table__value">
            <el-tag
              v-for="item in userList"
              :key="item.fieldUserId"
              size="small"
              type="info"
              class="role-detail__tag"
            >
              {{ item.fieldUserName || item.fieldUserLoginName || '-' }}
            </el-tag>
            <span v-if="userList.length === 0">-</span>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="1" class="u-form-table__label">创建时间</el-col>
          <el-col :span="3" class="u-form-table__value">{{ detail.fieldCreateTime || '-' }}</el-col>
          <el-col :span="1" class="u-form-table__label">更新时间</el-col>
          <el-col :span="3" class="u-form-table__value">{{ detail.fieldUpdateTime || '-' }}</el-col>
          <el-col :span="1" class="u-form-table__label" />
          <el-col :span="3" class="u-form-table__value" />
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { roleApi } from '@/api/org'
import { USER_STATUS_OPTIONS } from '@/utils/constants'

const route = useRoute()
const router = useRouter()
const fieldId = computed(() => route.params.id || '')

const loading = ref(false)
const detail = ref({})
const permissionList = ref([])
const userList = ref([])

function getStatusLabel(status) {
  if (!status) return '-'
  return USER_STATUS_OPTIONS.find((i) => i.value === status)?.label || status
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await roleApi.load(fieldId.value)
    detail.value = res.data || {}
    permissionList.value = res.map?.authRoleList || []
    userList.value = res.map?.authUserList || []
  } finally {
    loading.value = false
  }
}

function onBack() {
  router.push('/admin/org/role')
}

onMounted(loadDetail)
</script>

<style lang="scss" scoped>
.role-detail {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: var(--font-weight-bold);
  }

  &__tag {
    margin: 0 var(--spacing-xs) var(--spacing-xs) 0;
  }
}
</style>
