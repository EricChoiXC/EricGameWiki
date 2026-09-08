package com.wiki.admin.sys.org.model.dto;

import lombok.Data;

/**
 * 用户与权限-权限 DO，对应 {@code docs/admin/用户和权限管理.md} 中 {@code admin_org_role} 表。
 * <p>
 * 注意：本表在文档中命名为 role，但语义为"权限"（field_code 为权限名），
 * 由 {@code roles.yml} 增量写入。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgRoleDo {

    /** id */
    private String fieldId;

    /** 中文名 */
    private String fieldName;

    /** 权限名（如 admin-org::USER） */
    private String fieldCode;

    /** 状态 */
    private String fieldStatus;
}
