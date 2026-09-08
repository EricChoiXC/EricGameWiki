package com.wiki.admin.sys.org.service;

import com.wiki.admin.sys.org.model.dto.OrgUserDo;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ListResult;

import java.util.List;

/**
 * 用户与权限-用户 Service 接口，承载业务逻辑。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgUserService {

    /**
     * 用户列表分页查询（密码字段不出参）。
     */
    ListResult<OrgUserDo> list(ApiRequest<OrgUserDo> request);

    /**
     * 加载用户详情（密码字段不出参）。
     */
    OrgUserDo load(String fieldId);

    /**
     * 新建用户；返回新用户ID。
     */
    String save(OrgUserDo user, String rawPassword);

    /**
     * 编辑用户基本信息。
     */
    void update(OrgUserDo user);

    /**
     * 启用/停用用户。
     */
    void updateStatus(String fieldId, String status);

    /**
     * 解除用户锁定。
     */
    void unlock(String fieldId);

    /**
     * 修改当前用户密码；保存后自动登出。
     */
    void changePassword(String userId, String oldPassword, String newPassword);

    /**
     * 记录登录成功并更新最后登录时间/IP。
     */
    void recordLoginSuccess(String userId, String loginIp);

    /**
     * 记录登录失败并按配置累计锁定；返回前端可读消息。
     */
    String recordLoginFail(String userId, String loginIp);

    /**
     * 解锁已到期的用户（unlock_time <= now）；定时任务调用。
     */
    void unlockExpired();

    /**
     * 按登录名查询用户（含密码，仅用于登录校验内部使用）。
     */
    OrgUserDo loadByLoginNameForAuth(String loginName);

    /**
     * 按用户ID加载其全部权限码（用于上下文与按钮预判）。
     */
    List<String> loadPermissionCodesByUserId(String userId);

    /**
     * 启动初始化：检查并新建默认 admin 用户。
     */
    void ensureDefaultAdminUser();
}
