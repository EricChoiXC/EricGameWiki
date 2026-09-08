package com.wiki.admin.sys.org.service.impl;

import com.wiki.admin.sys.org.dao.IOrgAuthDao;
import com.wiki.admin.sys.org.dao.IOrgAuthRoleDao;
import com.wiki.admin.sys.org.dao.IOrgAuthUserDao;
import com.wiki.admin.sys.org.dao.IOrgRoleDao;
import com.wiki.admin.sys.org.dao.IOrgUserDao;
import com.wiki.admin.sys.org.model.dto.OrgAuthDo;
import com.wiki.admin.sys.org.model.dto.OrgRoleDo;
import com.wiki.admin.sys.org.model.request.OrgAuthSaveRequest;
import com.wiki.admin.sys.org.service.IOrgAuthService;
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
import com.wiki.common.util.QueryConditionBuilder;
import com.wiki.common.util.SqlSortBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户与权限-角色 Service 实现，对应 {@code admin_org_auth} 表。
 * <p>
 * 通过 {@code admin_org_auth_role} / {@code admin_org_auth_user} 关联权限与用户。
 * 业务第 3 条：启动时新建默认系统管理员角色，赋值所有权限与 admin 用户。
 * 业务第 4 条：管理员用户、系统管理员角色的权限/用户分配不可被修改。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgAuthServiceImpl implements IOrgAuthService {

    private final IOrgAuthDao authDao;
    private final IOrgRoleDao roleDao;
    private final IOrgUserDao userDao;
    private final IOrgAuthRoleDao authRoleDao;
    private final IOrgAuthUserDao authUserDao;

    @Override
    public ListResult<OrgAuthDo> list(ApiRequest<OrgAuthDo> request) {
        QueryRequest query = request.getQuery() == null ? new QueryRequest() : request.getQuery();
        boolean needPage = query.getNeedPage() == null || query.getNeedPage();
        int pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        int pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();

        QueryCondition root = query.getData();
        QueryConditionBuilder.Built built = QueryConditionBuilder.build(root, OrgFieldMaps.AUTH);
        String orderBy = SqlSortBuilder.buildOrderBy(query.getSortField(), query.getSortOrder(), OrgFieldMaps.AUTH);
        if (orderBy == null) {
            orderBy = "ORDER BY field_create_time DESC";
        }

        long total = authDao.countByCondition(built.getWhereSql(), built.getParams());
        long offset = needPage ? (long) (pageNum - 1) * pageSize : 0;
        List<OrgAuthDo> records = authDao.selectByCondition(
                built.getWhereSql(), orderBy, offset, pageSize, needPage, built.getParams());
        return new ListResult<>(records, QueryResponse.of(total, pageNum, pageSize));
    }

    @Override
    public OrgAuthDo load(String fieldId) {
        OrgAuthDo auth = authDao.selectById(fieldId);
        if (auth == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return auth;
    }

    @Override
    public OrgAuthSaveRequest loadDetail(String fieldId) {
        OrgAuthDo auth = load(fieldId);
        OrgAuthSaveRequest resp = new OrgAuthSaveRequest();
        resp.setFieldId(auth.getFieldId());
        resp.setFieldName(auth.getFieldName());
        resp.setFieldCode(auth.getFieldCode());
        resp.setFieldStatus(auth.getFieldStatus());
        List<String> roleIds = authRoleDao.selectRoleIdsByAuthId(fieldId);
        List<String> userIds = authUserDao.selectUserIdsByAuthId(fieldId);
        resp.setRoleIds(roleIds);
        resp.setUserIds(userIds);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String save(OrgAuthSaveRequest request) {
        if (request.getFieldCode() != null) {
            OrgAuthDo exists = authDao.selectByCode(request.getFieldCode());
            if (exists != null) {
                throw new BusinessException(ErrorCode.CONFLICT, "角色编号已存在: " + request.getFieldCode());
            }
        }
        OrgAuthDo auth = new OrgAuthDo();
        String id = IDUtil.initID(request.getFieldId());
        auth.setFieldId(id);
        auth.setFieldName(request.getFieldName());
        auth.setFieldCode(request.getFieldCode());
        auth.setFieldStatus(request.getFieldStatus() == null ? OrgConstants.STATUS_ENABLED : request.getFieldStatus());
        auth.setFieldCreateTime(LocalDateTime.now());
        authDao.insert(auth);
        replaceAuthRoles(id, request.getRoleIds());
        replaceAuthUsers(id, request.getUserIds());
        return id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(OrgAuthSaveRequest request) {
        OrgAuthDo exists = load(request.getFieldId());
        guardSysAdminRoleMutation(exists);
        OrgAuthDo auth = new OrgAuthDo();
        auth.setFieldId(exists.getFieldId());
        auth.setFieldName(request.getFieldName());
        auth.setFieldCode(request.getFieldCode());
        if (request.getFieldStatus() != null) {
            auth.setFieldStatus(request.getFieldStatus());
        }
        authDao.update(auth);
        // 编辑时同步刷新权限/用户分配
        replaceAuthRoles(exists.getFieldId(), request.getRoleIds());
        replaceAuthUsers(exists.getFieldId(), request.getUserIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuthRoles(String authId, List<String> roleIds) {
        OrgAuthDo exists = load(authId);
        guardSysAdminRoleMutation(exists);
        replaceAuthRoles(authId, roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuthUsers(String authId, List<String> userIds) {
        OrgAuthDo exists = load(authId);
        guardSysAdminRoleMutation(exists);
        replaceAuthUsers(authId, userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String fieldId, String status) {
        OrgAuthDo exists = load(fieldId);
        guardSysAdminRoleMutation(exists);
        authDao.updateStatus(fieldId, status);
    }

    @Override
    public List<OrgAuthDo> loadByUserId(String userId) {
        return authDao.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureDefaultSysAdminRole() {
        OrgAuthDo exists = authDao.selectById(OrgConstants.SYS_ADMIN_AUTH_ID);
        if (exists == null) {
            OrgAuthDo auth = new OrgAuthDo();
            auth.setFieldId(OrgConstants.SYS_ADMIN_AUTH_ID);
            auth.setFieldName(OrgConstants.SYS_ADMIN_AUTH_NAME);
            auth.setFieldCode(OrgConstants.SYS_ADMIN_AUTH_CODE);
            auth.setFieldStatus(OrgConstants.STATUS_ENABLED);
            auth.setFieldCreateTime(LocalDateTime.now());
            authDao.insert(auth);
            log.info("[用户与权限] 默认系统管理员角色已创建");
        }
        // 默认赋值所有权限
        List<OrgRoleDo> allRoles = roleDao.selectAll();
        List<String> roleIds = new ArrayList<>(allRoles.size());
        for (OrgRoleDo role : allRoles) {
            roleIds.add(role.getFieldId());
        }
        replaceAuthRoles(OrgConstants.SYS_ADMIN_AUTH_ID, roleIds);

        // 默认赋值 admin 用户
        replaceAuthUsers(OrgConstants.SYS_ADMIN_AUTH_ID, List.of(OrgConstants.ADMIN_USER_ID));
        log.info("[用户与权限] 默认系统管理员角色已赋值 {} 个权限与 admin 用户", roleIds.size());
    }

    private void replaceAuthRoles(String authId, List<String> roleIds) {
        authRoleDao.deleteByAuthId(authId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Set<String> distinct = new HashSet<>(roleIds);
        for (String roleId : distinct) {
            authRoleDao.insert(authId, roleId);
        }
    }

    private void replaceAuthUsers(String authId, List<String> userIds) {
        authUserDao.deleteByAuthId(authId);
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Set<String> distinct = new HashSet<>(userIds);
        for (String userId : distinct) {
            authUserDao.insert(authId, userId);
        }
    }

    /**
     * 系统管理员角色的权限分配和用户分配不可被修改，业务第 4 条。
     */
    private void guardSysAdminRoleMutation(OrgAuthDo auth) {
        if (auth != null && OrgConstants.SYS_ADMIN_AUTH_ID.equals(auth.getFieldId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统管理员角色不可被修改",
                    OrgConstants.PERMISSION_ROLE);
        }
    }
}
