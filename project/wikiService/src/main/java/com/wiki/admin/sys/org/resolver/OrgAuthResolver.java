package com.wiki.admin.sys.org.resolver;

import com.wiki.admin.sys.org.util.OrgConstants;
import com.wiki.common.security.PermissionChecker;
import org.springframework.stereotype.Component;

/**
 * 用户与权限-角色 接口级鉴权 Resolver。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Component
public class OrgAuthResolver {

    public void requireManage() {
        PermissionChecker.require(OrgConstants.PERMISSION_ROLE);
    }
}
