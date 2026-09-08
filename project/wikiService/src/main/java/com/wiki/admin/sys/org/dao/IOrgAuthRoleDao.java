package com.wiki.admin.sys.org.dao;

import java.util.List;

/**
 * 用户与权限-角色分配权限 Dao 接口。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgAuthRoleDao {

    List<String> selectRoleIdsByAuthId(String fieldAuthId);

    List<String> selectRoleIdsByAuthIds(List<String> fieldAuthIds);

    int insert(String fieldAuthId, String fieldRoleId);

    int deleteByAuthId(String fieldAuthId);

    int deleteByAuthIdAndRoleId(String fieldAuthId, String fieldRoleId);
}
