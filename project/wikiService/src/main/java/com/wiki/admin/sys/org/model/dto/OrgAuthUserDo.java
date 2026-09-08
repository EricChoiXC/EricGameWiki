package com.wiki.admin.sys.org.model.dto;

import lombok.Data;

/**
 * 用户与权限-角色分配用户 DO，对应 {@code admin_org_auth_user} 表。
 * <p>
 * field_auth_id 指向 {@code admin_org_auth}（角色），
 * field_user_id 指向 {@code admin_org_user}（用户）。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgAuthUserDo {

    /** 角色id */
    private String fieldAuthId;

    /** 用户id */
    private String fieldUserId;
}
