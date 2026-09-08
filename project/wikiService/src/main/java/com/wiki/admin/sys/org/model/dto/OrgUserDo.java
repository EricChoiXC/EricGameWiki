package com.wiki.admin.sys.org.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户与权限-用户 DO，对应 {@code docs/admin/用户和权限管理.md} 中 {@code admin_org_user} 表。
 * <p>
 * {@code field_password} 仅入库存储，任何查询列集合都不允许返回密码字段。
 * {@code field_lock_flag} 使用 Integer 0/1（0 未锁定 / 1 已锁定）。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgUserDo {

    /** id */
    private String fieldId;

    /** 名称 */
    private String fieldName;

    /** 登录名 */
    private String fieldLoginName;

    /** 密码（BCrypt，不出参） */
    private String fieldPassword;

    /** 手机 */
    private String fieldPhone;

    /** 邮箱 */
    private String fieldEmail;

    /** 状态 */
    private String fieldStatus;

    /** 创建时间 */
    private LocalDateTime fieldCreateTime;

    /** 更新时间 */
    private LocalDateTime fieldUpdateTime;

    /** 最后登录时间 */
    private LocalDateTime fieldLastLoginTime;

    /** 最后登录IP */
    private String fieldLastLoginIp;

    /** 锁定标识（0 未锁定 / 1 已锁定） */
    private Integer fieldLockFlag;

    /** 解锁时间 */
    private LocalDateTime fieldUnlockTime;
}
