package com.wiki.admin.sys.org.service.impl;

import com.wiki.admin.sys.org.dao.IOrgUserLoginLogDao;
import com.wiki.admin.sys.org.model.dto.OrgUserLoginLogDo;
import com.wiki.admin.sys.org.service.IOrgUserLoginLogService;
import com.wiki.admin.sys.org.util.OrgFieldMaps;
import com.wiki.common.constant.CommonConstants;
import com.wiki.common.model.query.QueryCondition;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.request.QueryRequest;
import com.wiki.common.model.response.ListResult;
import com.wiki.common.model.response.QueryResponse;
import com.wiki.common.util.IDUtil;
import com.wiki.common.util.QueryConditionBuilder;
import com.wiki.common.util.SqlSortBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户与权限-用户登录记录 Service 实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Service
@RequiredArgsConstructor
public class OrgUserLoginLogServiceImpl implements IOrgUserLoginLogService {

    private final IOrgUserLoginLogDao loginLogDao;

    @Override
    public ListResult<OrgUserLoginLogDo> list(ApiRequest<OrgUserLoginLogDo> request) {
        QueryRequest query = request.getQuery() == null ? new QueryRequest() : request.getQuery();
        boolean needPage = query.getNeedPage() == null || query.getNeedPage();
        int pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        int pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();

        QueryCondition root = query.getData();
        QueryConditionBuilder.Built built = QueryConditionBuilder.build(root, OrgFieldMaps.LOGIN_LOG);
        String orderBy = SqlSortBuilder.buildOrderBy(query.getSortField(), query.getSortOrder(), OrgFieldMaps.LOGIN_LOG);
        if (orderBy == null) {
            orderBy = "ORDER BY field_login_time DESC";
        }

        long total = loginLogDao.countByCondition(built.getWhereSql(), built.getParams());
        long offset = needPage ? (long) (pageNum - 1) * pageSize : 0;
        List<OrgUserLoginLogDo> records = loginLogDao.selectByCondition(
                built.getWhereSql(), orderBy, offset, pageSize, needPage, built.getParams());
        return new ListResult<>(records, QueryResponse.of(total, pageNum, pageSize));
    }

    @Override
    public ListResult<OrgUserLoginLogDo> listByUser(String userId, Integer pageNum, Integer pageSize) {
        int num = pageNum == null || pageNum < 1 ? CommonConstants.DEFAULT_PAGE_NUM : pageNum;
        int size = pageSize == null || pageSize < 1 ? CommonConstants.DEFAULT_PAGE_SIZE : pageSize;
        long total = loginLogDao.countByUserId(userId);
        long offset = (long) (num - 1) * size;
        List<OrgUserLoginLogDo> records = loginLogDao.selectByUserId(userId, offset, size, true);
        return new ListResult<>(records, QueryResponse.of(total, num, size));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(String userId, String loginIp, boolean success, String message) {
        OrgUserLoginLogDo log = new OrgUserLoginLogDo();
        log.setFieldId(IDUtil.initID());
        log.setFieldUserId(userId);
        log.setFieldLoginTime(LocalDateTime.now());
        log.setFieldLoginIp(loginIp);
        log.setFieldLoginSuccess(success ? 1 : 0);
        log.setFieldMessage(message);
        loginLogDao.insert(log);
    }
}
