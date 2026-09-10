package com.wiki.admin.sys.org.dao.impl;

import com.wiki.admin.sys.org.dao.IOrgUserLoginLogDao;
import com.wiki.admin.sys.org.model.dto.OrgUserLoginLogDo;
import com.wiki.admin.sys.org.model.mapper.OrgUserLoginLogMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 用户与权限-用户登录记录 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Repository
public class OrgUserLoginLogDaoImpl implements IOrgUserLoginLogDao {

    private final OrgUserLoginLogMapper mapper;

    public OrgUserLoginLogDaoImpl(OrgUserLoginLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<OrgUserLoginLogDo> selectByUserId(String fieldUserId, long offset, int pageSize, boolean needPage) {
        return mapper.selectByUserId(fieldUserId, offset, pageSize, needPage);
    }

    @Override
    public long countByUserId(String fieldUserId) {
        return mapper.countByUserId(fieldUserId);
    }

    @Override
    public List<OrgUserLoginLogDo> selectByCondition(String whereSql, String orderBySql, long offset, int pageSize,
                                                      boolean needPage, Map<String, Object> params) {
        return mapper.selectByCondition(whereSql, orderBySql, offset, pageSize, needPage, params);
    }

    @Override
    public long countByCondition(String whereSql, Map<String, Object> params) {
        return mapper.countByCondition(whereSql, params);
    }

    @Override
    public long countConsecutiveFails(String fieldUserId) {
        return mapper.countConsecutiveFails(fieldUserId);
    }

    @Override
    public int insert(OrgUserLoginLogDo log) {
        return mapper.insert(log);
    }
}
