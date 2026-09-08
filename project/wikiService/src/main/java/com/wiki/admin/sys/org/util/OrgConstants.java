package com.wiki.admin.sys.org.util;

/**
 * 用户与权限模块常量，对应 {@code docs/admin/用户和权限管理.md}。
 * <p>
 * 集中维护权限码、配置项 key、默认管理员/角色 ID、状态码值等。
 *
 * @author Eric
 * @date 2026/9/8
 */
public final class OrgConstants {

    private OrgConstants() {
    }

    /** 32 位 0 的字符串，作为默认管理员用户与系统管理员角色的 field_id */
    public static final String ZERO_ID = "0".repeat(32);

    /** 默认管理员用户ID */
    public static final String ADMIN_USER_ID = ZERO_ID;
    /** 默认管理员登录名 */
    public static final String ADMIN_LOGIN_NAME = "admin";
    /** 默认管理员名称 */
    public static final String ADMIN_USER_NAME = "管理员";

    /** 系统管理员角色ID */
    public static final String SYS_ADMIN_AUTH_ID = ZERO_ID;
    /** 系统管理员角色编号 */
    public static final String SYS_ADMIN_AUTH_CODE = "sys_admin";
    /** 系统管理员角色名称 */
    public static final String SYS_ADMIN_AUTH_NAME = "系统管理员";

    /** 权限码：用户管理员 */
    public static final String PERMISSION_USER = "admin-org::USER";
    /** 权限码：角色管理员 */
    public static final String PERMISSION_ROLE = "admin-org::ROLE";

    /** 状态：启用 */
    public static final String STATUS_ENABLED = "ENABLED";
    /** 状态：停用 */
    public static final String STATUS_DISABLED = "DISABLED";

    /** 配置项：密码有效期（天，0 不过期） */
    public static final String SETTING_CHANGE_PASSWORD_EXPIRE_DAYS = "admin-org::change-password-expire-days";
    /** 配置项：密码最小长度 */
    public static final String SETTING_PASSWORD_MIN_LENGTH = "admin-org::password-min-length";
    /** 配置项：密码不能相同 */
    public static final String SETTING_PASSWORD_NOT_EQUAL_TIME = "admin-org::password-not-equal-time";
    /** 配置项：登录失败锁定次数 */
    public static final String SETTING_LOCK_LOGIN_FAIL_TIMES = "admin-org::lock-login-fail-times";
    /** 配置项：默认密码 */
    public static final String SETTING_DEFAULT_PASSWORD = "admin-org::default_password";

    /** 默认密码（兜底，优先取配置项） */
    public static final String DEFAULT_PASSWORD = "123456";
}
