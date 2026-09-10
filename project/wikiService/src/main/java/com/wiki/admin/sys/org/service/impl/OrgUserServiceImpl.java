package com.wiki.admin.sys.org.service.impl;

import com.wiki.admin.sys.common.service.ICommonSettingService;
import com.wiki.admin.sys.org.dao.IOrgAuthRoleDao;
import com.wiki.admin.sys.org.dao.IOrgAuthUserDao;
import com.wiki.admin.sys.org.dao.IOrgRoleDao;
import com.wiki.admin.sys.org.dao.IOrgUserDao;
import com.wiki.admin.sys.org.dao.IOrgUserLoginLogDao;
import com.wiki.admin.sys.org.dao.IOrgUserPasswordLogDao;
import com.wiki.admin.sys.org.model.dto.OrgAuthDo;
import com.wiki.admin.sys.org.model.dto.OrgRoleDo;
import com.wiki.admin.sys.org.model.dto.OrgUserDo;
import com.wiki.admin.sys.org.service.IOrgAuthService;
import com.wiki.admin.sys.org.service.IOrgRoleService;
import com.wiki.admin.sys.org.service.IOrgUserLoginLogService;
import com.wiki.admin.sys.org.service.IOrgUserPasswordLogService;
import com.wiki.admin.sys.org.service.IOrgUserService;
import com.wiki.admin.sys.org.util.OrgConstants;
import com.wiki.admin.sys.org.util.OrgFieldMaps;
import com.wiki.common.constant.CommonConstants;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.query.QueryCondition;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.request.QueryRequest;
import com.wiki.common.model.response.ListResult;
import com.wiki.common.model.response.QueryResponse;
import com.wiki.common.util.IDUtil;
import com.wiki.common.util.PasswordUtil;
import com.wiki.common.util.QueryConditionBuilder;
import com.wiki.common.util.SqlSortBuilder;
import com.wiki.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 用户与权限-用户 Service 实现，对应 {@code admin_org_user} 表。
 * <p>
 * 业务逻辑：
 * <ul>
 *   <li>业务第 1 条：启动时检查并新建默认 admin 用户</li>
 *   <li>业务第 4 条：管理员用户不可编辑</li>
 *   <li>业务第 5 条：修改密码保存后自动登出</li>
 *   <li>业务第 6 条：密码使用 BCrypt 加密管理</li>
 *   <li>页面：登录名/手机号/邮箱不能重复；登录名不可为手机号（纯数字）或邮箱（含 '@'）</li>
 *   <li>配置项：密码最小长度、密码不能相同、默认密码、登录失败锁定次数</li>
 * </ul>
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgUserServiceImpl implements IOrgUserService {

    /** 纯数字（手机号格式）正则 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d+$");

    private final IOrgUserDao userDao;
    private final IOrgRoleDao roleDao;
    private final IOrgAuthUserDao authUserDao;
    private final IOrgAuthRoleDao authRoleDao;
    private final IOrgUserPasswordLogDao passwordLogDao;
    private final IOrgUserPasswordLogService passwordLogService;
    private final IOrgUserLoginLogService loginLogService;
    private final IOrgUserLoginLogDao loginLogDao;
    private final IOrgRoleService roleService;
    private final IOrgAuthService authService;
    private final ICommonSettingService commonSettingService;

    @Override
    public ListResult<OrgUserDo> list(ApiRequest<OrgUserDo> request) {
        QueryRequest query = request.getQuery() == null ? new QueryRequest() : request.getQuery();
        boolean needPage = query.getNeedPage() == null || query.getNeedPage();
        int pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        int pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();

        QueryCondition root = query.getData();
        QueryConditionBuilder.Built built = QueryConditionBuilder.build(root, OrgFieldMaps.USER);
        String orderBy = SqlSortBuilder.buildOrderBy(query.getSortField(), query.getSortOrder(), OrgFieldMaps.USER);
        if (orderBy == null) {
            orderBy = "ORDER BY field_create_time DESC";
        }

        long total = userDao.countByCondition(built.getWhereSql(), built.getParams());
        long offset = needPage ? (long) (pageNum - 1) * pageSize : 0;
        List<OrgUserDo> records = userDao.selectByCondition(
                built.getWhereSql(), orderBy, offset, pageSize, needPage, built.getParams());
        // 密码字段剥离，遵循 docs/common/业务流转公共规范.md 3.6
        records.forEach(this::stripPassword);
        return new ListResult<>(records, QueryResponse.of(total, pageNum, pageSize));
    }

    @Override
    public OrgUserDo load(String fieldId) {
        OrgUserDo user = userDao.selectById(fieldId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        stripPassword(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String save(OrgUserDo user, String rawPassword) {
        validateLoginNameFormat(user.getFieldLoginName());
        validateUnique(user);
        user.setFieldId(IDUtil.initID(user.getFieldId()));
        user.setFieldStatus(user.getFieldStatus() == null ? OrgConstants.STATUS_ENABLED : user.getFieldStatus());
        user.setFieldCreateTime(LocalDateTime.now());
        user.setFieldUpdateTime(LocalDateTime.now());
        user.setFieldLockFlag(0);
        String password = StringUtil.isEmpty(rawPassword)
                ? commonSettingService.getValueOrDefault(OrgConstants.SETTING_DEFAULT_PASSWORD, OrgConstants.DEFAULT_PASSWORD)
                : rawPassword;
        validatePasswordLength(password);
        user.setFieldPassword(PasswordUtil.encode(password));
        userDao.insert(user);
        passwordLogService.record(user.getFieldId(), user.getFieldPassword());
        return user.getFieldId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(OrgUserDo user) {
        OrgUserDo exists = load(user.getFieldId());
        guardAdminUserMutation(exists);
        validateLoginNameFormat(user.getFieldLoginName());
        validateUniqueForUpdate(user);
        OrgUserDo update = new OrgUserDo();
        update.setFieldId(exists.getFieldId());
        update.setFieldName(user.getFieldName());
        update.setFieldLoginName(user.getFieldLoginName());
        update.setFieldPhone(user.getFieldPhone());
        update.setFieldEmail(user.getFieldEmail());
        if (user.getFieldStatus() != null) {
            update.setFieldStatus(user.getFieldStatus());
        }
        update.setFieldUpdateTime(LocalDateTime.now());
        userDao.update(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String fieldId, String status) {
        OrgUserDo exists = load(fieldId);
        guardAdminUserMutation(exists);
        userDao.updateStatus(fieldId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlock(String fieldId) {
        OrgUserDo exists = load(fieldId);
        guardAdminUserMutation(exists);
        userDao.updateLock(fieldId, 0, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String userId, String oldPassword, String newPassword) {
        OrgUserDo exists = userDao.selectById(userId);
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (StringUtil.isEmpty(oldPassword) || !PasswordUtil.matches(oldPassword, exists.getFieldPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "旧密码不正确");
        }
        validatePasswordLength(newPassword);
        if (isPasswordNotEqualEnabled() && matchesHistory(userId, newPassword)) {
            throw new BusinessException(ErrorCode.CONFLICT, "新密码不能与历史密码相同");
        }
        String encoded = PasswordUtil.encode(newPassword);
        userDao.updatePassword(userId, encoded);
        passwordLogService.record(userId, encoded);
        // 业务第 5 条：保存修改后自动登出（由 Controller 通过返回值引导前端登出）
    }

    @Override
    public OrgUserDo loadByLoginNameForAuth(String loginName) {
        return userDao.selectByLoginName(loginName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginSuccess(String userId, String loginIp) {
        userDao.updateLastLogin(userId, LocalDateTime.now(), loginIp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String recordLoginFail(String userId, String loginIp) {
        if (StringUtil.isEmpty(userId)) {
            return "登录名或密码错误";
        }
        String timesStr = commonSettingService.getValueOrDefault(OrgConstants.SETTING_LOCK_LOGIN_FAIL_TIMES, "0");
        int lockTimes;
        try {
            lockTimes = Integer.parseInt(timesStr);
        } catch (NumberFormatException e) {
            lockTimes = 0;
        }
        if (lockTimes <= 0) {
            return "登录名或密码错误";
        }
        // 统计近 24 小时失败次数（简化实现：用 login_log 表统计，未锁定窗口内）
        // 由于 dao 未提供按 userId + success=0 计数的接口，此处简化为：每次失败累计，达到阈值即锁
        // 真实场景需查询连续失败次数；这里通过 login_log 简单累计最近失败次数
        long recentFails = countRecentFails(userId);
        if (recentFails + 1 >= lockTimes) {
            userDao.updateLock(userId, 1, null);
            return "登录失败次数过多，账号已被锁定";
        }
        return "登录名或密码错误";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockExpired() {
        userDao.unlockExpired(LocalDateTime.now());
    }

    /** 统计用户最近一次成功登录之后的连续失败次数（login_success=0） */
    private long countRecentFails(String userId) {
        return loginLogDao.countConsecutiveFails(userId);
    }

    @Override
    public List<String> loadPermissionCodesByUserId(String userId) {
        List<OrgAuthDo> auths = authService.loadByUserId(userId);
        if (auths.isEmpty()) {
            return List.of();
        }
        // 系统管理员角色直接放全部权限码（业务第 3 条：默认赋值所有权限）
        for (OrgAuthDo auth : auths) {
            if (OrgConstants.SYS_ADMIN_AUTH_ID.equals(auth.getFieldId())) {
                List<OrgRoleDo> all = roleDao.selectAll();
                List<String> codes = new ArrayList<>(all.size());
                for (OrgRoleDo role : all) {
                    addIfAbsent(codes, role.getFieldCode());
                }
                return codes;
            }
        }
        // 普通用户：通过 auth_role 关联查权限ID，再加载权限码
        List<String> authIds = new ArrayList<>(auths.size());
        for (OrgAuthDo auth : auths) {
            authIds.add(auth.getFieldId());
        }
        List<String> roleIds = authRoleDao.selectRoleIdsByAuthIds(authIds);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<OrgRoleDo> roles = roleDao.selectByIds(roleIds);
        List<String> codes = new ArrayList<>(roles.size());
        for (OrgRoleDo role : roles) {
            addIfAbsent(codes, role.getFieldCode());
        }
        return codes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureDefaultAdminUser() {
        OrgUserDo exists = userDao.selectById(OrgConstants.ADMIN_USER_ID);
        if (exists != null) {
            log.info("[用户与权限] 默认管理员用户已存在");
            return;
        }
        OrgUserDo admin = new OrgUserDo();
        admin.setFieldId(OrgConstants.ADMIN_USER_ID);
        admin.setFieldName(OrgConstants.ADMIN_USER_NAME);
        admin.setFieldLoginName(OrgConstants.ADMIN_LOGIN_NAME);
        String defaultPassword = commonSettingService.getValueOrDefault(
                OrgConstants.SETTING_DEFAULT_PASSWORD, OrgConstants.DEFAULT_PASSWORD);
        admin.setFieldPassword(PasswordUtil.encode(defaultPassword));
        admin.setFieldStatus(OrgConstants.STATUS_ENABLED);
        admin.setFieldCreateTime(LocalDateTime.now());
        admin.setFieldUpdateTime(LocalDateTime.now());
        admin.setFieldLockFlag(0);
        userDao.insert(admin);
        passwordLogService.record(admin.getFieldId(), admin.getFieldPassword());
        log.info("[用户与权限] 默认管理员用户已创建");
    }

    /** 密码字段剥离，遵循 docs/common/业务流转公共规范.md 3.6 */
    private void stripPassword(OrgUserDo user) {
        if (user != null) {
            user.setFieldPassword(null);
        }
    }

    /** 业务第 4 条：管理员用户不可编辑 */
    private void guardAdminUserMutation(OrgUserDo user) {
        if (user != null && OrgConstants.ADMIN_USER_ID.equals(user.getFieldId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理员用户不可被编辑",
                    OrgConstants.PERMISSION_USER);
        }
    }

    /** 校验登录名不能为手机号（纯数字）或邮箱（含 '@'）格式 */
    private void validateLoginNameFormat(String loginName) {
        if (StringUtil.isEmpty(loginName)) {
            return;
        }
        if (loginName.contains("@")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "登录名不能为邮箱格式");
        }
        if (PHONE_PATTERN.matcher(loginName).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "登录名不能为手机号格式");
        }
    }

    /** 校验登录名、手机号、邮箱唯一性（新增） */
    private void validateUnique(OrgUserDo user) {
        if (StringUtil.isNotEmpty(user.getFieldLoginName()) && userDao.selectByLoginName(user.getFieldLoginName()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "登录名已存在");
        }
        if (StringUtil.isNotEmpty(user.getFieldPhone()) && userDao.selectByPhone(user.getFieldPhone()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "手机号已存在");
        }
        if (StringUtil.isNotEmpty(user.getFieldEmail()) && userDao.selectByEmail(user.getFieldEmail()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已存在");
        }
    }

    /** 校验唯一性（编辑，排除自身） */
    private void validateUniqueForUpdate(OrgUserDo user) {
        if (StringUtil.isNotEmpty(user.getFieldLoginName())) {
            OrgUserDo other = userDao.selectByLoginName(user.getFieldLoginName());
            if (other != null && !other.getFieldId().equals(user.getFieldId())) {
                throw new BusinessException(ErrorCode.CONFLICT, "登录名已存在");
            }
        }
        if (StringUtil.isNotEmpty(user.getFieldPhone())) {
            OrgUserDo other = userDao.selectByPhone(user.getFieldPhone());
            if (other != null && !other.getFieldId().equals(user.getFieldId())) {
                throw new BusinessException(ErrorCode.CONFLICT, "手机号已存在");
            }
        }
        if (StringUtil.isNotEmpty(user.getFieldEmail())) {
            OrgUserDo other = userDao.selectByEmail(user.getFieldEmail());
            if (other != null && !other.getFieldId().equals(user.getFieldId())) {
                throw new BusinessException(ErrorCode.CONFLICT, "邮箱已存在");
            }
        }
    }

    /** 校验密码最小长度 */
    private void validatePasswordLength(String password) {
        if (StringUtil.isEmpty(password)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }
        String minStr = commonSettingService.getValueOrDefault(OrgConstants.SETTING_PASSWORD_MIN_LENGTH, "6");
        int minLen;
        try {
            minLen = Integer.parseInt(minStr);
        } catch (NumberFormatException e) {
            minLen = 6;
        }
        if (password.length() < minLen) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度不能少于 " + minLen + " 位");
        }
    }

    /** 读取"密码不能相同"配置项 */
    private boolean isPasswordNotEqualEnabled() {
        String value = commonSettingService.getValueOrDefault(OrgConstants.SETTING_PASSWORD_NOT_EQUAL_TIME, "false");
        return "true".equalsIgnoreCase(value);
    }

    /** 校验新密码是否与历史密码相同 */
    private boolean matchesHistory(String userId, String newPassword) {
        var history = passwordLogService.loadRecent(userId, 10);
        for (var log : history) {
            if (PasswordUtil.matches(newPassword, log.getFieldNewPassword())) {
                return true;
            }
        }
        return false;
    }

    private void addIfAbsent(List<String> list, String value) {
        if (value != null && !list.contains(value)) {
            list.add(value);
        }
    }
}
