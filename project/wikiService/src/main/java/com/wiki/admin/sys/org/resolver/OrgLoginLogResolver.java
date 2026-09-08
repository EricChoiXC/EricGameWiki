package com.wiki.admin.sys.org.resolver;

import com.wiki.common.security.PermissionChecker;
import org.springframework.stereotype.Component;

/**
 * 用户与权限-登录记录 接口级鉴权 Resolver。
 * <p>
 * 登录记录页面默认要求登录态，此处仅做基础认证（由 Security 配置保证）；
 * 用户自身登录记录无需额外权限码，他人记录需用户管理员权限。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Component
public class OrgLoginLogResolver {

    public void requireView() {
        // 任何已登录用户均可查看登录记录列表
    }

    public void requireManage() {
        PermissionChecker.require("admin-org::USER");
    }
}
