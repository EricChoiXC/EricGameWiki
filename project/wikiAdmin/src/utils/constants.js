/**
 * 用户与权限模块公共常量
 * 表结构遵循 docs/admin/用户和权限管理.md
 */

// 用户状态
export const USER_STATUS = {
  ENABLED: 'ENABLED',
  DISABLED: 'DISABLED'
}

export const USER_STATUS_OPTIONS = [
  { value: USER_STATUS.ENABLED, label: '启用', type: 'success' },
  { value: USER_STATUS.DISABLED, label: '停用', type: 'info' }
]

// 锁定标识
export const LOCK_FLAG = {
  UNLOCKED: false,
  LOCKED: true
}

export const LOCK_FLAG_OPTIONS = [
  { value: false, label: '未锁定', type: 'success' },
  { value: true, label: '已锁定', type: 'danger' }
]

// 登录成功标识
export const LOGIN_SUCCESS_OPTIONS = [
  { value: true, label: '成功', type: 'success' },
  { value: false, label: '失败', type: 'danger' }
]

// 权限码（与后端 RBAC 权限名一致）
export const PERMISSION_CODE = {
  USER: 'admin-org::USER',
  ROLE: 'admin-org::ROLE'
}

// 系统配置项 key（与 docs/admin/用户和权限管理.md 系统配置表一致）
export const CONFIG_KEY = {
  CHANGE_PASSWORD_EXPIRE_DAYS: 'admin-org::change-password-expire-days',
  PASSWORD_MIN_LENGTH: 'admin-org::password-min-length',
  PASSWORD_NOT_EQUAL_TIME: 'admin-org::password-not-equal-time',
  LOCK_LOGIN_FAIL_TIMES: 'admin-org::lock-login-fail-times',
  DEFAULT_PASSWORD: 'admin-org::default_password'
}

// 系统配置项元信息（驱动系统配置页面渲染）
export const CONFIG_ITEMS = [
  {
    key: CONFIG_KEY.CHANGE_PASSWORD_EXPIRE_DAYS,
    label: '密码有效期',
    description: '密码有效期，为 0 时表示不过期，过期时用户登录后强制进行修改密码，单位：天',
    type: 'number',
    defaultValue: 0,
    min: 0
  },
  {
    key: CONFIG_KEY.PASSWORD_MIN_LENGTH,
    label: '密码最小长度',
    description: '密码最小长度',
    type: 'number',
    defaultValue: 6,
    min: 1
  },
  {
    key: CONFIG_KEY.PASSWORD_NOT_EQUAL_TIME,
    label: '密码不能相同',
    description: '密码不能相同，开启后用户不可设定与历史密码相同的密码',
    type: 'switch',
    defaultValue: false
  },
  {
    key: CONFIG_KEY.LOCK_LOGIN_FAIL_TIMES,
    label: '登录失败锁定次数',
    description: '登录失败锁定次数，为 0 时表示不锁定',
    type: 'number',
    defaultValue: 5,
    min: 0
  },
  {
    key: CONFIG_KEY.DEFAULT_PASSWORD,
    label: '默认密码',
    description: '默认密码',
    type: 'text',
    defaultValue: '123456'
  }
]
