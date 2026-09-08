package com.wiki.admin.sys.org.service;

import com.wiki.admin.sys.org.model.dto.OrgUserLoginLogDo;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ListResult;

/**
 * 用户与权限-用户登录记录 Service 接口。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IOrgUserLoginLogService {

    /** 登录记录列表分页查询（页面-登录记录） */
    ListResult<OrgUserLoginLogDo> list(ApiRequest<OrgUserLoginLogDo> request);

    /** 按用户查询登录记录（用户详情页明细表） */
    ListResult<OrgUserLoginLogDo> listByUser(String userId, Integer pageNum, Integer pageSize);

    /** 记录登录日志 */
    void record(String userId, String loginIp, boolean success, String message);
}
