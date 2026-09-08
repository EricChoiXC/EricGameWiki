package com.wiki.admin.sys.org.dao;

import com.wiki.admin.sys.org.model.dto.OrgUserPasswordLogDo;

import java.util.List;

/**
 * 用户与权限-用户密码变更记录 Dao 接口。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgUserPasswordLogDao {

    List<OrgUserPasswordLogDo> selectByUserId(String fieldUserId, int limit);

    int insert(OrgUserPasswordLogDo log);
}
