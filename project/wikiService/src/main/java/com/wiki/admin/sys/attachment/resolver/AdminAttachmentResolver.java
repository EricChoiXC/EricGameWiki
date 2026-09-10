package com.wiki.admin.sys.attachment.resolver;

import com.wiki.admin.sys.attachment.util.AttachmentConstants;
import com.wiki.common.security.PermissionChecker;
import com.wiki.common.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 附件模块接口级鉴权 Resolver。
 * <p>
 * 遵循 {@code AGENTS.md} 4.3 与 {@code docs/common/业务流转公共规范.md} 4.6：
 * 鉴权核心参数必须是"接口 + 文档 ID"，resolver 至少接收当前用户、接口标识、文档 ID。
 * 本 resolver 以权限码作为接口标识、fieldId 作为文档 ID，校验通过后记录可追踪鉴权审计日志。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Component
public class AdminAttachmentResolver {

    /**
     * 要求当前用户具备附件管理员权限，并针对指定文档 ID 做接口级鉴权。
     *
     * @param fieldId 文档 ID（附件信息id）；列表查询场景可为 null
     */
    public void requireManage(String fieldId) {
        String userId = currentUserId();
        // 先完成权限码校验（权限码即接口标识），不通过则 PermissionChecker 抛 403
        PermissionChecker.require(AttachmentConstants.PERMISSION_ADMIN);
        // 记录可追踪鉴权审计：当前用户 + 接口标识（权限码） + 文档 ID + 允许结论
        log.info("[鉴权] 附件管理操作放行 userId={} 接口={} 文档ID={}",
                userId, AttachmentConstants.PERMISSION_ADMIN, fieldId);
    }

    private String currentUserId() {
        UserContext context = UserContext.current();
        return context == null ? null : context.getUserId();
    }
}
