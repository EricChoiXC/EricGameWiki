package com.wiki.admin.sys.org.model.mapper;

import com.wiki.admin.sys.org.model.dto.OrgUserLoginLogDo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户与权限-用户登录记录 MyBatis Mapper，对应 {@code admin_org_user_login_log} 表。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface OrgUserLoginLogMapper {

    /** 按用户ID查询登录记录（固定按登录时间降序，分页） */
    List<OrgUserLoginLogDo> selectByUserId(@Param("fieldUserId") String fieldUserId,
                                          @Param("offset") long offset,
                                          @Param("pageSize") int pageSize,
                                          @Param("needPage") boolean needPage);

    long countByUserId(@Param("fieldUserId") String fieldUserId);

    /** 通用查询（用于登录记录列表页面） */
    List<OrgUserLoginLogDo> selectByCondition(@Param("whereSql") String whereSql,
                                              @Param("orderBySql") String orderBySql,
                                              @Param("offset") long offset,
                                              @Param("pageSize") int pageSize,
                                              @Param("needPage") boolean needPage,
                                              @Param("params") java.util.Map<String, Object> params);

    long countByCondition(@Param("whereSql") String whereSql, @Param("params") java.util.Map<String, Object> params);

    /** 统计用户最近一次成功登录之后的连续失败次数（无成功记录则统计全部失败次数） */
    long countConsecutiveFails(@Param("fieldUserId") String fieldUserId);

    int insert(OrgUserLoginLogDo log);
}
