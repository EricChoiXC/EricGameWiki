package com.wiki.admin.sys.org.service.impl;

import com.wiki.admin.sys.org.service.IOrgCRPService;
import com.wiki.common.security.UserContext;
import com.wiki.common.security.UserContextLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link UserContextLoader} 桥接实现：将公共安全层的上下文加载委托给 org 模块 CRPService。
 * <p>
 * 遵循 {@code docs/common/业务流转公共规范.md} 2.4 包间调用方向约束：
 * 公共层不允许反向依赖具体业务模块，通过接口反转依赖。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Component
@RequiredArgsConstructor
public class OrgUserContextLoaderImpl implements UserContextLoader {

    private final IOrgCRPService orgCRPService;

    @Override
    public UserContext loadByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return orgCRPService.loadUserContext(userId);
    }
}
