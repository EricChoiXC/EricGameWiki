package com.wiki.admin.sys.org.service.impl;

import com.wiki.admin.sys.org.model.dto.OrgUserDo;
import com.wiki.admin.sys.org.model.request.OrgLoginRequest;
import com.wiki.admin.sys.org.model.response.OrgLoginResponse;
import com.wiki.admin.sys.org.service.IOrgCRPService;
import com.wiki.admin.sys.org.service.IOrgUserLoginLogService;
import com.wiki.admin.sys.org.service.IOrgUserPasswordLogService;
import com.wiki.admin.sys.org.service.IOrgUserService;
import com.wiki.admin.sys.org.util.OrgConstants;
import com.wiki.admin.sys.common.service.ICommonSettingService;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.security.JwtUtil;
import com.wiki.common.security.UserContext;
import com.wiki.common.util.PasswordUtil;
import com.wiki.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户与权限模块 CRPService 实现。
 * <p>
 * 仅做模块间调用入口与参数校验，业务逻辑委托给 {@link IOrgUserService} 等模块内 IService。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgCRPServiceImpl implements IOrgCRPService {

    private final IOrgUserService userService;
    private final IOrgUserLoginLogService loginLogService;
    private final IOrgUserPasswordLogService passwordLogService;
    private final ICommonSettingService commonSettingService;
    private final JwtUtil jwtUtil;

    @Override
    public UserContext loadUserContext(String userId) {
        OrgUserDo user = userService.load(userId);
        UserContext context = new UserContext();
        context.setUserId(user.getFieldId());
        context.setUserName(user.getFieldName());
        context.setPermissionKeys(userService.loadPermissionCodesByUserId(userId));
        return context;
    }

    @Override
    public List<String> batchCheckPermissions(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return List.of();
        }
        UserContext context = UserContext.current();
        if (context == null || context.getPermissionKeys() == null) {
            return List.of();
        }
        List<String> granted = new ArrayList<>();
        for (String code : permissionCodes) {
            if (context.getPermissionKeys().contains(code)) {
                granted.add(code);
            }
        }
        return granted;
    }

    @Override
    public OrgLoginResponse login(OrgLoginRequest request, String loginIp) {
        if (request == null || StringUtil.isEmpty(request.getLoginName()) || StringUtil.isEmpty(request.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "登录名与密码不能为空");
        }
        OrgUserDo user = userService.loadByLoginNameForAuth(request.getLoginName());
        if (user == null) {
            loginLogService.record(null, loginIp, false, "登录名不存在: " + request.getLoginName());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录名或密码错误");
        }
        // 已锁定：拒绝并写日志
        if (user.getFieldLockFlag() != null && user.getFieldLockFlag() == 1) {
            String msg = user.getFieldUnlockTime() == null
                    ? "用户已被锁定"
                    : "用户已被锁定，解锁时间: " + user.getFieldUnlockTime();
            loginLogService.record(user.getFieldId(), loginIp, false, msg);
            throw new BusinessException(ErrorCode.FORBIDDEN, msg);
        }
        if (!PasswordUtil.matches(request.getPassword(), user.getFieldPassword())) {
            String failMsg = userService.recordLoginFail(user.getFieldId(), loginIp);
            loginLogService.record(user.getFieldId(), loginIp, false, "密码错误");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, failMsg);
        }
        // 状态校验
        if (OrgConstants.STATUS_DISABLED.equalsIgnoreCase(user.getFieldStatus())) {
            loginLogService.record(user.getFieldId(), loginIp, false, "用户已停用");
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户已停用");
        }
        // 登录成功
        userService.recordLoginSuccess(user.getFieldId(), loginIp);
        loginLogService.record(user.getFieldId(), loginIp, true, "登录成功");
        String token = jwtUtil.generate(user.getFieldId(), user.getFieldName());
        OrgLoginResponse resp = new OrgLoginResponse();
        resp.setToken(token);
        resp.setUserId(user.getFieldId());
        resp.setUserName(user.getFieldName());
        resp.setLoginName(user.getFieldLoginName());
        // 业务：密码有效期检查，过期则标记需强制修改密码
        resp.setPasswordExpired(isPasswordExpired(user.getFieldId()));
        return resp;
    }

    /**
     * 判断用户密码是否已过期。
     * <p>
     * 业务第 5 条配套：配置项 {@code admin-org::change-password-expire-days}，
     * 为 0 时表示不过期；>0 时按最近一次密码变更时间计算是否超期。
     * 无变更记录时（如初始密码）按创建时间或直接判定为过期，强制改密。
     */
    private boolean isPasswordExpired(String userId) {
        String expireDaysStr = commonSettingService.getValueOrDefault(
                OrgConstants.SETTING_CHANGE_PASSWORD_EXPIRE_DAYS, "0");
        int expireDays;
        try {
            expireDays = Integer.parseInt(expireDaysStr);
        } catch (NumberFormatException e) {
            return false;
        }
        if (expireDays <= 0) {
            return false;
        }
        LocalDateTime latestChange = passwordLogService.loadLatestChangeTime(userId);
        if (latestChange == null) {
            // 无密码变更记录，强制改密
            return true;
        }
        return latestChange.plusDays(expireDays).isBefore(LocalDateTime.now());
    }

    @Override
    public void logout() {
        log.debug("用户登出");
    }
}
