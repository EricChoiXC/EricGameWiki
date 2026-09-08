package com.wiki.common.security;

/**
 * 用户上下文加载器，由具备用户/权限数据的模块实现。
 * <p>
 * 避免公共安全层反向依赖具体业务模块，遵循
 * {@code docs/common/业务流转公共规范.md} 2.4 包间调用方向约束。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface UserContextLoader {

    /**
     * 按用户ID加载当前用户上下文（含权限码集合）。
     *
     * @param userId 用户ID
     * @return 用户上下文；用户不存在返回 null
     */
    UserContext loadByUserId(String userId);
}
