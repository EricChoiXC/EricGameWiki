package com.wiki.web.wiki.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wiki.admin.wiki.dao.IWikiMainDao;
import com.wiki.admin.wiki.dao.IWikiMainDataDao;
import com.wiki.admin.wiki.model.dto.WikiDataDetailDo;
import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.dto.WikiMainDataVo;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.admin.wiki.model.dto.WikiMainVo;
import com.wiki.admin.wiki.model.dto.WikiPageConfigDo;
import com.wiki.admin.wiki.model.request.WikiDataRequest;
import com.wiki.admin.wiki.model.response.WikiDataVo;
import com.wiki.admin.wiki.model.response.WikiPageVo;
import com.wiki.admin.wiki.service.IWikiDataService;
import com.wiki.admin.wiki.service.IWikiMainDataService;
import com.wiki.admin.wiki.service.IWikiMainService;
import com.wiki.admin.wiki.service.IWikiPageService;
import com.wiki.admin.wiki.util.DynamicTableSqlBuilder;
import com.wiki.admin.wiki.util.WikiConstants;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.query.LogicalOperator;
import com.wiki.common.model.query.QueryCondition;
import com.wiki.common.model.query.QueryOperator;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.request.QueryRequest;
import com.wiki.common.model.response.ListResult;
import com.wiki.common.util.JsonUtil;
import com.wiki.common.util.StringUtil;
import com.wiki.web.wiki.model.dto.WebWikiDataItemVo;
import com.wiki.web.wiki.model.dto.WebWikiProjectVo;
import com.wiki.web.wiki.model.dto.WebWikiRecordDetailVo;
import com.wiki.web.wiki.model.dto.WebWikiRecordListVo;
import com.wiki.web.wiki.model.dto.WebWikiRecordVo;
import com.wiki.web.wiki.service.IWebWikiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * wiki 前台只读服务实现，对应 {@code docs/wiki} 系列页面。
 * <p>
 * 组合复用 {@code com.wiki.admin.wiki} 模块的领域查询逻辑：
 * <ul>
 *   <li>项目 / 数据项元数据列表查询：委托 {@link IWikiMainService} / {@link IWikiMainDataService}</li>
 *   <li>数据明细查询（含关联项联表）：委托 {@link IWikiDataService}</li>
 *   <li>wiki 页面配置解析：委托 {@link IWikiPageService}，未配置时返回 null</li>
 *   <li>按 {@code fieldSimpleName}/{@code fieldDataName} 解析元数据：经
 *       {@link IWikiMainDao} / {@link IWikiMainDataDao} 查询，只读</li>
 * </ul>
 * 所有对外接口仅读取开启状态的项目，且不暴露维护人员等内部字段。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Service
@RequiredArgsConstructor
public class WebWikiServiceImpl implements IWebWikiService {

    private final IWikiMainService wikiMainService;
    private final IWikiMainDataService wikiMainDataService;
    private final IWikiDataService wikiDataService;
    private final IWikiPageService wikiPageService;
    private final IWikiMainDao wikiMainDao;
    private final IWikiMainDataDao wikiMainDataDao;

    @Override
    public List<WebWikiProjectVo> homeProjects(String keyword) {
        ApiRequest<WikiMainDo> request = new ApiRequest<>();
        QueryRequest query = new QueryRequest();
        query.setNeedPage(Boolean.FALSE);
        List<QueryCondition> conditions = new ArrayList<>();
        conditions.add(QueryCondition.leaf(QueryOperator.EQ, "fieldStatus", WikiConstants.STATUS_ENABLED));
        if (StringUtil.isNotEmpty(keyword)) {
            conditions.add(QueryCondition.group(LogicalOperator.OR, List.of(
                    QueryCondition.leaf(QueryOperator.LIKE, "fieldName", keyword),
                    QueryCondition.leaf(QueryOperator.LIKE, "fieldEnName", keyword),
                    QueryCondition.leaf(QueryOperator.LIKE, "fieldJpName", keyword),
                    QueryCondition.leaf(QueryOperator.LIKE, "fieldSimpleName", keyword))));
        }
        query.setData(QueryCondition.group(LogicalOperator.AND, conditions));
        request.setQuery(query);

        ListResult<WikiMainVo> result = wikiMainService.list(request);
        List<WebWikiProjectVo> vos = new ArrayList<>(result.getRecords().size());
        for (WikiMainVo vo : result.getRecords()) {
            vos.add(toProjectVo(vo));
        }
        return vos;
    }

    @Override
    public WebWikiProjectVo loadProject(String fieldSimpleName) {
        return toProjectVo(loadProjectDo(fieldSimpleName));
    }

    @Override
    public List<WebWikiDataItemVo> listDataItems(String fieldSimpleName, String keyword, String fieldDataType) {
        WikiMainDo main = loadProjectDo(fieldSimpleName);
        ApiRequest<WikiMainDataVo> request = new ApiRequest<>();
        QueryRequest query = new QueryRequest();
        query.setNeedPage(Boolean.FALSE);
        List<QueryCondition> conditions = new ArrayList<>();
        conditions.add(QueryCondition.leaf(QueryOperator.EQ, "fieldMainId", main.getFieldId()));
        if (StringUtil.isNotEmpty(keyword)) {
            conditions.add(QueryCondition.group(LogicalOperator.OR, List.of(
                    QueryCondition.leaf(QueryOperator.LIKE, "fieldName", keyword),
                    QueryCondition.leaf(QueryOperator.LIKE, "fieldDataName", keyword))));
        }
        if (StringUtil.isNotEmpty(fieldDataType)) {
            conditions.add(QueryCondition.leaf(QueryOperator.EQ, "fieldDataType", fieldDataType));
        }
        query.setData(QueryCondition.group(LogicalOperator.AND, conditions));
        request.setQuery(query);

        ListResult<WikiMainDataVo> result = wikiMainDataService.list(request);
        List<WebWikiDataItemVo> vos = new ArrayList<>(result.getRecords().size());
        for (WikiMainDataVo vo : result.getRecords()) {
            vos.add(toDataItemVo(vo));
        }
        return vos;
    }

    @Override
    public WebWikiRecordListVo listRecords(String fieldSimpleName, String fieldDataName, String keyword) {
        WikiMainDo main = loadProjectDo(fieldSimpleName);
        WikiMainDataDo dataItem = loadDataItemDo(main, fieldDataName);

        ApiRequest<WikiDataRequest> request = new ApiRequest<>();
        WikiDataRequest data = new WikiDataRequest();
        data.setFieldDataId(dataItem.getFieldId());
        request.setData(data);

        QueryRequest query = new QueryRequest();
        query.setNeedPage(Boolean.FALSE);
        switch (dataItem.getFieldDataType()) {
            case WikiConstants.DATA_TYPE_DATA -> {
                if (StringUtil.isNotEmpty(keyword)) {
                    query.setData(QueryCondition.group(LogicalOperator.OR, List.of(
                            QueryCondition.leaf(QueryOperator.LIKE, "fieldName", keyword),
                            QueryCondition.leaf(QueryOperator.LIKE, "fieldCode", keyword))));
                }
            }
            case WikiConstants.DATA_TYPE_DOC -> {
                if (StringUtil.isNotEmpty(keyword)) {
                    query.setData(QueryCondition.leaf(QueryOperator.LIKE, "fieldName", keyword));
                }
            }
            default -> {
                // 关联项：前台不提供关键字筛选，返回全部记录（field_id 降序）
            }
        }
        request.setQuery(query);

        ListResult<WikiDataVo> result = wikiDataService.list(request);
        WebWikiRecordListVo vo = new WebWikiRecordListVo();
        vo.setDataItem(toDataItemVo(dataItem));
        vo.setRecords(toRecordVos(result.getRecords()));
        return vo;
    }

    @Override
    public WebWikiRecordDetailVo loadRecord(String fieldSimpleName, String fieldDataName, String fieldId) {
        WikiMainDo main = loadProjectDo(fieldSimpleName);
        WikiMainDataDo dataItem = loadDataItemDo(main, fieldDataName);
        if (StringUtil.isEmpty(fieldId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "明细记录ID不能为空");
        }
        WikiDataVo record = wikiDataService.load(fieldId, dataItem.getFieldId());

        WebWikiRecordDetailVo vo = new WebWikiRecordDetailVo();
        vo.setDataItem(toDataItemVo(dataItem));
        vo.setRecord(toRecordVo(record));
        vo.setPageConfig(loadPageConfig(dataItem.getFieldId()));
        vo.setRelatedRecords(buildRelatedRecords(main, dataItem, fieldId));
        return vo;
    }

    // ===== 内部：元数据解析 =====

    /**
     * 按简称加载开启状态的项目；不存在或停用时抛 NOT_FOUND。
     */
    private WikiMainDo loadProjectDo(String fieldSimpleName) {
        if (StringUtil.isEmpty(fieldSimpleName)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "项目简称不能为空");
        }
        WikiMainDo main = wikiMainDao.selectBySimpleName(fieldSimpleName);
        if (main == null || !Integer.valueOf(WikiConstants.STATUS_ENABLED).equals(main.getFieldStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 项目不存在或已停用：" + fieldSimpleName);
        }
        return main;
    }

    /**
     * 按项目 + 数据项简称加载数据项元数据；不存在时抛 NOT_FOUND。
     */
    private WikiMainDataDo loadDataItemDo(WikiMainDo main, String fieldDataName) {
        if (StringUtil.isEmpty(fieldDataName)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据项简称不能为空");
        }
        WikiMainDataDo dataItem = wikiMainDataDao.selectByMainIdAndDataName(main.getFieldId(), fieldDataName);
        if (dataItem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 数据项不存在：" + fieldDataName);
        }
        return dataItem;
    }

    /**
     * 加载 wiki 页面配置；未配置时返回 null（详情页退化为默认展示）。
     */
    private WikiPageConfigDo loadPageConfig(String fieldDataId) {
        try {
            WikiPageVo vo = wikiPageService.load(fieldDataId);
            return vo == null ? null : vo.getFieldWikiPage();
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    // ===== 内部：关联展示块数据 =====

    /**
     * 查询所有引用本记录的关联项记录，键为关联类数据项 id，值为关联记录列表。
     * <p>
     * 遍历同项目关联类数据项，其 {@code field_data_json} 中任一 {@code type=join}
     * 明细的目标等于本数据项 id 时，按该关联列值等于本记录 id 过滤查询。
     */
    private Map<String, List<WebWikiRecordVo>> buildRelatedRecords(WikiMainDo main, WikiMainDataDo dataItem,
                                                                   String recordId) {
        Map<String, List<WebWikiRecordVo>> related = new LinkedHashMap<>();
        for (WikiMainDataDo sibling : wikiMainDataDao.selectByMainId(dataItem.getFieldMainId())) {
            if (sibling.getFieldId().equals(dataItem.getFieldId())
                    || !WikiConstants.DATA_TYPE_JOIN.equals(sibling.getFieldDataType())) {
                continue;
            }
            for (WikiDataDetailDo detail : DynamicTableSqlBuilder.parseDetails(sibling.getFieldDataJson())) {
                if (!WikiConstants.DATA_TYPE_JOIN.equals(detail.getType())
                        || !dataItem.getFieldId().equals(detail.getJoin())) {
                    continue;
                }
                String joinFieldKey = "field" + upperFirst(detail.getDataName()) + "Id";
                ListResult<WikiDataVo> result = wikiDataService.list(buildJoinFilterRequest(
                        sibling.getFieldId(), joinFieldKey, recordId));
                related.put(sibling.getFieldId(), toRecordVos(result.getRecords()));
            }
        }
        return related;
    }

    /**
     * 构建关联项按关联列等值过滤的列表请求。
     */
    private ApiRequest<WikiDataRequest> buildJoinFilterRequest(String fieldDataId, String joinFieldKey,
                                                               String recordId) {
        ApiRequest<WikiDataRequest> request = new ApiRequest<>();
        WikiDataRequest data = new WikiDataRequest();
        data.setFieldDataId(fieldDataId);
        request.setData(data);
        QueryRequest query = new QueryRequest();
        query.setNeedPage(Boolean.FALSE);
        query.setData(QueryCondition.leaf(QueryOperator.EQ, joinFieldKey, recordId));
        request.setQuery(query);
        return request;
    }

    // ===== 内部：DO / Vo → Web Vo =====

    private WebWikiProjectVo toProjectVo(WikiMainVo vo) {
        WebWikiProjectVo target = new WebWikiProjectVo();
        target.setFieldId(vo.getFieldId());
        target.setFieldName(vo.getFieldName());
        target.setFieldEnName(vo.getFieldEnName());
        target.setFieldJpName(vo.getFieldJpName());
        target.setFieldSimpleName(vo.getFieldSimpleName());
        target.setFieldPublishDate(vo.getFieldPublishDate());
        target.setFieldExtend(vo.getFieldExtend());
        return target;
    }

    private WebWikiProjectVo toProjectVo(WikiMainDo main) {
        WebWikiProjectVo target = new WebWikiProjectVo();
        target.setFieldId(main.getFieldId());
        target.setFieldName(main.getFieldName());
        target.setFieldEnName(main.getFieldEnName());
        target.setFieldJpName(main.getFieldJpName());
        target.setFieldSimpleName(main.getFieldSimpleName());
        target.setFieldPublishDate(main.getFieldPublishDate());
        target.setFieldExtend(parseExtend(main.getFieldExtend()));
        return target;
    }

    private WebWikiDataItemVo toDataItemVo(WikiMainDataVo vo) {
        WebWikiDataItemVo target = new WebWikiDataItemVo();
        target.setFieldId(vo.getFieldId());
        target.setFieldMainId(vo.getFieldMainId());
        target.setFieldName(vo.getFieldName());
        target.setFieldDataName(vo.getFieldDataName());
        target.setFieldDataType(vo.getFieldDataType());
        return target;
    }

    private WebWikiDataItemVo toDataItemVo(WikiMainDataDo item) {
        WebWikiDataItemVo target = new WebWikiDataItemVo();
        target.setFieldId(item.getFieldId());
        target.setFieldMainId(item.getFieldMainId());
        target.setFieldName(item.getFieldName());
        target.setFieldDataName(item.getFieldDataName());
        target.setFieldDataType(item.getFieldDataType());
        return target;
    }

    private List<WebWikiRecordVo> toRecordVos(List<WikiDataVo> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<WebWikiRecordVo> vos = new ArrayList<>(records.size());
        for (WikiDataVo record : records) {
            vos.add(toRecordVo(record));
        }
        return vos;
    }

    private WebWikiRecordVo toRecordVo(WikiDataVo record) {
        WebWikiRecordVo vo = new WebWikiRecordVo();
        vo.setFieldId(record.getFieldId());
        vo.setFieldName(record.getFieldName());
        vo.setFieldCode(record.getFieldCode());
        vo.setFieldContext(record.getFieldContext());
        vo.setFieldData(record.getFieldData());
        return vo;
    }

    private List<Map<String, Object>> parseExtend(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JsonUtil.fromJson(json, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    private String upperFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
