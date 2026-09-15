import { defineStore } from 'pinia'
import { getToken, setToken } from '@/api/request'

/**
 * 当前登录用户状态
 * 职责：持有 token、用户信息、权限列表，并提供按钮级权限预判断
 *
 * 约束（docs/admin/用户和权限管理.md 业务逻辑）：
 *  - 页面按钮显隐基于权限码预判断（admin-org::USER / admin-org::ROLE）
 *  - 预判断不可替代后端最终鉴权（docs/common/接口公共规范.md 第 3.2 章）
 *
 * 说明：用户信息与权限列表在登录成功时由 LoginView 写入并持久化到
 *       localStorage（与 token 同生命周期），刷新后从此处恢复；
 *       后端暂无 current 用户查询接口，故不保存过期快照以外的来源。
 */
const USER_CACHE_KEY = 'wikiAdminUser'

function loadCachedUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_CACHE_KEY) || 'null')
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => {
    const cached = loadCachedUser()
    return {
      token: getToken(),
      userInfo: cached?.userInfo || null,
      permissions: Array.isArray(cached?.permissions) ? cached.permissions : []
    }
  },

  getters: {
    isLogin: (state) => !!state.token,
    nickname: (state) => state.userInfo?.fieldName || ''
  },

  actions: {
    /**
     * 是否拥有指定权限码
     * @param {string} code 权限码，如 'admin-org::USER'
     * @returns {boolean}
     */
    hasPermission(code) {
      if (!code) return true
      return this.permissions.includes(code)
    },

    /**
     * 是否拥有任一权限码
     * @param {string[]} codes
     * @returns {boolean}
     */
    hasAnyPermission(codes = []) {
      if (!codes || codes.length === 0) return true
      return codes.some((c) => this.permissions.includes(c))
    },

    /**
     * 设置 token 并持久化
     */
    setToken(token) {
      this.token = token
      setToken(token)
    },

    /**
     * 设置用户信息并持久化
     */
    setUser(userInfo) {
      this.userInfo = userInfo || null
      this.persist()
    },

    /**
     * 设置权限列表并持久化
     */
    setPermissions(permissions) {
      this.permissions = Array.isArray(permissions) ? permissions : []
      this.persist()
    },

    /**
     * 持久化用户信息与权限列表（刷新后由 state 初始化恢复）
     */
    persist() {
      localStorage.setItem(
        USER_CACHE_KEY,
        JSON.stringify({ userInfo: this.userInfo, permissions: this.permissions })
      )
    },

    /**
     * 登出：清空本地状态
     */
    logout() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      setToken('')
      localStorage.removeItem(USER_CACHE_KEY)
    }
  }
})
