import { defineStore } from 'pinia'
import { getToken, setToken } from '@/api/request'
import { userApi } from '@/api/org'
import { PERMISSION_CODE } from '@/utils/constants'

/**
 * 当前登录用户状态
 * 职责：持有 token、用户信息、权限列表，并提供按钮级权限预判断
 *
 * 约束（docs/admin/用户和权限管理.md 业务逻辑）：
 *  - 页面按钮显隐基于权限码预判断（admin-org::USER / admin-org::ROLE）
 *  - 预判断不可替代后端最终鉴权（docs/common/接口公共规范.md 第 3.2 章）
 *
 * 说明：登录流程尚未接入前，permissions 默认包含全部权限码，
 *       以保证管理后台在开发阶段按钮可见；登录接口就绪后由 initUser() 覆盖。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: null,
    permissions: [
      PERMISSION_CODE.USER,
      PERMISSION_CODE.ROLE,
      PERMISSION_CODE.ATTACHMENT_ADMIN,
      PERMISSION_CODE.WIKI_ADMIN
    ]
  }),

  getters: {
    isLogin: (state) => !!state.token,
    nickname: (state) => state.userInfo?.fieldName || '管理员'
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
     * 设置用户信息与权限列表
     */
    setUser(userInfo, permissions = []) {
      this.userInfo = userInfo
      this.permissions = permissions && permissions.length > 0 ? permissions : this.permissions
    },

    /**
     * 拉取当前用户信息（登录态下调用）
     */
    async initUser() {
      if (!this.token) return
      try {
        const res = await userApi.load('current')
        if (res?.data) {
          this.setUser(res.data, res.map?.permissions)
        }
      } catch {
        // 拉取失败不阻塞页面渲染
      }
    },

    /**
     * 登出：清空本地状态
     */
    logout() {
      this.token = ''
      this.userInfo = null
      this.permissions = [
        PERMISSION_CODE.USER,
        PERMISSION_CODE.ROLE,
        PERMISSION_CODE.ATTACHMENT_ADMIN,
        PERMISSION_CODE.WIKI_ADMIN
      ]
      setToken('')
    }
  }
})
