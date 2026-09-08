package com.wiki.admin.sys.org.dao;

import com.wiki.admin.sys.org.model.dto.OrgAuthDo;

import java.util.List;
import java.util.Map;

/**
 * 用户与权限-角色 Dao 接口。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgAuthDao {

    OrgAuthDo selectById(String fieldId);

    OrgAuthDo selectByCode(String fieldCode);

    List<OrgAuthDo> selectByUserId(String fieldUserId);

    List<OrgAuthDo> selectByCondition(String whereSql, String orderBySql, long offset, int pageSize, boolean needPage,
                                     Map<String, Object> params);

    long countByCondition(String whereSql, Map<String, Object> params);

    List<OrgAuthDo> selectAll();

    int insert(OrgAuthDo auth);

    int update(OrgAuthDo auth);

    int updateStatus(String fieldId, String fieldStatus);

    int deleteById(String fieldId);
}
