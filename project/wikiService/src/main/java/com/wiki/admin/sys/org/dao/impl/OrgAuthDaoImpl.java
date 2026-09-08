package com.wiki.admin.sys.org.dao.impl;

import com.wiki.admin.sys.org.dao.IOrgAuthDao;
import com.wiki.admin.sys.org.model.dto.OrgAuthDo;
import com.wiki.admin.sys.org.model.mapper.OrgAuthMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 用户与权限-角色 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Repository
public class OrgAuthDaoImpl implements IOrgAuthDao {

    private final OrgAuthMapper mapper;

    public OrgAuthDaoImpl(OrgAuthMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public OrgAuthDo selectById(String fieldId) {
        return mapper.selectById(fieldId);
    }

    @Override
    public OrgAuthDo selectByCode(String fieldCode) {
        return mapper.selectByCode(fieldCode);
    }

    @Override
    public List<OrgAuthDo> selectByUserId(String fieldUserId) {
        return mapper.selectByUserId(fieldUserId);
    }

    @Override
    public List<OrgAuthDo> selectByCondition(String whereSql, String orderBySql, long offset, int pageSize, boolean needPage,
                                             Map<String, Object> params) {
        return mapper.selectByCondition(whereSql, orderBySql, offset, pageSize, needPage, params);
    }

    @Override
    public long countByCondition(String whereSql, Map<String, Object> params) {
        return mapper.countByCondition(whereSql, params);
    }

    @Override
    public List<OrgAuthDo> selectAll() {
        return mapper.selectAll();
    }

    @Override
    public int insert(OrgAuthDo auth) {
        return mapper.insert(auth);
    }

    @Override
    public int update(OrgAuthDo auth) {
        return mapper.update(auth);
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
