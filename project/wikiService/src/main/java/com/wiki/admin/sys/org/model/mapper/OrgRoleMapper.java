package com.wiki.admin.sys.org.model.mapper;

import com.wiki.admin.sys.org.model.dto.OrgRoleDo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户与权限-权限 MyBatis Mapper，对应 {@code admin_org_role} 表。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface OrgRoleMapper {

    OrgRoleDo selectById(@Param("fieldId") String fieldId);

    /** 按权限ID集合批量查询 */
    List<OrgRoleDo> selectByIds(@Param("fieldIds") List<String> fieldIds);

    OrgRoleDo selectByCode(@Param("fieldCode") String fieldCode);

    /** 按权限码集合批量查询 */
    List<OrgRoleDo> selectByCodes(@Param("fieldCodes") List<String> fieldCodes);

    /** 通用查询 */
    List<OrgRoleDo> selectByCondition(@Param("whereSql") String whereSql,
                                      @Param("orderBySql") String orderBySql,
                                      @Param("offset") long offset,
                                      @Param("pageSize") int pageSize,
                                      @Param("needPage") boolean needPage,
                                      @Param("params") java.util.Map<String, Object> params);

    long countByCondition(@Param("whereSql") String whereSql, @Param("params") java.util.Map<String, Object> params);

    List<OrgRoleDo> selectAll();

    int insert(OrgRoleDo role);

    int update(OrgRoleDo role);

    int updateStatus(@Param("fieldId") String fieldId, @Param("fieldStatus") String fieldStatus);

    int deleteById(@Param("fieldId") String fieldId);
}
