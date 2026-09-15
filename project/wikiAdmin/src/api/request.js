import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * Axios 请求封装
 * 通用约束遵循 docs/common/接口公共规范.md 与 AGENTS.md 第 5 章：
 *  - baseURL: /api/v1/admin
 *  - 鉴权: Authorization: Bearer <token>
 *  - 响应格式: { success, httpCode, code, message, data, list, query, map }
 *  - 错误码: 400/401/403/404/409/500（规范第 3.7 章）
 */

const TOKEN_KEY = 'wikiAdminToken'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

const request = axios.create({
  baseURL: '/api/v1/admin',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// -----------------------------------------------------------------------------
// 请求拦截：注入 JWT
// -----------------------------------------------------------------------------
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// -----------------------------------------------------------------------------
// 响应拦截：解包标准响应 + 统一错误提示
// -----------------------------------------------------------------------------
request.interceptors.response.use(
  (response) => {
    const body = response.data || {}
    // 业务失败（HTTP 200 但 success=false）
    if (body.success === false) {
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message || body.code || 'BUSINESS_ERROR'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data || {}
    const message = body.message || error.message

    switch (status) {
      case 401:
        ElMessage.error(message || '未登录或登录已过期')
        setToken('')
        redirectToLogin()
        break
      case 403:
        ElMessage.error(message || '无操作权限')
        break
      case 404:
        ElMessage.error(message || '资源不存在')
        break
      case 409:
        ElMessage.error(message || '数据冲突，请修改后重试')
        break
      case 500:
        ElMessage.error(message || '服务器内部错误')
        break
      default:
        ElMessage.error(message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

// -----------------------------------------------------------------------------
// 401 统一处理：清除登录态并跳转登录页
// 动态导入 router，避免 request → router → stores/user → request 的循环依赖；
// 已在登录页时仅提示（如密码错误），不再跳转以免循环
// -----------------------------------------------------------------------------
function redirectToLogin() {
  import('@/router')
    .then(({ default: router }) => {
      const current = router.currentRoute.value
      if (current.path === '/login') {
        return
      }
      router.push({ path: '/login', query: { redirect: current.fullPath } })
    })
    .catch(() => {})
}

export default request
