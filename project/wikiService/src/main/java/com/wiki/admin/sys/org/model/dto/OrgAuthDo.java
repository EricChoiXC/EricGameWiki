package com.wiki.admin.sys.org.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户与权限-角色 DO，对应 {@code docs/admin/用户和权限管理.md} 中 {@code admin_org_auth} 表。
 * <p>
 * 注意：本表在文档中命名为 auth，但语义为"角色"，通过 auth_role / auth_user 关联权限与用户。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgAuthDo {

    /** id */
    private String fieldId;

    /** 名称 */
    private String fieldName;

    /** 编号 */
    private String fieldCode;

    /** 状态 */
    private String fieldStatus;

    /** 创建时间 */
    private LocalDateTime fieldCreateTime;
}
