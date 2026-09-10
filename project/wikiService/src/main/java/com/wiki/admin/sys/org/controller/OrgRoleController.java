package com.wiki.admin.sys.org.controller;

import com.wiki.admin.sys.org.model.dto.OrgRoleDo;
import com.wiki.admin.sys.org.resolver.OrgUserResolver;
import com.wiki.admin.sys.org.service.IOrgRoleService;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ApiResponse;
import com.wiki.common.model.response.ListResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户与权限-权限 Controller，对应 {@code docs/admin/用户和权限管理.md} 权限列表页面。
 * <p>
 * 权限页面无操作（无新增/编辑/停用），仅展示 {@code admin_org_role} 表中由 roles.yml 同步的权限信息。
 * 路径前缀 {@code /api/v1/admin/org/role}，遵循 {@code AGENTS.md} 4.2。
 *
 * @author Eric
 * @date 2026/9/8
 */
@RestController
@RequestMapping("/api/v1/admin/org/role")
@RequiredArgsConstructor
public class OrgRoleController {

    private final IOrgRoleService roleService;
    private final OrgUserResolver userResolver;

    @PostMapping("/list")
    public ApiResponse<OrgRoleDo> list(@RequestBody ApiRequest<OrgRoleDo> request) {
        ListResult<OrgRoleDo> result = roleService.list(request);
        return ApiResponse.success(result.getRecords(), result.getQuery());
    }

    @GetMapping("/load")
    public ApiResponse<OrgRoleDo> load(@RequestParam("fieldId") String fieldId) {
        return ApiResponse.success(roleService.load(fieldId));
    }

    @PatchMapping("/updateStatus")
    public ApiResponse<Void> updateStatus(@RequestParam("fieldId") String fieldId,
                                          @RequestBody ApiRequest<OrgRoleDo> request) {
        userResolver.requireManage(fieldId);
        OrgRoleDo data = request.getData();
        roleService.updateStatus(fieldId, data == null ? null : data.getFieldStatus());
        return ApiResponse.success();
    }
}
