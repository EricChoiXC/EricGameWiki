package com.wiki.admin.wiki.resolver;

import com.wiki.admin.wiki.dao.IWikiMainDao;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.admin.wiki.service.IWikiCRPService;
import com.wiki.admin.wiki.util.WikiConstants;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.security.PermissionChecker;
import com.wiki.common.security.UserContext;
import com.wiki.common.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * wiki 模块接口级鉴权 Resolver，对应 {@code docs/admin/wiki/wiki业务逻辑.md} 5.2 双层鉴权规则。
 * <p>
 * 遵循 {@code AGENTS.md} 4.3 与 {@code docs/common/业务流转公共规范.md} 4.6：
 * 鉴权核心参数必须是"接口 + 文档 ID"，resolver 至少接收当前用户、接口标识、文档 ID，
 * 明确返回允许/拒绝结论并给出可追踪原因或权限码。
 * <p>
 * 双层鉴权规则：{@code admin-wiki::ADMIN} 权限 或 项目维护人员（{@code wiki_main.field_managers}），
 * 满足二者其一即可。维护人员判断经 {@link IWikiCRPService} 调用 sys.org 校验用户有效性，
 * 禁止 wiki 模块直接访问 admin_org_user 表。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiResolver {

    private final IWikiMainDao wikiMainDao;
    private final IWikiCRPService wikiCRPService;

    /**
     * 要求当前用户具备 wiki 管理员权限。
     * <p>
     * 对应业务逻辑文档 5.2 中"项目新建 / 编辑 / 启用停用"操作的鉴权规则。
     * 权限码即接口标识，不通过时由 {@link PermissionChecker} 抛 403。
     */
    public void requireAdmin() {
        String userId = currentUserId();
        PermissionChecker.require(WikiConstants.PERMISSION_ADMIN);
        log.info("[鉴权] wiki 管理员操作放行 userId={} 接口={}", userId, WikiConstants.PERMISSION_ADMIN);
    }

    /**
     * 要求当前用户具备 wiki 管理员权限或为指定项目的维护人员。
     * <p>
     * 对应业务逻辑文档 5.2 中"项目维护内容入口 / 数据项管理 / 数据维护 / wiki 页面维护"操作的双层鉴权规则。
     * 满足 {@code admin-wiki::ADMIN} 权限 或 项目的 {@code field_managers} 包含当前用户即可。
     *
     * @param fieldMainId 文档 ID（项目ID）
     */
    public void requireMaintain(String fieldMainId) {
        String userId = currentUserId();
        // 未登录直接拒绝
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 第一层：ADMIN 权限直接放行
        if (PermissionChecker.has(WikiConstants.PERMISSION_ADMIN)) {
            log.info("[鉴权] wiki 维护操作放行(管理员) userId={} 接口={} 文档ID={}",
                    userId, WikiConstants.PERMISSION_ADMIN, fieldMainId);
            return;
        }

        // 第二层：项目维护人员校验
        WikiMainDo wikiMain = wikiMainDao.selectById(fieldMainId);
        if (wikiMain == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 项目不存在");
        }

        List<String> managers = parseManagers(wikiMain.getFieldManagers());
        if (!managers.contains(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "无操作权限：非项目维护人员",
                    WikiConstants.PERMISSION_ADMIN);
        }

        // 维护人员需经 CRPService 调用 sys.org 校验用户有效性
        if (!wikiCRPService.checkUserValid(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "无操作权限：维护人员账号已停用或不存在",
                    WikiConstants.PERMISSION_ADMIN);
        }

        log.info("[鉴权] wiki 维护操作放行(维护人员) userId={} 接口={} 文档ID={}",
                userId, WikiConstants.PERMISSION_ADMIN, fieldMainId);
    }

    /**
     * 解析 field_managers JSON 数组为维护人员 id 列表。
     */
    private List<String> parseManagers(String managersJson) {
        if (managersJson == null || managersJson.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> managers = JsonUtil.fromJson(managersJson, new TypeReference<List<String>>() {
        });
        return managers == null ? Collections.emptyList() : managers;
    }

    private String currentUserId() {
        UserContext context = UserContext.current();
        return context == null ? null : context.getUserId();
    }
}
