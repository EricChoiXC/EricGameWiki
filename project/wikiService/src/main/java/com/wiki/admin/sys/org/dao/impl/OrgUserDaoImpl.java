package com.wiki.admin.sys.org.dao.impl;

import com.wiki.admin.sys.org.dao.IOrgUserDao;
import com.wiki.admin.sys.org.model.dto.OrgUserDo;
import com.wiki.admin.sys.org.model.mapper.OrgUserMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户与权限-用户 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Repository
public class OrgUserDaoImpl implements IOrgUserDao {

    private final OrgUserMapper mapper;

    public OrgUserDaoImpl(OrgUserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public OrgUserDo selectById(String fieldId) {
        return mapper.selectById(fieldId);
    }

    @Override
    public OrgUserDo selectByLoginName(String fieldLoginName) {
        return mapper.selectByLoginName(fieldLoginName);
    }

    @Override
    public OrgUserDo selectByPhone(String fieldPhone) {
        return mapper.selectByPhone(fieldPhone);
    }

    @Override
    public OrgUserDo selectByEmail(String fieldEmail) {
        return mapper.selectByEmail(fieldEmail);
    }

    @Override
    public List<OrgUserDo> selectByCondition(String whereSql, String orderBySql, long offset, int pageSize, boolean needPage,
                                             Map<String, Object> params) {
        return mapper.selectByCondition(whereSql, orderBySql, offset, pageSize, needPage, params);
    }

    @Override
    public long countByCondition(String whereSql, Map<String, Object> params) {
        return mapper.countByCondition(whereSql, params);
    }

    @Override
    public int insert(OrgUserDo user) {
        return mapper.insert(user);
    }

    @Override
    public int update(OrgUserDo user) {
        return mapper.update(user);
    }

    @Override
    public int updatePassword(String fieldId, String fieldPassword) {
        return mapper.updatePassword(fieldId, fieldPassword);
    }

    @Override
    public int updateLastLogin(String fieldId, LocalDateTime fieldLoginTime, String fieldLoginIp) {
        return mapper.updateLastLogin(fieldId, fieldLoginTime, fieldLoginIp);
    }

    @Override
    public int updateStatus(String fieldId, String fieldStatus) {
        return mapper.updateStatus(fieldId, fieldStatus);
    }

    @Override
    public int updateLock(String fieldId, Integer fieldLockFlag, LocalDateTime fieldUnlockTime) {
        return mapper.updateLock(fieldId, fieldLockFlag, fieldUnlockTime);
    }

    @Override
    public int unlockExpired(LocalDateTime now) {
        return mapper.unlockExpired(now);
    }
}
