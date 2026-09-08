package com.wiki.admin.sys.org.model.mapper;

import com.wiki.admin.sys.org.model.dto.OrgAuthDo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户与权限-角色 MyBatis Mapper，对应 {@code admin_org_auth} 表。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface OrgAuthMapper {

    OrgAuthDo selectById(@Param("fieldId") String fieldId);

    OrgAuthDo selectByCode(@Param("fieldCode") String fieldCode);

    /** 按用户ID查询所属角色（关联 admin_org_auth_user） */
    List<OrgAuthDo> selectByUserId(@Param("fieldUserId") String fieldUserId);

    /** 通用查询 */
    List<OrgAuthDo> selectByCondition(@Param("whereSql") String whereSql,
                                      @Param("orderBySql") String orderBySql,
                                      @Param("offset") long offset,
                                      @Param("pageSize") int pageSize,
                                      @Param("needPage") boolean needPage,
                                      @Param("params") java.util.Map<String, Object> params);

    long countByCondition(@Param("whereSql") String whereSql, @Param("params") java.util.Map<String, Object> params);

    List<OrgAuthDo> selectAll();

    int insert(OrgAuthDo auth);

    int update(OrgAuthDo auth);

    int updateStatus(@Param("fieldId") String fieldId, @Param("fieldStatus") String fieldStatus);

    int deleteById(@Param("fieldId") String fieldId);
}
