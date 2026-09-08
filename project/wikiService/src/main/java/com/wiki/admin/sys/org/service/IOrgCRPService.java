package com.wiki.admin.sys.org.service;

import com.wiki.admin.sys.org.model.request.OrgLoginRequest;
import com.wiki.admin.sys.org.model.response.OrgLoginResponse;
import com.wiki.common.security.UserContext;

import java.util.List;

/**
 * 用户与权限模块跨模块调用入口 Service（CRPService）。
 * <p>
 * 遵循 {@code docs/common/业务流转公共规范.md} 2.3：
 * CRPService 只负责模块间调用入口与参数校验，不承载业务逻辑。
 * 跨模块调用方应通过本接口转发，禁止直接调用 org 模块内部 IService/Dao。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgCRPService {

    /**
     * 按 userId 加载当前用户上下文（含权限码集合），供 JWT 过滤器使用。
     */
    UserContext loadUserContext(String userId);

    /**
     * 批量鉴权预判：判断当前用户是否具备给定权限码集合。
     * 仅用于前端按钮显隐预判，不能替代后端最终鉴权。
     */
    List<String> batchCheckPermissions(List<String> permissionCodes);

    /**
     * 登录入口，校验账号密码并签发 JWT。
     */
    OrgLoginResponse login(OrgLoginRequest request, String loginIp);

    /**
     * 登出当前用户。
     */
    void logout();
}
