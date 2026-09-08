package com.wiki.admin.sys.org.service;

import com.wiki.admin.sys.org.model.dto.OrgRoleDo;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ListResult;

import java.util.List;

/**
 * 用户与权限-权限 Service 接口。
 * <p>
 * 对应 {@code admin_org_role} 表（文档命名为 role，语义为权限）。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgRoleService {

    ListResult<OrgRoleDo> list(ApiRequest<OrgRoleDo> request);

    OrgRoleDo load(String fieldId);

    List<OrgRoleDo> listAll();

    List<OrgRoleDo> loadByCodes(List<String> codes);

    void updateStatus(String fieldId, String status);

    /**
     * 启动初始化：读取所有模块的 roles.yml，增量保存权限到 admin_org_role 表。
     */
    void syncRolesFromYml();
}
