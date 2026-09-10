package com.wiki.admin.sys.org.service;

import com.wiki.admin.sys.org.model.dto.OrgUserPasswordLogDo;

import java.util.List;

/**
 * 用户与权限-用户密码变更记录 Service 接口。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgUserPasswordLogService {

    /** 记录密码变更 */
    void record(String userId, String newPassword);

    /** 查询用户最近 N 次历史密码（用于"密码不能相同"校验） */
    List<OrgUserPasswordLogDo> loadRecent(String userId, int limit);

    /** 查询用户最近一次密码变更时间（用于密码过期校验） */
    java.time.LocalDateTime loadLatestChangeTime(String userId);
}
