package com.wiki.admin.sys.org.resolver;

import com.wiki.admin.sys.org.util.OrgConstants;
import com.wiki.common.security.PermissionChecker;
import com.wiki.common.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户与权限-登录记录 接口级鉴权 Resolver。
 * <p>
 * 遵循 {@code AGENTS.md} 4.3 与 {@code docs/common/业务流转公共规范.md} 4.6：
 * 鉴权核心参数必须是"接口 + 文档 ID"，resolver 至少接收当前用户、接口标识、文档 ID。
 * 登录记录列表默认要求登录态（由 Security 配置保证）；
 * 用户自身登录记录无需额外权限码，他人记录需用户管理员权限。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Component
public class OrgLoginLogResolver {

    /**
     * 查看登录记录：已登录用户均可查看（基础认证由 Security 配置保证）。
     *
     * @param fieldId 文档 ID（登录记录ID 或用户ID）；列表场景可为 null
     */
    public void requireView(String fieldId) {
        String userId = currentUserId();
        log.info("[鉴权] 登录记录查看放行 userId={} 文档ID={}", userId, fieldId);
    }

    /**
     * 管理登录记录（如删除他人记录等）：需用户管理员权限。
     *
     * @param fieldId 文档 ID；列表场景可为 null
     */
    public void requireManage(String fieldId) {
        String userId = currentUserId();
        PermissionChecker.require(OrgConstants.PERMISSION_USER);
        log.info("[鉴权] 登录记录管理操作放行 userId={} 接口={} 文档ID={}",
                userId, OrgConstants.PERMISSION_USER, fieldId);
    }

    private String currentUserId() {
        UserContext context = UserContext.current();
        return context == null ? null : context.getUserId();
    }
}
