package com.wiki.admin.sys.org.dao.impl;

import com.wiki.admin.sys.org.dao.IOrgAuthRoleDao;
import com.wiki.admin.sys.org.model.mapper.OrgAuthRoleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户与权限-角色分配权限 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Repository
public class OrgAuthRoleDaoImpl implements IOrgAuthRoleDao {

    private final OrgAuthRoleMapper mapper;

    public OrgAuthRoleDaoImpl(OrgAuthRoleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<String> selectRoleIdsByAuthId(String fieldAuthId) {
        return mapper.selectRoleIdsByAuthId(fieldAuthId);
    }

    @Override
    public List<String> selectRoleIdsByAuthIds(List<String> fieldAuthIds) {
        if (fieldAuthIds == null || fieldAuthIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectRoleIdsByAuthIds(fieldAuthIds);
    }

    @Override
    public int insert(String fieldAuthId, String fieldRoleId) {
        return mapper.insert(fieldAuthId, fieldRoleId);
    }

    @Override
    public int deleteByAuthId(String fieldAuthId) {
        return mapper.deleteByAuthId(fieldAuthId);
    }

    @Override
    public int deleteByAuthIdAndRoleId(String fieldAuthId, String fieldRoleId) {
        return mapper.deleteByAuthIdAndRoleId(fieldAuthId, fieldRoleId);
    }
}
