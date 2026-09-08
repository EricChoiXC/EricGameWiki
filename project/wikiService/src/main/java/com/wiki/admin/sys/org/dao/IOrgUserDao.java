package com.wiki.admin.sys.org.dao;

import com.wiki.admin.sys.org.model.dto.OrgUserDo;

import java.util.List;

/**
 * 用户与权限-用户 Dao 接口。仅与 {@code OrgUserMapper} 一对一对接，不承载业务逻辑。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgUserDao {

    OrgUserDo selectById(String fieldId);

    OrgUserDo selectByLoginName(String fieldLoginName);

    OrgUserDo selectByPhone(String fieldPhone);

    OrgUserDo selectByEmail(String fieldEmail);

    List<OrgUserDo> selectByCondition(String whereSql, String orderBySql, long offset, int pageSize, boolean needPage,
                                      java.util.Map<String, Object> params);

    long countByCondition(String whereSql, java.util.Map<String, Object> params);

    int insert(OrgUserDo user);

    int update(OrgUserDo user);

    int updatePassword(String fieldId, String fieldPassword);

    int updateLastLogin(String fieldId, java.time.LocalDateTime fieldLoginTime, String fieldLoginIp);

    int updateStatus(String fieldId, String fieldStatus);

    int updateLock(String fieldId, Integer fieldLockFlag, java.time.LocalDateTime fieldUnlockTime);

    int unlockExpired(java.time.LocalDateTime now);
}
