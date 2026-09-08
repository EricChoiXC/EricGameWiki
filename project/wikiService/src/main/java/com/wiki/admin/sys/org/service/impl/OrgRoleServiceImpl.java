package com.wiki.admin.sys.org.service.impl;

import com.wiki.admin.sys.org.dao.IOrgRoleDao;
import com.wiki.admin.sys.org.model.dto.OrgRoleDo;
import com.wiki.admin.sys.org.service.IOrgRoleService;
import com.wiki.admin.sys.org.util.OrgFieldMaps;
import com.wiki.admin.sys.org.util.OrgRolesYmlLoader;
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

import java.io.IOException;
import java.util.List;

/**
 * 用户与权限-权限 Service 实现，对应 {@code admin_org_role} 表。
 * <p>
 * 业务第 2 条：启动时读取所有模块的 roles.yml，增量保存权限信息。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgRoleServiceImpl implements IOrgRoleService {

    private final IOrgRoleDao roleDao;

    @Override
    public ListResult<OrgRoleDo> list(ApiRequest<OrgRoleDo> request) {
        QueryRequest query = request.getQuery() == null ? new QueryRequest() : request.getQuery();
        boolean needPage = query.getNeedPage() == null || query.getNeedPage();
        int pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        int pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();

        QueryCondition root = query.getData();
        QueryConditionBuilder.Built built = QueryConditionBuilder.build(root, OrgFieldMaps.ROLE);
        String orderBy = SqlSortBuilder.buildOrderBy(query.getSortField(), query.getSortOrder(), OrgFieldMaps.ROLE);

        long total = roleDao.countByCondition(built.getWhereSql(), built.getParams());
        long offset = needPage ? (long) (pageNum - 1) * pageSize : 0;
        List<OrgRoleDo> records = roleDao.selectByCondition(
                built.getWhereSql(), orderBy, offset, pageSize, needPage, built.getParams());
        return new ListResult<>(records, QueryResponse.of(total, pageNum, pageSize));
    }

    @Override
    public OrgRoleDo load(String fieldId) {
        OrgRoleDo role = roleDao.selectById(fieldId);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权限不存在");
        }
        return role;
    }

    @Override
    public List<OrgRoleDo> listAll() {
        return roleDao.selectAll();
    }

    @Override
    public List<OrgRoleDo> loadByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return roleDao.selectByCodes(codes);
    }

    @Override
    public void updateStatus(String fieldId, String status) {
        roleDao.updateStatus(fieldId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncRolesFromYml() {
        List<OrgRolesYmlLoader.RoleItem> items;
        try {
            items = OrgRolesYmlLoader.loadAll();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取 roles.yml 失败", e);
        }
        for (OrgRolesYmlLoader.RoleItem item : items) {
            upsertRole(item);
        }
        log.info("[用户与权限] roles.yml 权限增量同步完成，共处理 {} 项", items.size());
    }

    private void upsertRole(OrgRolesYmlLoader.RoleItem item) {
        OrgRoleDo exists = roleDao.selectByCode(item.getKey());
        if (exists == null) {
            OrgRoleDo role = new OrgRoleDo();
            role.setFieldId(IDUtil.initID());
            role.setFieldName(item.getName());
            role.setFieldCode(item.getKey());
            role.setFieldStatus("ENABLED");
            roleDao.insert(role);
            return;
        }
        boolean metaChanged = !java.util.Objects.equals(exists.getFieldName(), item.getName());
        if (metaChanged) {
            OrgRoleDo update = new OrgRoleDo();
            update.setFieldId(exists.getFieldId());
            update.setFieldName(item.getName());
            roleDao.update(update);
        }
    }
}
