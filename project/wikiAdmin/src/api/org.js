import request from './request'
import { createStandardApi } from './system'

/**
 * 用户与权限模块 API
 * 模块映射：docs/admin/用户和权限管理.md
 *
 * 表与语义对应关系（docs/admin/用户和权限管理.md 表结构）：
 *  - admin_org_user              → 用户
 *  - admin_org_auth             → 角色（field_name 名称 / field_code 编号）
 *  - admin_org_role             → 权限（field_name 中文名 / field_code 权限名）
 *  - admin_org_user_login_log   → 用户登录记录
 *  - admin_org_user_password_log→ 用户密码变更记录
 *
 * 路径前缀对齐 AGENTS.md §4.2/§5：baseURL=/api/v1/admin，模块路径 /org/...
 * 通用接口规范遵循 docs/common/接口公共规范.md 第 3.4 章
 */

// ---------------------------------------------------------------------------
// 用户管理（admin_org_user）
// ---------------------------------------------------------------------------
export const userApi = {
  ...createStandardApi('/org/user'),

  /**
   * 启用/停用用户（PATCH /updateStatus 仅更新状态字段）
   * @param {string} fieldId
   * @param {string} fieldStatus ENABLED | DISABLED
   */
  updateStatus(fieldId, fieldStatus) {
    return request.patch('/org/user/updateStatus', {
      data: { fieldStatus }
    }, {
      params: { fieldId }
    })
  },

  /**
   * 解除用户锁定
   * @param {string} fieldId
   */
  unlock(fieldId) {
    return request.patch('/org/user/unlock', null, {
      params: { fieldId }
    })
  },

  /**
   * 修改当前登录用户密码
   * @param {Object} payload { oldPassword, newPassword }
   */
  changePassword(payload) {
    return request.post('/org/user/changePassword', payload)
  },

  /**
   * 查询指定用户的登录记录（用户详情页明细表）
   * @param {string} fieldUserId
   * @param {Object} pageQuery { pageNum, pageSize, sortField, sortOrder }
   */
  listLoginLog(fieldUserId, pageQuery = {}) {
    const { pageNum = 1, pageSize = 15, sortField = 'fieldLoginTime', sortOrder = 'desc' } = pageQuery
    return request.post('/org/loginLog/list', {
      query: {
        pageNum,
        pageSize,
        needPage: true,
        sortField,
        sortOrder,
        data: { and: [{ eq: { fieldUserId } }] }
      }
    })
  }
}

// ---------------------------------------------------------------------------
// 角色管理（admin_org_auth 表 — 角色）
// ---------------------------------------------------------------------------
export const roleApi = {
  ...createStandardApi('/org/auth'),

  /**
   * 角色不支持删除（后端已移除 DELETE /org/auth/delete 接口）
   * 覆盖 createStandardApi 生成的 remove，防止误调用
   */
  remove() {
    return Promise.reject(new Error('角色不支持删除'))
  },

  /**
   * 启用/停用角色
   * @param {string} fieldId
   * @param {string} fieldStatus ENABLED | DISABLED
   */
  updateStatus(fieldId, fieldStatus) {
    return request.patch('/org/auth/updateStatus', {
      data: { fieldStatus }
    }, {
      params: { fieldId }
    })
  }
}

// ---------------------------------------------------------------------------
// 认证（登录 / 登出 / 权限预判）
// ---------------------------------------------------------------------------
export const authApi = {
  /**
   * 登录
   * @param {Object} payload { data: { loginName, password } }
   * @returns 响应含 token、userId、userName、loginName、passwordExpired（过期需强制改密）
   */
  login(payload) {
    return request.post('/org/auth/login', payload)
  },

  /**
   * 登出
   */
  logout() {
    return request.post('/org/auth/logout')
  },

  /**
   * 批量预判当前用户是否具备指定权限码（仅用于按钮显隐预判，不可替代后端最终鉴权）
   * @param {string[]} permissionCodes
   */
  batchCheckPermissions(permissionCodes) {
    return request.post('/org/auth/batchCheckPermissions', permissionCodes || [])
  }
}

// ---------------------------------------------------------------------------
// 权限管理（admin_org_role 表 — 权限）
// ---------------------------------------------------------------------------
export const permissionApi = createStandardApi('/org/role')

// ---------------------------------------------------------------------------
// 登录记录（admin_org_user_login_log）
// ---------------------------------------------------------------------------
export const loginLogApi = createStandardApi('/org/loginLog')

// ---------------------------------------------------------------------------
// 系统配置（admin_common_setting，docs/admin/公共服务.md）
// 后端 Controller 前缀 /api/v1/admin/sys/setting（CommonSettingController）
// ---------------------------------------------------------------------------
export const configApi = {
  /**
   * 配置项列表加载（含 fieldId/fieldCode/fieldValue，供页面回显与保存时定位 fieldId）
   */
  init() {
    return request.get('/sys/setting/init')
  },

  /**
   * 更新单个配置项值
   * @param {string} fieldId 配置项 id
   * @param {string} fieldValue 配置值
   */
  update(fieldId, fieldValue) {
    return request.patch('/sys/setting/update', { data: { fieldValue } }, { params: { fieldId } })
  }
}

export default {
  authApi,
  userApi,
  roleApi,
  permissionApi,
  loginLogApi,
  configApi
}
