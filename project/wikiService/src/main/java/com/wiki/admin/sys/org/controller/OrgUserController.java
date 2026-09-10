package com.wiki.admin.sys.org.controller;

import com.wiki.admin.sys.org.model.dto.OrgUserDo;
import com.wiki.admin.sys.org.model.request.OrgChangePasswordRequest;
import com.wiki.admin.sys.org.model.request.OrgUserSaveRequest;
import com.wiki.admin.sys.org.resolver.OrgUserResolver;
import com.wiki.admin.sys.org.service.IOrgUserLoginLogService;
import com.wiki.admin.sys.org.service.IOrgUserService;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ApiResponse;
import com.wiki.common.model.response.ListResult;
import com.wiki.common.security.UserContext;
import com.wiki.common.util.IDUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户与权限-用户 Controller，对应 {@code docs/admin/用户和权限管理.md} 用户相关页面。
 * <p>
 * 路径前缀 {@code /api/v1/admin/org/user}，遵循 {@code AGENTS.md} 4.2。
 *
 * @author Eric
 * @date 2026/9/8
 */
@RestController
@RequestMapping("/api/v1/admin/org/user")
@RequiredArgsConstructor
public class OrgUserController {

    private final IOrgUserService userService;
    private final IOrgUserLoginLogService loginLogService;
    private final OrgUserResolver userResolver;

    @PostMapping("/list")
    public ApiResponse<OrgUserDo> list(@RequestBody ApiRequest<OrgUserDo> request) {
        ListResult<OrgUserDo> result = userService.list(request);
        return ApiResponse.success(result.getRecords(), result.getQuery());
    }

    @GetMapping("/load")
    public ApiResponse<OrgUserDo> load(@RequestParam("fieldId") String fieldId) {
        return ApiResponse.success(userService.load(fieldId));
    }

    @PostMapping("/save")
    public ApiResponse<String> save(@RequestBody @Valid ApiRequest<OrgUserSaveRequest> request) {
        userResolver.requireManage(request.getData().getFieldId());
        OrgUserSaveRequest data = request.getData();
        OrgUserDo user = new OrgUserDo();
        user.setFieldId(IDUtil.initID(data.getFieldId()));
        user.setFieldName(data.getFieldName());
        user.setFieldLoginName(data.getFieldLoginName());
        user.setFieldPhone(data.getFieldPhone());
        user.setFieldEmail(data.getFieldEmail());
        user.setFieldStatus(data.getFieldStatus());
        user.setFieldPassword(data.getFieldPassword());
        String id = userService.save(user, data.getFieldPassword());
        return ApiResponse.success(id);
    }

    @PatchMapping("/update")
    public ApiResponse<Void> update(@RequestBody @Valid ApiRequest<OrgUserSaveRequest> request) {
        userResolver.requireManage(request.getData().getFieldId());
        OrgUserSaveRequest data = request.getData();
        OrgUserDo user = new OrgUserDo();
        user.setFieldId(data.getFieldId());
        user.setFieldName(data.getFieldName());
        user.setFieldLoginName(data.getFieldLoginName());
        user.setFieldPhone(data.getFieldPhone());
        user.setFieldEmail(data.getFieldEmail());
        user.setFieldStatus(data.getFieldStatus());
        userService.update(user);
        return ApiResponse.success();
    }

    @PatchMapping("/updateStatus")
    public ApiResponse<Void> updateStatus(@RequestParam("fieldId") String fieldId,
                                          @RequestBody ApiRequest<OrgUserDo> request) {
        userResolver.requireManage(fieldId);
        OrgUserDo data = request.getData();
        userService.updateStatus(fieldId, data == null ? null : data.getFieldStatus());
        return ApiResponse.success();
    }

    @PatchMapping("/unlock")
    public ApiResponse<Void> unlock(@RequestParam("fieldId") String fieldId) {
        userResolver.requireManage(fieldId);
        userService.unlock(fieldId);
        return ApiResponse.success();
    }

    @PostMapping("/changePassword")
    public ApiResponse<Void> changePassword(@RequestBody @Valid OrgChangePasswordRequest request) {
        UserContext context = UserContext.current();
        if (context == null || context.getUserId() == null) {
            throw new com.wiki.common.exception.BusinessException(com.wiki.common.exception.ErrorCode.UNAUTHORIZED);
        }
        userService.changePassword(context.getUserId(), request.getOldPassword(), request.getNewPassword());
        // 业务第 5 条：保存修改后自动登出（前端清除 Token 即可）
        return ApiResponse.success();
    }

    @GetMapping("/loginLogs")
    public ApiResponse<com.wiki.admin.sys.org.model.dto.OrgUserLoginLogDo> loginLogs(
            @RequestParam("fieldId") String fieldId,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        ListResult<com.wiki.admin.sys.org.model.dto.OrgUserLoginLogDo> result =
                loginLogService.listByUser(fieldId, pageNum, pageSize);
        return ApiResponse.success(result.getRecords(), result.getQuery());
    }
}
