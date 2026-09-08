package com.wiki.admin.sys.org.dao.impl;

import com.wiki.admin.sys.org.dao.IOrgAuthUserDao;
import com.wiki.admin.sys.org.model.mapper.OrgAuthUserMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户与权限-角色分配用户 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Repository
public class OrgAuthUserDaoImpl implements IOrgAuthUserDao {

    private final OrgAuthUserMapper mapper;

    public OrgAuthUserDaoImpl(OrgAuthUserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<String> selectUserIdsByAuthId(String fieldAuthId) {
        return mapper.selectUserIdsByAuthId(fieldAuthId);
    }

    @Override
    public List<String> selectAuthIdsByUserId(String fieldUserId) {
        return mapper.selectAuthIdsByUserId(fieldUserId);
    }

    @Override
    public int insert(String fieldAuthId, String fieldUserId) {
        return mapper.insert(fieldAuthId, fieldUserId);
    }

    @Override
    public int deleteByAuthId(String fieldAuthId) {
        return mapper.deleteByAuthId(fieldAuthId);
    }

    @Override
    public int deleteByAuthIdAndUserId(String fieldAuthId, String fieldUserId) {
        return mapper.deleteByAuthIdAndUserId(fieldAuthId, fieldUserId);
    }
}
