package com.wiki.admin.sys.org.resolver;

import com.wiki.admin.sys.org.util.OrgConstants;
import com.wiki.common.security.PermissionChecker;
import org.springframework.stereotype.Component;

/**
 * 用户与权限-用户 接口级鉴权 Resolver。
 * <p>
 * 遵循 {@code AGENTS.md} 4.3 与 {@code docs/common/业务流转公共规范.md} 4.6。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Component
public class OrgUserResolver {

    public void requireManage() {
        PermissionChecker.require(OrgConstants.PERMISSION_USER);
    }
}
