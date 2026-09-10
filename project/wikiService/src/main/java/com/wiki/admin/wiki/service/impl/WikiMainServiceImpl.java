package com.wiki.admin.wiki.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wiki.admin.sys.org.model.dto.OrgUserDo;
import com.wiki.admin.sys.org.service.IOrgUserService;
import com.wiki.admin.wiki.dao.IWikiMainDao;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.admin.wiki.model.dto.WikiMainVo;
import com.wiki.admin.wiki.model.request.WikiMainSaveRequest;
import com.wiki.admin.wiki.service.IWikiCRPService;
import com.wiki.admin.wiki.service.IWikiMainService;
import com.wiki.admin.wiki.util.WikiConstants;
import com.wiki.admin.wiki.util.WikiFieldMaps;
import com.wiki.common.constant.CommonConstants;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.query.QueryCondition;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.request.QueryRequest;
import com.wiki.common.model.response.ListResult;
import com.wiki.common.model.response.QueryResponse;
import com.wiki.common.util.IDUtil;
import com.wiki.common.util.JsonUtil;
import com.wiki.common.util.QueryConditionBuilder;
import com.wiki.common.util.SqlSortBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * wiki 项目 Service 实现，对应 {@code wiki_main} 表。
 * <p>
 * 业务逻辑遵循 {@code docs/admin/wiki/wiki业务逻辑.md} 6.1：
 * <ul>
 *   <li>简称全局唯一（限小写英文和数字），仅新建时可设置</li>
 *   <li>名称模糊查询同时匹配 field_name、field_en_name、field_jp_name</li>
 *   <li>维护人员校验用户有效性（经 CRPService 调用 sys.org）</li>
 *   <li>fieldExtend / fieldManagers 序列化为 JSON 存储</li>
 * </ul>
 *
 * @author Eric
 * @date 2026/9/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiMainServiceImpl implements IWikiMainService {

    /** 简称格式：小写英文和数字 */
    private static final Pattern SIMPLE_NAME_PATTERN = Pattern.compile("^[a-z0-9]+$");

    private final IWikiMainDao wikiMainDao;
    private final IWikiCRPService wikiCRPService;
    private final IOrgUserService orgUserService;

    @Override
    public ListResult<WikiMainVo> list(ApiRequest<WikiMainDo> request) {
        QueryRequest query = request.getQuery() == null ? new QueryRequest() : request.getQuery();
        boolean needPage = query.getNeedPage() == null || query.getNeedPage();
        int pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        int pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();

        QueryCondition root = query.getData();
        QueryConditionBuilder.Built built = QueryConditionBuilder.build(root, WikiFieldMaps.MAIN);
        String orderBy = SqlSortBuilder.buildOrderBy(query.getSortField(), query.getSortOrder(), WikiFieldMaps.MAIN);
        if (orderBy == null) {
            orderBy = "ORDER BY m.field_create_time DESC";
        }

        long total = wikiMainDao.countByCondition(built.getWhereSql(), built.getParams());
        long offset = needPage ? (long) (pageNum - 1) * pageSize : 0;
        List<WikiMainDo> records = wikiMainDao.selectByCondition(
                built.getWhereSql(), orderBy, offset, pageSize, needPage, built.getParams());
        List<WikiMainVo> vos = new ArrayList<>(records.size());
        for (WikiMainDo record : records) {
            vos.add(toVo(record));
        }
        return new ListResult<>(vos, QueryResponse.of(total, pageNum, pageSize));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String save(WikiMainSaveRequest request) {
        validateSimpleName(request.getFieldSimpleName());
        WikiMainDo exists = wikiMainDao.selectBySimpleName(request.getFieldSimpleName());
        if (exists != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "项目简称已存在：" + request.getFieldSimpleName());
        }
        validateManagers(request.getFieldManagers());

        WikiMainDo wikiMain = new WikiMainDo();
        wikiMain.setFieldId(IDUtil.initID(request.getFieldId()));
        wikiMain.setFieldName(request.getFieldName());
        wikiMain.setFieldEnName(request.getFieldEnName());
        wikiMain.setFieldJpName(request.getFieldJpName());
        wikiMain.setFieldSimpleName(request.getFieldSimpleName());
        wikiMain.setFieldPublishDate(request.getFieldPublishDate());
        wikiMain.setFieldCreateTime(LocalDateTime.now());
        wikiMain.setFieldStatus(request.getFieldStatus() == null
                ? WikiConstants.STATUS_ENABLED : request.getFieldStatus());
        wikiMain.setFieldExtend(serializeExtend(request.getFieldExtend()));
        wikiMain.setFieldManagers(serializeManagers(request.getFieldManagers()));
        wikiMainDao.insert(wikiMain);
        return wikiMain.getFieldId();
    }

    @Override
    public WikiMainVo load(String fieldId) {
        WikiMainDo wikiMain = wikiMainDao.selectById(fieldId);
        if (wikiMain == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 项目不存在");
        }
        return toVo(wikiMain);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WikiMainSaveRequest request) {
        WikiMainDo exists = wikiMainDao.selectById(request.getFieldId());
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 项目不存在");
        }
        validateManagers(request.getFieldManagers());

        WikiMainDo update = new WikiMainDo();
        update.setFieldId(exists.getFieldId());
        update.setFieldName(request.getFieldName());
        update.setFieldEnName(request.getFieldEnName());
        update.setFieldJpName(request.getFieldJpName());
        update.setFieldPublishDate(request.getFieldPublishDate());
        update.setFieldStatus(request.getFieldStatus());
        update.setFieldExtend(serializeExtend(request.getFieldExtend()));
        update.setFieldManagers(serializeManagers(request.getFieldManagers()));
        wikiMainDao.update(update);
    }

    @Override
    public Map<String, Object> init() {
        // 返回维护人员候选用户列表
        ListResult<OrgUserDo> userResult = orgUserService.list(new ApiRequest<>());
        List<OrgUserDo> users = userResult.getRecords();
        List<Map<String, Object>> userList = new ArrayList<>(users.size());
        for (OrgUserDo user : users) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fieldId", user.getFieldId());
            item.put("fieldName", user.getFieldName());
            userList.add(item);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("managers", userList);
        return data;
    }

    /**
     * 校验简称格式（小写英文和数字）。
     */
    private void validateSimpleName(String simpleName) {
        if (simpleName == null || simpleName.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "简称不能为空");
        }
        if (!SIMPLE_NAME_PATTERN.matcher(simpleName).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "简称仅支持小写英文和数字");
        }
    }

    /**
     * 校验维护人员用户有效性（经 CRPService 调用 sys.org）。
     */
    private void validateManagers(List<String> managers) {
        if (managers == null || managers.isEmpty()) {
            return;
        }
        for (String userId : managers) {
            if (!wikiCRPService.checkUserValid(userId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "维护人员不存在或已停用：" + userId);
            }
        }
    }

    /**
     * 序列化拓展信息明细行数组为 JSON 字符串。
     */
    private String serializeExtend(List<Map<String, Object>> extend) {
        return extend == null ? null : JsonUtil.toJson(extend);
    }

    /**
     * 序列化维护人员 id 数组为 JSON 字符串。
     */
    private String serializeManagers(List<String> managers) {
        return managers == null ? null : JsonUtil.toJson(managers);
    }

    /**
     * DO → Vo 转换，解析 fieldExtend / fieldManagers JSON 字符串为结构化对象。
     */
    private WikiMainVo toVo(WikiMainDo wikiMain) {
        WikiMainVo vo = new WikiMainVo();
        vo.setFieldId(wikiMain.getFieldId());
        vo.setFieldName(wikiMain.getFieldName());
        vo.setFieldEnName(wikiMain.getFieldEnName());
        vo.setFieldJpName(wikiMain.getFieldJpName());
        vo.setFieldSimpleName(wikiMain.getFieldSimpleName());
        vo.setFieldPublishDate(wikiMain.getFieldPublishDate());
        vo.setFieldCreateTime(wikiMain.getFieldCreateTime());
        vo.setFieldStatus(wikiMain.getFieldStatus());
        vo.setFieldExtend(parseExtend(wikiMain.getFieldExtend()));
        vo.setFieldManagers(parseManagers(wikiMain.getFieldManagers()));
        return vo;
    }

    private List<Map<String, Object>> parseExtend(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JsonUtil.fromJson(json, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    private List<String> parseManagers(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        List<String> managers = JsonUtil.fromJson(json, new TypeReference<List<String>>() {
        });
        return managers == null ? Collections.emptyList() : managers;
    }
}
