package com.wiki.admin.sys.org.model.request;

import lombok.Data;

/**
 * 登录请求。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgLoginRequest {

    /** 登录名 */
    private String loginName;

    /** 明文密码 */
    private String password;

    /** 登录IP（由后端从请求头解析后填充） */
    private String loginIp;
}
