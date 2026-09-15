<template>
  <div class="login-view">
    <el-card class="login-view__card">
      <div class="login-view__header">
        <el-icon class="login-view__logo" :size="32"><Platform /></el-icon>
        <h1 class="login-view__title">EricGameWiki 管理后台</h1>
        <p class="login-view__subtitle">请登录后继续操作</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @submit.prevent="onSubmit"
      >
        <el-form-item prop="loginName">
          <el-input
            v-model="form.loginName"
            placeholder="登录名"
            :prefix-icon="User"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="密码"
            :prefix-icon="Lock"
            autocomplete="current-password"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-view__submit"
            :loading="loading"
            native-type="submit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, Platform, User } from '@element-plus/icons-vue'
import { authApi } from '@/api/org'
import { useUserStore } from '@/stores/user'
import { PERMISSION_CODE } from '@/utils/constants'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  loginName: '',
  password: ''
})

const rules = {
  loginName: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 登录成功：写入登录态 → 拉取实际权限码 → 回跳目标页
// 错误提示由 request.js 响应拦截器统一处理（如 401 登录名或密码错误）
async function onSubmit() {
  if (loading.value) return
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const res = await authApi.login({ data: { loginName: form.loginName, password: form.password } })
    const data = res?.data || {}
    userStore.setToken(data.token || '')
    userStore.setUser({
      fieldId: data.userId,
      fieldName: data.userName,
      fieldLoginName: data.loginName
    })
    userStore.setPermissions(await fetchGrantedPermissions())

    // 密码已过期：强制先修改密码（docs/admin/用户和权限管理.md 业务逻辑第 5 条）
    if (data.passwordExpired === true) {
      router.push({ path: '/home', query: { forceChangePwd: '1' } })
      return
    }
    router.push(typeof route.query.redirect === 'string' && route.query.redirect
      ? route.query.redirect
      : '/home')
  } catch {
    // 登录失败保持页面，等待用户重试
  } finally {
    loading.value = false
  }
}

/**
 * 批量预判当前用户权限码（仅用于按钮显隐预判，不可替代后端最终鉴权）
 * 后端返回 JSON 字符串，如 '["admin-org::USER"]'
 */
async function fetchGrantedPermissions() {
  const res = await authApi.batchCheckPermissions(Object.values(PERMISSION_CODE))
  const raw = res?.data
  const granted = typeof raw === 'string' ? JSON.parse(raw) : raw
  return Array.isArray(granted) ? granted : []
}
</script>

<style lang="scss" scoped>
.login-view {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: var(--color-bg-page);

  &__card {
    width: 380px;
    padding: var(--spacing-lg) 0;
  }

  &__header {
    margin-bottom: var(--spacing-xl);
    text-align: center;
  }

  &__logo {
    color: var(--color-primary);
  }

  &__title {
    margin: var(--spacing-sm) 0 var(--spacing-xs);
    font-size: var(--font-size-h2);
    font-weight: var(--font-weight-bold);
    color: var(--color-text-primary);
  }

  &__subtitle {
    margin: 0;
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
  }

  &__submit {
    width: 100%;
  }
}
</style>
