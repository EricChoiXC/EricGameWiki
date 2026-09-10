package com.wiki.admin.sys.org.controller;

import com.wiki.admin.sys.org.model.dto.OrgUserLoginLogDo;
import com.wiki.admin.sys.org.resolver.OrgLoginLogResolver;
import com.wiki.admin.sys.org.service.IOrgUserLoginLogService;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ApiResponse;
import com.wiki.common.model.response.ListResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户与权限-登录记录 Controller，对应 {@code docs/admin/用户和权限管理.md} 登录记录列表页面。
 * <p>
 * 路径前缀 {@code /api/v1/admin/org/loginLog}，遵循 {@code AGENTS.md} 4.2。
 *
 * @author Eric
 * @date 2026/9/8
 */
@RestController
@RequestMapping("/api/v1/admin/org/loginLog")
@RequiredArgsConstructor
public class OrgLoginLogController {

    private final IOrgUserLoginLogService loginLogService;
    private final OrgLoginLogResolver loginLogResolver;

    @PostMapping("/list")
    public ApiResponse<OrgUserLoginLogDo> list(@RequestBody ApiRequest<OrgUserLoginLogDo> request) {
        loginLogResolver.requireView(null);
        ListResult<OrgUserLoginLogDo> result = loginLogService.list(request);
        return ApiResponse.success(result.getRecords(), result.getQuery());
    }
}
