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
}
