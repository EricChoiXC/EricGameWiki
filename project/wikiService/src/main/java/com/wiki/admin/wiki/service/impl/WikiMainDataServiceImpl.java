package com.wiki.admin.wiki.service.impl;

import com.wiki.admin.wiki.dao.IWikiMainDao;
import com.wiki.admin.wiki.dao.IWikiMainDataDao;
import com.wiki.admin.wiki.dao.dynamic.WikiDynamicDataDao;
import com.wiki.admin.wiki.model.dto.WikiDataDetailDo;
import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.dto.WikiMainDataVo;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.admin.wiki.model.request.WikiMainDataSaveRequest;
import com.wiki.admin.wiki.service.IWikiMainDataService;
import com.wiki.admin.wiki.util.DynamicTableSqlBuilder;
import com.wiki.admin.wiki.util.WikiConstants;
import com.wiki.admin.wiki.util.WikiMainDataFieldMaps;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * wiki 数据项 Service 实现，对应 {@code wiki_main_data} 表。
 * <p>
 * 业务逻辑遵循 {@code docs/admin/wiki/wiki业务逻辑.md} 6.2（FLOW-W002）：
 * save 图鉴类自动补充 name/code 固定列 → 保存元数据 → 触发动态建表（DDL 失败补偿删除元数据）；
 * update fieldDataName/fieldDataType 不可改，明细变更仅允许 ALTER TABLE ADD COLUMN；
 * delete 校验被引用关系 → 删元数据 → DROP TABLE，被引用时返回 CONFLICT。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiMainDataServiceImpl implements IWikiMainDataService {

    private static final Pattern SIMPLE_NAME_PATTERN = Pattern.compile("^[a-z0-9]+$");

    private final IWikiMainDataDao wikiMainDataDao;
    private final IWikiMainDao wikiMainDao;
    private final WikiDynamicDataDao wikiDynamicDataDao;

    @Override
    public ListResult<WikiMainDataVo> list(ApiRequest<WikiMainDataVo> request) {
        QueryRequest query = request.getQuery() == null ? new QueryRequest() : request.getQuery();
        boolean needPage = query.getNeedPage() == null || query.getNeedPage();
        int pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        int pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();

        QueryCondition root = query.getData();
        QueryConditionBuilder.Built built = QueryConditionBuilder.build(root, WikiMainDataFieldMaps.MAIN_DATA);
        String orderBy = SqlSortBuilder.buildOrderBy(query.getSortField(), query.getSortOrder(),
                WikiMainDataFieldMaps.MAIN_DATA);
        if (orderBy == null) {
            orderBy = "ORDER BY m.field_id DESC";
        }

        long total = wikiMainDataDao.countByCondition(built.getWhereSql(), built.getParams());
        long offset = needPage ? (long) (pageNum - 1) * pageSize : 0;
        List<WikiMainDataDo> records = wikiMainDataDao.selectByCondition(
                built.getWhereSql(), orderBy, offset, pageSize, needPage, built.getParams());
        List<WikiMainDataVo> vos = new ArrayList<>(records.size());
        for (WikiMainDataDo record : records) {
            vos.add(toVo(record));
        }
        return new ListResult<>(vos, QueryResponse.of(total, pageNum, pageSize));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String save(WikiMainDataSaveRequest request) {
        validateSimpleName(request.getFieldDataName());
        validateDataType(request.getFieldDataType());
        requireMainId(request.getFieldMainId());
        WikiMainDo wikiMain = wikiMainDao.selectById(request.getFieldMainId());
        if (wikiMain == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 项目不存在");
        }
        WikiMainDataDo exists = wikiMainDataDao.selectByMainIdAndDataName(
                request.getFieldMainId(), request.getFieldDataName());
        if (exists != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该项目下数据项简称已存在：" + request.getFieldDataName());
        }

        List<WikiDataDetailDo> details = normalizeDetailsForSave(request);
        String dataJson = JsonUtil.toJson(details);
        String fieldId = IDUtil.initID(request.getFieldId());
        WikiMainDataDo wikiMainData = new WikiMainDataDo();
        wikiMainData.setFieldId(fieldId);
        wikiMainData.setFieldMainId(request.getFieldMainId());
        wikiMainData.setFieldName(request.getFieldName());
        wikiMainData.setFieldDataName(request.getFieldDataName());
        wikiMainData.setFieldDataType(request.getFieldDataType());
        wikiMainData.setFieldDataJson(dataJson);

        // 先插入元数据，再建表；DDL 失败补偿删除元数据（MySQL DDL 隐式提交无法回滚）
        wikiMainDataDao.insert(wikiMainData);
        try {
            createDynamicTable(wikiMain, wikiMainData);
        } catch (Exception e) {
            log.error("[wiki] 数据项建表失败，补偿删除元数据 fieldId={}", fieldId, e);
            wikiMainDataDao.deleteById(fieldId);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "动态表创建失败：" + e.getMessage(), e);
        }
        return fieldId;
    }

    @Override
    public WikiMainDataVo load(String fieldId) {
        WikiMainDataDo wikiMainData = wikiMainDataDao.selectById(fieldId);
        if (wikiMainData == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 数据项不存在");
        }
        return toVo(wikiMainData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WikiMainDataSaveRequest request) {
        WikiMainDataDo exists = wikiMainDataDao.selectById(request.getFieldId());
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 数据项不存在");
        }
        if (!exists.getFieldDataName().equals(request.getFieldDataName())
                || !exists.getFieldDataType().equals(request.getFieldDataType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据项简称与类型不可修改");
        }
        WikiMainDo wikiMain = wikiMainDao.selectById(exists.getFieldMainId());
        if (wikiMain == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 项目不存在");
        }
        List<WikiDataDetailDo> oldDetails = DynamicTableSqlBuilder.parseDetails(exists.getFieldDataJson());
        List<WikiDataDetailDo> newDetails = normalizeDetailsForUpdate(request, exists.getFieldDataType());
        validateIncrementalColumns(oldDetails, newDetails);

        WikiMainDataDo update = new WikiMainDataDo();
        update.setFieldId(exists.getFieldId());
        update.setFieldName(request.getFieldName());
        update.setFieldDataJson(JsonUtil.toJson(newDetails));
        wikiMainDataDao.update(update);
        alterTableAddColumns(wikiMain, exists, oldDetails, newDetails);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String fieldId) {
        WikiMainDataDo wikiMainData = wikiMainDataDao.selectById(fieldId);
        if (wikiMainData == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 数据项不存在");
        }
        checkReferencedBeforeDelete(wikiMainData);
        WikiMainDo wikiMain = wikiMainDao.selectById(wikiMainData.getFieldMainId());
        if (wikiMain == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 项目不存在");
        }
        String tableName = DynamicTableSqlBuilder.buildTableName(wikiMain, wikiMainData);
        boolean tableExists = wikiDynamicDataDao.tableExists(tableName);
        wikiMainDataDao.deleteById(fieldId);
        if (tableExists) {
            try {
                wikiDynamicDataDao.executeDdl(DynamicTableSqlBuilder.buildDropTable(tableName));
            } catch (Exception e) {
                log.error("[wiki] 动态表删表失败 tableName={}", tableName, e);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "动态表删除失败：" + e.getMessage(), e);
            }
        }
    }

    @Override
    public Map<String, Object> init(String fieldMainId) {
        List<WikiMainDataDo> all = wikiMainDataDao.selectByMainId(fieldMainId);
        List<Map<String, Object>> joinSources = new ArrayList<>();
        for (WikiMainDataDo item : all) {
            if (WikiConstants.DATA_TYPE_DATA.equals(item.getFieldDataType())
                    || WikiConstants.DATA_TYPE_DOC.equals(item.getFieldDataType())) {
                Map<String, Object> source = new LinkedHashMap<>();
                source.put("fieldId", item.getFieldId());
                source.put("fieldName", item.getFieldName());
                source.put("fieldDataName", item.getFieldDataName());
                source.put("fieldDataType", item.getFieldDataType());
                joinSources.add(source);
            }
        }
        Map<String, Object> dataTypes = new LinkedHashMap<>();
        dataTypes.put("data", "图鉴类");
        dataTypes.put("join", "关联项");
        dataTypes.put("doc", "文档类");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataTypes", dataTypes);
        result.put("joinSources", joinSources);
        return result;
    }

    // ===== 内部：动态建表与列变更 =====

    private void createDynamicTable(WikiMainDo wikiMain, WikiMainDataDo wikiMainData) {
        String tableName = DynamicTableSqlBuilder.buildTableName(wikiMain, wikiMainData);
        if (wikiDynamicDataDao.tableExists(tableName)) {
            throw new BusinessException(ErrorCode.CONFLICT, "数据项表已存在：" + tableName);
        }
        wikiDynamicDataDao.executeDdl(DynamicTableSqlBuilder.buildCreateTable(wikiMain, wikiMainData));
        log.info("[wiki] 动态表创建成功 tableName={}", tableName);
    }

    private void alterTableAddColumns(WikiMainDo wikiMain, WikiMainDataDo wikiMainData,
                                      List<WikiDataDetailDo> oldDetails, List<WikiDataDetailDo> newDetails) {
        if (WikiConstants.DATA_TYPE_DOC.equals(wikiMainData.getFieldDataType())) {
            return;
        }
        Set<String> oldNames = new HashSet<>();
        for (WikiDataDetailDo d : oldDetails) {
            oldNames.add(d.getDataName());
        }
        String tableName = DynamicTableSqlBuilder.buildTableName(wikiMain, wikiMainData);
        for (WikiDataDetailDo detail : newDetails) {
            if (!oldNames.contains(detail.getDataName())) {
                wikiDynamicDataDao.executeDdl(DynamicTableSqlBuilder.buildAddColumn(tableName, detail));
                log.info("[wiki] 动态表新增列 tableName={} column={}", tableName, detail.getDataName());
            }
        }
    }

    // ===== 内部：校验 =====

    private void validateSimpleName(String dataName) {
        if (dataName == null || dataName.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据项简称不能为空");
        }
        if (!SIMPLE_NAME_PATTERN.matcher(dataName).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据项简称仅支持小写英文和数字");
        }
    }

    private void validateDataType(String dataType) {
        if (!WikiConstants.DATA_TYPE_DATA.equals(dataType)
                && !WikiConstants.DATA_TYPE_JOIN.equals(dataType)
                && !WikiConstants.DATA_TYPE_DOC.equals(dataType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的数据项类型：" + dataType);
        }
    }

    private void requireMainId(String fieldMainId) {
        if (fieldMainId == null || fieldMainId.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "所属项目ID不能为空");
        }
    }

    private void validateIncrementalColumns(List<WikiDataDetailDo> oldDetails,
                                            List<WikiDataDetailDo> newDetails) {
        Map<String, WikiDataDetailDo> oldMap = new LinkedHashMap<>();
        for (WikiDataDetailDo d : oldDetails) {
            oldMap.put(d.getDataName(), d);
        }
        for (WikiDataDetailDo d : newDetails) {
            WikiDataDetailDo old = oldMap.get(d.getDataName());
            if (old != null && (!equalsStr(old.getType(), d.getType())
                    || !equalsStr(old.getJoin(), d.getJoin()))) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "已存在明细列不可修改：" + d.getDataName());
            }
        }
        if (newDetails.size() < oldDetails.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不允许删除已存在的明细列");
        }
    }

    private void checkReferencedBeforeDelete(WikiMainDataDo wikiMainData) {
        List<WikiMainDataDo> siblings = wikiMainDataDao.selectByMainId(wikiMainData.getFieldMainId());
        for (WikiMainDataDo sibling : siblings) {
            if (sibling.getFieldId().equals(wikiMainData.getFieldId())) {
                continue;
            }
            if (!WikiConstants.DATA_TYPE_JOIN.equals(sibling.getFieldDataType())) {
                continue;
            }
            for (WikiDataDetailDo detail : DynamicTableSqlBuilder.parseDetails(sibling.getFieldDataJson())) {
                if (WikiConstants.DATA_TYPE_JOIN.equals(detail.getType())
                        && wikiMainData.getFieldId().equals(detail.getJoin())) {
                    throw new BusinessException(ErrorCode.CONFLICT,
                            "该数据项被关联项引用，不可删除：" + sibling.getFieldName());
                }
            }
        }
    }

    // ===== 内部：明细归一化 =====

    private List<WikiDataDetailDo> normalizeDetailsForSave(WikiMainDataSaveRequest request) {
        List<WikiDataDetailDo> details = request.getFieldDataJson() == null
                ? new ArrayList<>() : new ArrayList<>(request.getFieldDataJson());
        if (WikiConstants.DATA_TYPE_DATA.equals(request.getFieldDataType())) {
            List<WikiDataDetailDo> normalized = new ArrayList<>();
            normalized.add(buildFixedDetail("名称", "name", "text"));
            normalized.add(buildFixedDetail("编号", "code", "text"));
            Set<String> fixedNames = Set.of("name", "code");
            for (WikiDataDetailDo d : details) {
                if (d.getDataName() != null && fixedNames.contains(d.getDataName())) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "图鉴类固定列 name/code 不可重复定义：" + d.getDataName());
                }
                normalized.add(d);
            }
            return normalized;
        }
        if (WikiConstants.DATA_TYPE_DOC.equals(request.getFieldDataType())) {
            return new ArrayList<>();
        }
        return details;
    }

    private List<WikiDataDetailDo> normalizeDetailsForUpdate(WikiMainDataSaveRequest request, String dataType) {
        List<WikiDataDetailDo> details = request.getFieldDataJson() == null
                ? new ArrayList<>() : new ArrayList<>(request.getFieldDataJson());
        if (WikiConstants.DATA_TYPE_DATA.equals(dataType)) {
            List<WikiDataDetailDo> normalized = new ArrayList<>();
            normalized.add(buildFixedDetail("名称", "name", "text"));
            normalized.add(buildFixedDetail("编号", "code", "text"));
            Set<String> fixedNames = Set.of("name", "code");
            for (WikiDataDetailDo d : details) {
                if (d.getDataName() != null && fixedNames.contains(d.getDataName())) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "图鉴类固定列 name/code 不可重复定义：" + d.getDataName());
                }
                normalized.add(d);
            }
            return normalized;
        }
        if (WikiConstants.DATA_TYPE_DOC.equals(dataType)) {
            return new ArrayList<>();
        }
        return details;
    }

    private WikiDataDetailDo buildFixedDetail(String name, String dataName, String type) {
        WikiDataDetailDo detail = new WikiDataDetailDo();
        detail.setName(name);
        detail.setDataName(dataName);
        detail.setType(type);
        return detail;
    }

    private WikiMainDataVo toVo(WikiMainDataDo wikiMainData) {
        WikiMainDataVo vo = new WikiMainDataVo();
        vo.setFieldId(wikiMainData.getFieldId());
        vo.setFieldMainId(wikiMainData.getFieldMainId());
        vo.setFieldName(wikiMainData.getFieldName());
        vo.setFieldDataName(wikiMainData.getFieldDataName());
        vo.setFieldDataType(wikiMainData.getFieldDataType());
        vo.setFieldDataJson(DynamicTableSqlBuilder.parseDetails(wikiMainData.getFieldDataJson()));
        return vo;
    }

    private boolean equalsStr(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
