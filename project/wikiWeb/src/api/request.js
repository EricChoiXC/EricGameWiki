import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * Axios 请求封装
 * 通用约束遵循 docs/common/接口公共规范.md 与 AGENTS.md 第 5 章：
 *  - baseURL: /api/v1/wiki（wiki 前台系统）
 *  - 前台无登录校验（docs/wiki/wiki访问.md），不注入鉴权头
 *  - 响应格式: { success, httpCode, code, message, data, list, query, map }
 *  - 错误码: 400/401/403/404/409/500（规范第 3.7 章）
 */

const request = axios.create({
  baseURL: '/api/v1/wiki',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

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

export default request
