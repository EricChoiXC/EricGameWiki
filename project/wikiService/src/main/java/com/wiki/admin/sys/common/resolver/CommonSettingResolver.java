package com.wiki.admin.sys.common.resolver;

import com.wiki.admin.sys.org.util.OrgConstants;
import com.wiki.common.security.PermissionChecker;
import org.springframework.stereotype.Component;

/**
 * 公共服务-配置表 接口级鉴权 Resolver。
 * <p>
 * 遵循 {@code AGENTS.md} 4.3 与 {@code docs/common/业务流转公共规范.md} 4.6：
 * 系统配置编辑复用 org 模块用户管理员权限码。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Component
public class CommonSettingResolver {

    /**
     * 要求具备配置编辑权限（用户管理员）。
     */
    public void requireManage() {
        PermissionChecker.require(OrgConstants.PERMISSION_USER);
    }
}
