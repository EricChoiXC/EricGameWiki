package com.wiki.admin.sys.org.model.dto;

import lombok.Data;

/**
 * 用户与权限-角色分配权限 DO，对应 {@code admin_org_auth_role} 表。
 * <p>
 * field_auth_id 指向 {@code admin_org_auth}（角色），
 * field_role_id 指向 {@code admin_org_role}（权限）。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgAuthRoleDo {

    /** 角色id */
    private String fieldAuthId;

    /** 权限id */
    private String fieldRoleId;
}
