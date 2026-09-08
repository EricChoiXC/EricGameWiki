package com.wiki.admin.sys.org.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户与权限-用户密码变更记录 DO，对应 {@code admin_org_user_password_log} 表。
 * <p>
 * field_new_password 存放变更后的 BCrypt 哈希，用于"密码不能相同"校验。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgUserPasswordLogDo {

    /** id */
    private String fieldId;

    /** 用户id */
    private String fieldUserId;

    /** 变更时间 */
    private LocalDateTime fieldChangeTime;

    /** 新密码（BCrypt） */
    private String fieldNewPassword;
}
