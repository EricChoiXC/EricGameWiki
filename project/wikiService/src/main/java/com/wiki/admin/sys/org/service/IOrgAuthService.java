package com.wiki.admin.sys.org.service;

import com.wiki.admin.sys.org.model.dto.OrgAuthDo;
import com.wiki.admin.sys.org.model.request.OrgAuthSaveRequest;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ListResult;

import java.util.List;

/**
 * 用户与权限-角色 Service 接口。
 * <p>
 * 对应 {@code admin_org_auth} 表（文档命名为 auth，语义为角色），
 * 通过 auth_role / auth_user 关联权限与用户。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgAuthService {

    ListResult<OrgAuthDo> list(ApiRequest<OrgAuthDo> request);

    OrgAuthDo load(String fieldId);

    /** 加载角色详情，含权限ID列表与用户ID列表 */
    OrgAuthSaveRequest loadDetail(String fieldId);

    /** 新建角色；返回角色ID */
    String save(OrgAuthSaveRequest request);

    /** 编辑角色基本信息（不含权限/用户分配） */
    void update(OrgAuthSaveRequest request);

    /** 更新角色分配的权限（先清空后重建） */
    void updateAuthRoles(String authId, List<String> roleIds);

    /** 更新角色分配的用户（先清空后重建） */
    void updateAuthUsers(String authId, List<String> userIds);

    void updateStatus(String fieldId, String status);

    /** 按用户ID查询其角色列表 */
    List<OrgAuthDo> loadByUserId(String userId);

    /** 启动初始化：检查并新建默认系统管理员角色，赋值所有权限和 admin 用户 */
    void ensureDefaultSysAdminRole();
}
