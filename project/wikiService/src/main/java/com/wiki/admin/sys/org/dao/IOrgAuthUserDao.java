package com.wiki.admin.sys.org.dao;

import java.util.List;

/**
 * 用户与权限-角色分配用户 Dao 接口。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgAuthUserDao {

    List<String> selectUserIdsByAuthId(String fieldAuthId);

    List<String> selectAuthIdsByUserId(String fieldUserId);

    int insert(String fieldAuthId, String fieldUserId);

    int deleteByAuthId(String fieldAuthId);

    int deleteByAuthIdAndUserId(String fieldAuthId, String fieldUserId);
}
