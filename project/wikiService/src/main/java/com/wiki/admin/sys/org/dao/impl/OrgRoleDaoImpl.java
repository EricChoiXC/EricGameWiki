package com.wiki.admin.sys.org.dao.impl;

import com.wiki.admin.sys.org.dao.IOrgRoleDao;
import com.wiki.admin.sys.org.model.dto.OrgRoleDo;
import com.wiki.admin.sys.org.model.mapper.OrgRoleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 用户与权限-权限 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Repository
public class OrgRoleDaoImpl implements IOrgRoleDao {

    private final OrgRoleMapper mapper;

    public OrgRoleDaoImpl(OrgRoleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public OrgRoleDo selectById(String fieldId) {
        return mapper.selectById(fieldId);
    }

    @Override
    public List<OrgRoleDo> selectByIds(List<String> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectByIds(fieldIds);
    }

    @Override
    public OrgRoleDo selectByCode(String fieldCode) {
        return mapper.selectByCode(fieldCode);
    }

    @Override
    public List<OrgRoleDo> selectByCodes(List<String> fieldCodes) {
        return mapper.selectByCodes(fieldCodes);
    }

    @Override
    public List<OrgRoleDo> selectByCondition(String whereSql, String orderBySql, long offset, int pageSize, boolean needPage,
                                             Map<String, Object> params) {
        return mapper.selectByCondition(whereSql, orderBySql, offset, pageSize, needPage, params);
    }

    @Override
    public long countByCondition(String whereSql, Map<String, Object> params) {
        return mapper.countByCondition(whereSql, params);
    }

    @Override
    public List<OrgRoleDo> selectAll() {
        return mapper.selectAll();
    }

    @Override
    public int insert(OrgRoleDo role) {
        return mapper.insert(role);
    }

    @Override
    public int update(OrgRoleDo role) {
        return mapper.update(role);
    }

    @Override
    public int updateStatus(String fieldId, String fieldStatus) {
        return mapper.updateStatus(fieldId, fieldStatus);
    }

    @Override
    public int deleteById(String fieldId) {
        return mapper.deleteById(fieldId);
    }
}
