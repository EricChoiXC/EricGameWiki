package com.wiki.admin.sys.org.dao;

import com.wiki.admin.sys.org.model.dto.OrgUserLoginLogDo;

import java.util.List;
import java.util.Map;

/**
 * 用户与权限-用户登录记录 Dao 接口。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgUserLoginLogDao {

    List<OrgUserLoginLogDo> selectByUserId(String fieldUserId, long offset, int pageSize, boolean needPage);

    long countByUserId(String fieldUserId);

    List<OrgUserLoginLogDo> selectByCondition(String whereSql, String orderBySql, long offset, int pageSize,
                                              boolean needPage, Map<String, Object> params);

    long countByCondition(String whereSql, Map<String, Object> params);

    /** 统计用户最近一次成功登录之后的连续失败次数 */
    long countConsecutiveFails(String fieldUserId);

    int insert(OrgUserLoginLogDo log);
}
