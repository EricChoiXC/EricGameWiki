package com.wiki.admin.sys.org.model.response;

import lombok.Data;

/**
 * 登录响应。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgLoginResponse {

    /** JWT Token */
    private String token;

    /** 用户ID */
    private String userId;

    /** 用户名 */
    private String userName;

    /** 登录名 */
    private String loginName;

    /** 密码是否已过期（true 表示登录成功但需强制修改密码） */
    private Boolean passwordExpired;
}
