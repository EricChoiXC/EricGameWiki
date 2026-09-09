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
 * 通用接口规范遵循 docs/common/接口公共规范.md 第 3.4 章
 */

// ---------------------------------------------------------------------------
// 用户管理（admin_org_user）
// ---------------------------------------------------------------------------
export const userApi = {
  ...createStandardApi('/sys/org/user'),

  /**
   * 启用/停用用户（PATCH /update 仅更新状态字段）
   * @param {string} fieldId
   * @param {string} fieldStatus ENABLED | DISABLED
   */
  updateStatus(fieldId, fieldStatus) {
    return request.patch('/sys/org/user/update', {
      data: { fieldId, fieldStatus }
    })
  },

  /**
   * 解除用户锁定
   * @param {string} fieldId
   */
  unlock(fieldId) {
    return request.post('/sys/org/user/unlock', null, {
      params: { fieldId }
    })
  },

  /**
   * 修改当前登录用户密码
   * @param {Object} payload { oldPassword, newPassword }
   */
  changePassword(payload) {
    return request.post('/sys/org/user/changePassword', { data: payload })
  },

  /**
   * 查询指定用户的登录记录（用户详情页明细表）
   * @param {string} fieldUserId
   * @param {Object} pageQuery { pageNum, pageSize, sortField, sortOrder }
   */
  listLoginLog(fieldUserId, pageQuery = {}) {
    const { pageNum = 1, pageSize = 15, sortField = 'fieldLoginTime', sortOrder = 'desc' } = pageQuery
    return request.post('/sys/org/userLoginLog/list', {
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
  ...createStandardApi('/sys/org/auth'),

  /**
   * 启用/停用角色
   * @param {string} fieldId
   * @param {string} fieldStatus ENABLED | DISABLED
   */
  updateStatus(fieldId, fieldStatus) {
    return request.patch('/sys/org/auth/update', {
      data: { fieldId, fieldStatus }
    })
  },

  /**
   * 保存角色（含权限与用户分配）
   * @param {Object} payload { data: roleVo, map: { authRoleList, authUserList } }
   */
  saveWithAssign(payload) {
    return request.post('/sys/org/auth/save', payload)
  },

  /**
   * 更新角色（含权限与用户分配）
   * @param {Object} payload { data: roleVo, map: { authRoleList, authUserList } }
   */
  updateWithAssign(payload) {
    return request.patch('/sys/org/auth/update', payload)
  }
}

// ---------------------------------------------------------------------------
// 权限管理（admin_org_role 表 — 权限）
// ---------------------------------------------------------------------------
export const permissionApi = createStandardApi('/sys/org/role')

// ---------------------------------------------------------------------------
// 登录记录（admin_org_user_login_log）
// ---------------------------------------------------------------------------
export const loginLogApi = createStandardApi('/sys/org/userLoginLog')

// ---------------------------------------------------------------------------
// 系统配置（admin-org 模块配置项）
// ---------------------------------------------------------------------------
export const configApi = {
  ...createStandardApi('/sys/org/config'),

  /**
   * 批量保存配置项
   * @param {Array<{ fieldKey: string, fieldValue: string }>} list
   */
  saveBatch(list) {
    return request.post('/sys/org/config/save', { list })
  }
}

export default {
  userApi,
  roleApi,
  permissionApi,
  loginLogApi,
  configApi
}
