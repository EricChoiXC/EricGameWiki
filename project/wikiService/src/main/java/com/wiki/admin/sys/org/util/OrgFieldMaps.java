package com.wiki.admin.sys.org.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Org 模块字段白名单：Java 属性名 → 数据库列名。
 * <p>
 * 仅允许这些字段出现在 {@code query.data} 查询条件树中以及 {@code sortField} 排序中，
 * 由 {@code QueryConditionBuilder} / {@code SqlSortBuilder} 在渲染 SQL 前校验。
 * <p>
 * 字段映射与各 DO 属性、数据库列保持一致，遵循 {@code docs/common/数据库公共规范.md}。
 *
 * @author Eric
 * @date 2026/9/8
 */
public final class OrgFieldMaps {

    private OrgFieldMaps() {
    }

    /** admin_org_user 字段白名单 */
    public static final Map<String, String> USER = buildUserMap();

    /** admin_org_role（权限）字段白名单 */
    public static final Map<String, String> ROLE = buildRoleMap();

    /** admin_org_auth（角色）字段白名单 */
    public static final Map<String, String> AUTH = buildAuthMap();

    /** admin_org_user_login_log 字段白名单 */
    public static final Map<String, String> LOGIN_LOG = buildLoginLogMap();

    private static Map<String, String> buildUserMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("fieldId", "field_id");
        map.put("fieldName", "field_name");
        map.put("fieldLoginName", "field_login_name");
        map.put("fieldPhone", "field_phone");
        map.put("fieldEmail", "field_email");
        map.put("fieldStatus", "field_status");
        map.put("fieldCreateTime", "field_create_time");
        map.put("fieldUpdateTime", "field_update_time");
        map.put("fieldLastLoginTime", "field_last_login_time");
        map.put("fieldLastLoginIp", "field_last_login_ip");
        map.put("fieldLockFlag", "field_lock_flag");
        map.put("fieldUnlockTime", "field_unlock_time");
        return map;
    }

    private static Map<String, String> buildRoleMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("fieldId", "field_id");
        map.put("fieldName", "field_name");
        map.put("fieldCode", "field_code");
        map.put("fieldStatus", "field_status");
        return map;
    }

    private static Map<String, String> buildAuthMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("fieldId", "field_id");
        map.put("fieldName", "field_name");
        map.put("fieldCode", "field_code");
        map.put("fieldStatus", "field_status");
        map.put("fieldCreateTime", "field_create_time");
        return map;
    }

    private static Map<String, String> buildLoginLogMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("fieldId", "field_id");
        map.put("fieldUserId", "field_user_id");
        map.put("fieldLoginTime", "field_login_time");
        map.put("fieldLoginIp", "field_login_ip");
        map.put("fieldLoginSuccess", "field_login_success");
        map.put("fieldMessage", "field_message");
        return map;
    }
}
