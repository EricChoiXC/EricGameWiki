package com.wiki.admin.sys.attachment.resolver;

import com.wiki.admin.sys.attachment.util.AttachmentConstants;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.security.PermissionChecker;
import com.wiki.common.security.UserContext;
import com.wiki.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 附件模块接口级鉴权 Resolver。
 * <p>
 * 遵循 {@code AGENTS.md} 4.3 与 {@code docs/common/业务流转公共规范.md} 4.6：
 * 鉴权核心参数必须是"接口 + 文档 ID"，resolver 至少接收当前用户、接口标识、文档 ID。
 * <p>
 * 鉴权规则对应 {@code docs/admin/附件机制.md} 权限与接口约定：
 * <ul>
 *   <li>列表查询 / 上传 / 下载：仅要求登录态，不做权限过滤</li>
 *   <li>系统配置维护：要求 admin-attachment::ADMIN 权限</li>
 *   <li>删除：限制为上传者本人或具备 admin-attachment::ADMIN 的用户</li>
 * </ul>
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Component
public class AdminAttachmentResolver {

    /**
     * 要求登录态（任何已登录用户），不做权限过滤。
     * <p>
     * 用于上传 / 下载接口。
     *
     * @param fieldId 文档 ID（附件信息id）；可空
     */
    public void requireLogin(String fieldId) {
        String userId = currentUserId();
        // 登录态由 Security 全局认证保障；此处仅记录可追踪鉴权审计
        log.info("[鉴权] 附件登录态操作放行 userId={} 文档ID={}", userId, fieldId);
    }

    /**
     * 要求当前用户具备附件管理员权限。
     * <p>
     * 用于系统配置维护接口。
     */
    public void requireAdmin() {
        String userId = currentUserId();
        // 权限码即接口标识，不通过则 PermissionChecker 抛 403
        PermissionChecker.require(AttachmentConstants.PERMISSION_ADMIN);
        // 记录可追踪鉴权审计：当前用户 + 接口标识（权限码） + 允许结论
        log.info("[鉴权] 附件管理员操作放行 userId={} 接口={}",
                userId, AttachmentConstants.PERMISSION_ADMIN);
    }

    /**
     * 删除鉴权：限制为上传者本人或具备 admin-attachment::ADMIN 的用户。
     *
     * @param fieldId       文档 ID（附件信息id）
     * @param uploaderId    附件上传者id（admin_attachment_main.field_uploader_id）
     */
    public void requireDelete(String fieldId, String uploaderId) {
        String userId = currentUserId();
        // 1. 具备附件管理员权限直接放行
        if (PermissionChecker.has(AttachmentConstants.PERMISSION_ADMIN)) {
            log.info("[鉴权] 附件删除放行 userId={} 接口={} 原因=ADMIN 文档ID={}",
                    userId, AttachmentConstants.PERMISSION_ADMIN, fieldId);
            return;
        }
        // 2. 否则必须是上传者本人
        if (!StringUtil.isEmpty(uploaderId) && uploaderId.equals(userId)) {
            log.info("[鉴权] 附件删除放行 userId={} 接口={} 原因=UPLOADER 文档ID={}",
                    userId, AttachmentConstants.PERMISSION_ADMIN, fieldId);
            return;
        }
        log.warn("[鉴权] 附件删除拒绝 userId={} 接口={} 文档ID={}",
                userId, AttachmentConstants.PERMISSION_ADMIN, fieldId);
        throw new BusinessException(ErrorCode.FORBIDDEN, "仅上传者或附件管理员可删除该附件",
                AttachmentConstants.PERMISSION_ADMIN);
    }

    private String currentUserId() {
        UserContext context = UserContext.current();
        return context == null ? null : context.getUserId();
    }
}
