package com.wiki.admin.sys.org.model.mapper;

import com.wiki.admin.sys.org.model.dto.OrgAuthRoleDo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户与权限-角色分配权限 MyBatis Mapper，对应 {@code admin_org_auth_role} 表。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface OrgAuthRoleMapper {

    /** 按角色ID查询其全部权限ID */
    List<String> selectRoleIdsByAuthId(@Param("fieldAuthId") String fieldAuthId);

    /** 按角色ID集合批量查询其全部权限ID（去重） */
    List<String> selectRoleIdsByAuthIds(@Param("fieldAuthIds") List<String> fieldAuthIds);

    int insert(@Param("fieldAuthId") String fieldAuthId, @Param("fieldRoleId") String fieldRoleId);

    int deleteByAuthId(@Param("fieldAuthId") String fieldAuthId);

    int deleteByAuthIdAndRoleId(@Param("fieldAuthId") String fieldAuthId, @Param("fieldRoleId") String fieldRoleId);
}
