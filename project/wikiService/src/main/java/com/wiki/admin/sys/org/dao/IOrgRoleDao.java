package com.wiki.admin.sys.org.dao;

import com.wiki.admin.sys.org.model.dto.OrgRoleDo;

import java.util.List;
import java.util.Map;

/**
 * 用户与权限-权限 Dao 接口。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgRoleDao {

    OrgRoleDo selectById(String fieldId);

    List<OrgRoleDo> selectByIds(List<String> fieldIds);

    OrgRoleDo selectByCode(String fieldCode);

    List<OrgRoleDo> selectByCodes(List<String> fieldCodes);

    List<OrgRoleDo> selectByCondition(String whereSql, String orderBySql, long offset, int pageSize, boolean needPage,
                                      Map<String, Object> params);

    long countByCondition(String whereSql, Map<String, Object> params);

    List<OrgRoleDo> selectAll();

    int insert(OrgRoleDo role);

    int update(OrgRoleDo role);

    int updateStatus(String fieldId, String fieldStatus);

    int deleteById(String fieldId);
}
