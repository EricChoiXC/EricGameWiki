package com.wiki.admin.sys.org.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户与权限-用户登录记录 DO，对应 {@code admin_org_user_login_log} 表。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgUserLoginLogDo {

    /** id */
    private String fieldId;

    /** 用户id */
    private String fieldUserId;

    /** 登录时间 */
    private LocalDateTime fieldLoginTime;

    /** 登录IP */
    private String fieldLoginIp;

    /** 登录成功标识（0 失败 / 1 成功） */
    private Integer fieldLoginSuccess;

    /** 消息 */
    private String fieldMessage;
}
