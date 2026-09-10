package com.wiki.admin.sys.org.controller;

import com.wiki.admin.sys.org.model.dto.OrgAuthDo;
import com.wiki.admin.sys.org.model.request.OrgAuthSaveRequest;
import com.wiki.admin.sys.org.model.request.OrgLoginRequest;
import com.wiki.admin.sys.org.model.response.OrgLoginResponse;
import com.wiki.admin.sys.org.resolver.OrgAuthResolver;
import com.wiki.admin.sys.org.service.IOrgAuthService;
import com.wiki.admin.sys.org.service.IOrgCRPService;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ApiResponse;
import com.wiki.common.model.response.ListResult;
import com.wiki.common.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户与权限-角色 与 认证 Controller，对应 {@code docs/admin/用户和权限管理.md} 角色、登录相关页面。
 * <p>
 * 路径前缀显式包含完整 {@code /api/v1/admin/org/auth}，遵循 {@code AGENTS.md} 4.2。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/org/auth")
@RequiredArgsConstructor
public class OrgAuthController {

    private static final String SYS_ADMIN_CODE = "sys_admin";

    private final IOrgAuthService authService;
    private final IOrgCRPService crpService;
    private final OrgAuthResolver authResolver;

    @PostMapping("/login")
    public ApiResponse<OrgLoginResponse> login(@RequestBody ApiRequest<OrgLoginRequest> request,
                                                HttpServletRequest httpRequest) {
        OrgLoginRequest data = request.getData();
        if (data == null) {
            data = new OrgLoginRequest();
        }
        data.setLoginIp(resolveClientIp(httpRequest));
        log.info("用户登录请求: loginName={}", data.getLoginName());
        return ApiResponse.success(crpService.login(data, data.getLoginIp()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        crpService.logout();
        return ApiResponse.success();
    }

    @PostMapping("/batchCheckPermissions")
    public ApiResponse<String> batchCheckPermissions(@RequestBody List<String> permissionCodes) {
        return ApiResponse.success(JsonUtil.toJson(crpService.batchCheckPermissions(permissionCodes)));
    }

    @PostMapping("/list")
    public ApiResponse<OrgAuthDo> list(@RequestBody ApiRequest<OrgAuthDo> request) {
        ListResult<OrgAuthDo> result = authService.list(request);
        return ApiResponse.success(result.getRecords(), result.getQuery());
    }

    @GetMapping("/load")
    public ApiResponse<OrgAuthSaveRequest> load(@RequestParam("fieldId") String fieldId) {
        return ApiResponse.success(authService.loadDetail(fieldId));
    }

    @PostMapping("/save")
    public ApiResponse<String> save(@RequestBody @Valid ApiRequest<OrgAuthSaveRequest> request) {
        authResolver.requireManage(request.getData().getFieldId());
        String id = authService.save(request.getData());
        return ApiResponse.success(id);
    }

    @PatchMapping("/update")
    public ApiResponse<Void> update(@RequestBody @Valid ApiRequest<OrgAuthSaveRequest> request) {
        authResolver.requireManage(request.getData().getFieldId());
        authService.update(request.getData());
        return ApiResponse.success();
    }

    @PatchMapping("/updateStatus")
    public ApiResponse<Void> updateStatus(@RequestParam("fieldId") String fieldId,
                                          @RequestBody ApiRequest<OrgAuthDo> request) {
        authResolver.requireManage(fieldId);
        OrgAuthDo exists = authService.load(fieldId);
        if (exists != null && SYS_ADMIN_CODE.equals(exists.getFieldCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统管理员角色不可修改状态");
        }
        OrgAuthDo data = request.getData();
        authService.updateStatus(fieldId, data == null ? null : data.getFieldStatus());
        return ApiResponse.success();
    }

    @PatchMapping("/updateAuthRoles")
    public ApiResponse<Void> updateAuthRoles(@RequestParam("authId") String authId,
                                             @RequestBody List<String> roleIds) {
        authResolver.requireManage(authId);
        authService.updateAuthRoles(authId, roleIds);
        return ApiResponse.success();
    }

    @PatchMapping("/updateAuthUsers")
    public ApiResponse<Void> updateAuthUsers(@RequestParam("authId") String authId,
                                              @RequestBody List<String> userIds) {
        authResolver.requireManage(authId);
        authService.updateAuthUsers(authId, userIds);
        return ApiResponse.success();
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }
        return ip;
    }
}
