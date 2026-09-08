package com.wiki.common.security;

import lombok.Data;

import java.util.List;

/**
 * 当前用户上下文，供 {@code resolver} 鉴权与 Service 记录操作人使用。
 * <p>
 * 遵循 {@code docs/common/业务流转公共规范.md} 4.6 鉴权流转约定：
 * Resolver 至少接收当前用户、接口标识、文档 ID。
 * <p>
 * 使用 ThreadLocal 绑定当前线程，由认证过滤器在请求开始时写入、请求结束时清理。
 */
@Data
public class UserContext {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    /** 用户ID */
    private String userId;

    /** 用户名 */
    private String userName;

    /** 角色标识列表 */
    private List<String> roleKeys;

    /** 权限标识列表（用于按钮显示预判，不可替代后端最终鉴权） */
    private List<String> permissionKeys;

    /**
     * 获取当前线程绑定的用户上下文。
     */
    public static UserContext current() {
        return HOLDER.get();
    }

    /**
     * 绑定用户上下文到当前线程。
     */
    public static void set(UserContext context) {
        HOLDER.set(context);
    }

    /**
     * 清理当前线程绑定的用户上下文。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
