package com.wiki.admin.sys.org.model.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户与权限-角色分配用户 MyBatis Mapper，对应 {@code admin_org_auth_user} 表。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface OrgAuthUserMapper {

    /** 按角色ID查询其全部用户ID */
    List<String> selectUserIdsByAuthId(@Param("fieldAuthId") String fieldAuthId);

    /** 按用户ID查询其全部角色ID */
    List<String> selectAuthIdsByUserId(@Param("fieldUserId") String fieldUserId);

    int insert(@Param("fieldAuthId") String fieldAuthId, @Param("fieldUserId") String fieldUserId);

    int deleteByAuthId(@Param("fieldAuthId") String fieldAuthId);

    int deleteByAuthIdAndUserId(@Param("fieldAuthId") String fieldAuthId, @Param("fieldUserId") String fieldUserId);
}
