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

    /** 查询用户最近一次密码变更时间（用于密码过期校验） */
    java.time.LocalDateTime selectLatestChangeTime(String fieldUserId);

    int insert(OrgUserPasswordLogDo log);
}
