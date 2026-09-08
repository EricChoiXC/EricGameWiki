package com.wiki.common.security;

import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;

import java.util.List;

/**
 * 权限检查工具，供各接口 Resolver 复用。
 * <p>
 * 遵循 {@code docs/common/业务流转公共规范.md} 4.6：批量权限码仅用于预判，
 * 最终鉴权由各接口 Resolver 调用本工具完成。管理员（拥有全部权限码）自然通过。
 *
 * @author Eric
 * @date 2026/9/8
 */
public final class PermissionChecker {

    private PermissionChecker() {
    }

    /**
     * 要求当前用户具备指定权限码，否则抛 403 业务异常。
     */
    public static void require(String permissionCode) {
        UserContext context = UserContext.current();
        if (context == null || context.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        List<String> keys = context.getPermissionKeys();
        if (keys == null || !keys.contains(permissionCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限：" + permissionCode, permissionCode);
        }
    }

    /**
     * 判断当前用户是否具备指定权限码（不抛异常）。
     */
    public static boolean has(String permissionCode) {
        UserContext context = UserContext.current();
        if (context == null) {
            return false;
        }
        List<String> keys = context.getPermissionKeys();
        return keys != null && keys.contains(permissionCode);
    }
}
