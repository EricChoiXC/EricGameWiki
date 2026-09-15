package com.wiki.admin.wiki.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wiki.admin.wiki.dao.IWikiMainDataDao;
import com.wiki.admin.wiki.dao.IWikiMainDao;
import com.wiki.admin.wiki.dao.dynamic.WikiDynamicDataDao;
import com.wiki.admin.wiki.model.dto.ImportResult;
import com.wiki.admin.wiki.model.dto.WikiDataDetailDo;
import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.admin.wiki.model.request.WikiDataRequest;
import com.wiki.admin.wiki.model.response.WikiDataVo;
import com.wiki.admin.wiki.service.IWikiCRPService;
import com.wiki.admin.wiki.service.IWikiDataService;
import com.wiki.admin.wiki.util.DynamicFieldMaps;
import com.wiki.admin.wiki.util.DynamicTableSqlBuilder;
import com.wiki.admin.wiki.util.ImportExportProcessor;
import com.wiki.admin.wiki.util.WikiConstants;
import com.wiki.common.constant.CommonConstants;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * wiki 数据明细 Service 实现，对应 {@code docs/admin/wiki/wiki业务逻辑.md} 6.3（FLOW-W003）。
 * <p>
 * 以 {@code fieldDataId} 为入口加载数据项元数据，按 {@code fieldDataType} 分发：
 * <ul>
 *   <li>图鉴类（data）/文档类（doc）：固定列 + 动态列，默认 fieldCode 降序</li>
 *   <li>关联项（join）：动态 join 列联表查询（技术方案 4.5），关联数据模糊筛选，默认 fieldId 降序</li>
 * </ul>
 * 动态表数据通过 {@link WikiDynamicDataDao} 通用动态查询层访问（技术方案 ARCH-W02）；
 * 动态列值经元数据白名单落列，附件类明细（无物理列）写入 {@code field_data} json。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiDataServiceImpl implements IWikiDataService {

    /** 记录 id 命名参数占位 */
    private static final String ROW_ID_PARAM = "id";

    private final IWikiMainDataDao wikiMainDataDao;
    private final IWikiMainDao wikiMainDao;
    private final WikiDynamicDataDao wikiDynamicDataDao;
    private final ImportExportProcessor importExportProcessor;
    private final IWikiCRPService wikiCRPService;

    @Override
    public ListResult<WikiDataVo> list(ApiRequest<WikiDataRequest> request) {
        WikiDataRequest data = request.getData();
        if (data == null || data.getFieldDataId() == null || data.getFieldDataId().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据项ID不能为空");
        }
        WikiMainDataDo meta = loadMeta(data.getFieldDataId());
        WikiMainDo wikiMain = loadMain(meta.getFieldMainId());
        QueryRequest query = request.getQuery() == null ? new QueryRequest() : request.getQuery();
        String tableName = DynamicTableSqlBuilder.buildTableName(wikiMain, meta);
        ensureTableExists(tableName);

        return switch (meta.getFieldDataType()) {
            case WikiConstants.DATA_TYPE_DATA, WikiConstants.DATA_TYPE_DOC ->
                    queryList(meta, tableName, query, DynamicFieldMaps.build(meta), null, null,
                            "ORDER BY field_code DESC", false);
            case WikiConstants.DATA_TYPE_JOIN -> queryJoinList(wikiMain, meta, tableName, query);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不支持的数据项类型：" + meta.getFieldDataType());
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String save(WikiDataRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求数据不能为空");
        }
        WikiMainDataDo meta = loadMeta(request.getFieldDataId());
        WikiMainDo wikiMain = loadMain(meta.getFieldMainId());
        String tableName = DynamicTableSqlBuilder.buildTableName(wikiMain, meta);
        ensureTableExists(tableName);

        Map<String, Object> row = new LinkedHashMap<>();
        String id = IDUtil.initID(request.getFieldId());
        row.put("field_id", id);
        fillColumns(row, wikiMain, meta, request);
        wikiDynamicDataDao.insert(tableName, row);
        return id;
    }

    @Override
    public WikiDataVo load(String fieldId, String fieldDataId) {
        WikiMainDataDo meta = loadMeta(fieldDataId);
        WikiMainDo wikiMain = loadMain(meta.getFieldMainId());
        String tableName = DynamicTableSqlBuilder.buildTableName(wikiMain, meta);
        ensureTableExists(tableName);

        if (WikiConstants.DATA_TYPE_JOIN.equals(meta.getFieldDataType())) {
            return loadJoinRecord(wikiMain, meta, tableName, fieldId);
        }
        List<Map<String, Object>> rows = wikiDynamicDataDao.selectByCondition(
                tableName, null, null, "field_id = #{id}", null, 0, 1, false,
                Map.of(ROW_ID_PARAM, fieldId));
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据明细不存在");
        }
        return toFixedVo(rows.get(0), meta);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WikiDataRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求数据不能为空");
        }
        if (request.getFieldId() == null || request.getFieldId().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "明细记录ID不能为空");
        }
        WikiMainDataDo meta = loadMeta(request.getFieldDataId());
        WikiMainDo wikiMain = loadMain(meta.getFieldMainId());
        String tableName = DynamicTableSqlBuilder.buildTableName(wikiMain, meta);
        ensureTableExists(tableName);
        ensureRecordExists(tableName, request.getFieldId());

        Map<String, Object> row = new LinkedHashMap<>();
        fillColumns(row, wikiMain, meta, request);
        wikiDynamicDataDao.update(tableName, request.getFieldId(), row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String fieldId, String fieldDataId) {
        WikiMainDataDo meta = loadMeta(fieldDataId);
        WikiMainDo wikiMain = loadMain(meta.getFieldMainId());
        String tableName = DynamicTableSqlBuilder.buildTableName(wikiMain, meta);
        ensureTableExists(tableName);
        ensureRecordExists(tableName, fieldId);
        wikiDynamicDataDao.deleteById(tableName, fieldId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(String fieldDataId, List<String> fieldIds) {
        WikiMainDataDo meta = loadMeta(fieldDataId);
        if (!WikiConstants.DATA_TYPE_JOIN.equals(meta.getFieldDataType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅关联项数据项支持批量删除");
        }
        if (fieldIds == null || fieldIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "待删除记录不能为空");
        }
        WikiMainDo wikiMain = loadMain(meta.getFieldMainId());
        String tableName = DynamicTableSqlBuilder.buildTableName(wikiMain, meta);
        ensureTableExists(tableName);
        wikiDynamicDataDao.batchDeleteByIds(tableName, fieldIds);
    }

    @Override
    public byte[] template(String fieldDataId) {
        WikiMainDataDo meta = loadMeta(fieldDataId);
        WikiMainDo wikiMain = loadMain(meta.getFieldMainId());
        return importExportProcessor.buildTemplate(meta);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importData(WikiDataRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求数据不能为空");
        }
        WikiMainDataDo meta = loadMeta(request.getFieldDataId());
        WikiMainDo wikiMain = loadMain(meta.getFieldMainId());
        // 跨模块调用经 CRPService 读取附件物理文件路径，禁止直接访问 admin_attachment 表
        String filePath = wikiCRPService.loadAttachmentFilePath(request.getFieldAttachmentId());
        boolean skipFail = Boolean.TRUE.equals(request.getSkipFail());
        boolean skipError = Boolean.TRUE.equals(request.getSkipError());
        return importExportProcessor.importRows(wikiMain, meta, filePath, skipFail, skipError);
    }

    @Override
    public byte[] export(String fieldDataId) {
        WikiMainDataDo meta = loadMeta(fieldDataId);
        WikiMainDo wikiMain = loadMain(meta.getFieldMainId());
        return importExportProcessor.export(wikiMain, meta);
    }

    // ===== 内部：三类型列表查询 =====

    /**
     * 通用列表查询：按动态白名单构建查询条件与排序，映射为 VO。
     * <p>
     * 图鉴类/文档类（无联表）以及无关联列的关联项使用本方法；关联项联表走 {@link #queryJoinList}。
     *
     * @param meta          数据项元数据
     * @param tableName     动态表名
     * @param query         分页与查询条件
     * @param fieldMap      字段白名单（Java 属性名 → 列名）
     * @param joinClauses   LEFT JOIN 片段（无联表时 null/空）
     * @param columns       SELECT 列片段（null 时查询全部列）
     * @param defaultOrderBy 默认排序
     * @param isJoin        VO 映射是否为关联项形态
     */
    private ListResult<WikiDataVo> queryList(WikiMainDataDo meta, String tableName, QueryRequest query,
                                             Map<String, String> fieldMap, List<String> joinClauses,
                                             String columns, String defaultOrderBy, boolean isJoin) {
        QueryConditionBuilder.Built built = QueryConditionBuilder.build(query.getData(), fieldMap);
        String orderBy = SqlSortBuilder.buildOrderBy(query.getSortField(), query.getSortOrder(), fieldMap);
        if (orderBy == null) {
            orderBy = defaultOrderBy;
        }
        boolean needPage = query.getNeedPage() == null || query.getNeedPage();
        int pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        int pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        long offset = needPage ? (long) (pageNum - 1) * pageSize : 0;

        long total = wikiDynamicDataDao.countByCondition(tableName, joinClauses, built.getWhereSql(),
                built.getParams());
        List<Map<String, Object>> rows = wikiDynamicDataDao.selectByCondition(
                tableName, columns, joinClauses, built.getWhereSql(), orderBy, offset, pageSize, needPage,
                built.getParams());
        return toListResult(rows, meta, isJoin, total, pageNum, pageSize);
    }

    /**
     * 关联项列表查询：构建联表查询上下文（技术方案 4.5），默认 fieldId 降序。
     * <p>
     * 无关联列时退化为普通查询，避免 {@code base.} 前缀无别名报错。
     */
    private ListResult<WikiDataVo> queryJoinList(WikiMainDo wikiMain, WikiMainDataDo meta,
                                                 String tableName, QueryRequest query) {
        List<WikiMainDataDo> targets = wikiMainDataDao.selectByMainId(meta.getFieldMainId());
        DynamicFieldMaps.JoinQuery joinQuery = DynamicFieldMaps.buildJoinQuery(wikiMain, meta, targets);
        if (joinQuery.getJoinClauses().isEmpty()) {
            return queryList(meta, tableName, query, DynamicFieldMaps.build(meta), null, null,
                    "ORDER BY field_id DESC", true);
        }
        return queryList(meta, tableName, query, joinQuery.getFieldColumnMap(), joinQuery.getJoinClauses(),
                joinQuery.getColumnsSql(), "ORDER BY base.field_id DESC", true);
    }

    /**
     * 关联项单条加载：联表查询返回关联目标显示值。
     */
    private WikiDataVo loadJoinRecord(WikiMainDo wikiMain, WikiMainDataDo meta, String tableName, String fieldId) {
        List<WikiMainDataDo> targets = wikiMainDataDao.selectByMainId(meta.getFieldMainId());
        DynamicFieldMaps.JoinQuery joinQuery = DynamicFieldMaps.buildJoinQuery(wikiMain, meta, targets);
        List<Map<String, Object>> rows;
        if (joinQuery.getJoinClauses().isEmpty()) {
            rows = wikiDynamicDataDao.selectByCondition(tableName, null, null,
                    "field_id = #{id}", null, 0, 1, false, Map.of(ROW_ID_PARAM, fieldId));
        } else {
            rows = wikiDynamicDataDao.selectByCondition(tableName, joinQuery.getColumnsSql(),
                    joinQuery.getJoinClauses(), "base.field_id = #{id}", null, 0, 1, false,
                    Map.of(ROW_ID_PARAM, fieldId));
        }
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据明细不存在");
        }
        return toJoinVo(rows.get(0), meta);
    }

    // ===== 内部：行落列 =====

    /**
     * 按类型分发填充行列，附件类明细写入 {@code field_data} json。
     */
    private void fillColumns(Map<String, Object> row, WikiMainDo wikiMain, WikiMainDataDo meta,
                             WikiDataRequest request) {
        switch (meta.getFieldDataType()) {
            case WikiConstants.DATA_TYPE_DATA -> fillDataColumns(row, wikiMain, meta, request);
            case WikiConstants.DATA_TYPE_JOIN -> fillDynamicColumns(row, wikiMain, meta, request);
            case WikiConstants.DATA_TYPE_DOC -> fillDocColumns(row, request);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不支持的数据项类型：" + meta.getFieldDataType());
        }
        String fieldDataJson = buildFieldDataJson(meta, request.getFieldData());
        if (fieldDataJson != null) {
            row.put("field_data", fieldDataJson);
        }
    }

    /**
     * 图鉴类列填充：固定 name/code + 动态列。
     */
    private void fillDataColumns(Map<String, Object> row, WikiMainDo wikiMain, WikiMainDataDo meta,
                                 WikiDataRequest request) {
        requireValue(request.getFieldName(), "名称");
        requireValue(request.getFieldCode(), "编号");
        row.put("field_name", request.getFieldName());
        row.put("field_code", request.getFieldCode());
        fillDynamicColumns(row, wikiMain, meta, request);
    }

    /**
     * 文档类列填充：固定 title/code/context。
     */
    private void fillDocColumns(Map<String, Object> row, WikiDataRequest request) {
        requireValue(request.getFieldName(), "标题");
        requireValue(request.getFieldCode(), "编号");
        row.put("field_name", request.getFieldName());
        row.put("field_code", request.getFieldCode());
        row.put("field_context", request.getFieldContext());
    }

    /**
     * 动态列填充：按元数据明细逐列写入，type=join 生成 {@code field_${dataName}_id} 并校验关联数据存在性；
     * attachment 无物理列跳过（写入 {@code field_data} json）。
     */
    private void fillDynamicColumns(Map<String, Object> row, WikiMainDo wikiMain, WikiMainDataDo meta,
                                    WikiDataRequest request) {
        Map<String, Object> fieldData = request.getFieldData();
        for (WikiDataDetailDo detail : DynamicTableSqlBuilder.parseDetails(meta.getFieldDataJson())) {
            String dataName = detail.getDataName();
            String type = detail.getType();
            if ("name".equals(dataName) || "code".equals(dataName) || "attachment".equals(type)) {
                continue;
            }
            String key = "field" + upperFirst(dataName);
            if (WikiConstants.DATA_TYPE_JOIN.equals(type)) {
                String joinKey = key + "Id";
                String value = strValue(fieldData, joinKey);
                validateJoinValue(wikiMain, detail, value);
                row.put("field_" + dataName + "_id", value);
            } else {
                row.put("field_" + dataName, fieldData == null ? null : fieldData.get(key));
            }
        }
    }

    /**
     * 附件类明细值收集为 {@code field_data} json（key 为小驼峰属性名）。
     */
    private String buildFieldDataJson(WikiMainDataDo meta, Map<String, Object> fieldData) {
        Map<String, Object> attachments = new LinkedHashMap<>();
        for (WikiDataDetailDo detail : DynamicTableSqlBuilder.parseDetails(meta.getFieldDataJson())) {
            if (!"attachment".equals(detail.getType())) {
                continue;
            }
            String key = "field" + upperFirst(detail.getDataName());
            Object value = fieldData == null ? null : fieldData.get(key);
            if (value != null) {
                attachments.put(key, value);
            }
        }
        return attachments.isEmpty() ? null : JsonUtil.toJson(attachments);
    }

    // ===== 内部：行 → VO =====

    private ListResult<WikiDataVo> toListResult(List<Map<String, Object>> rows, WikiMainDataDo meta,
                                                boolean isJoin, long total, int pageNum, int pageSize) {
        List<WikiDataVo> vos = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            vos.add(isJoin ? toJoinVo(row, meta) : toFixedVo(row, meta));
        }
        return new ListResult<>(vos, QueryResponse.of(total, pageNum, pageSize));
    }

    /**
     * 图鉴类/文档类 VO 映射：固定列强类型字段 + 动态列 Map。
     */
    private WikiDataVo toFixedVo(Map<String, Object> row, WikiMainDataDo meta) {
        WikiDataVo vo = new WikiDataVo();
        vo.setFieldId(asString(cell(row, "field_id")));
        vo.setFieldName(asString(cell(row, "field_name")));
        vo.setFieldCode(asString(cell(row, "field_code")));
        if (WikiConstants.DATA_TYPE_DOC.equals(meta.getFieldDataType())) {
            vo.setFieldContext(toDisplayString(cell(row, "field_context")));
        }
        vo.setFieldData(buildFieldDataMap(row, meta, false));
        return vo;
    }

    /**
     * 关联项 VO 映射：动态列 Map（含关联目标显示值 {@code field${X}Name}/{@code field${X}Code}）。
     */
    private WikiDataVo toJoinVo(Map<String, Object> row, WikiMainDataDo meta) {
        WikiDataVo vo = new WikiDataVo();
        vo.setFieldId(asString(cell(row, "field_id")));
        vo.setFieldData(buildFieldDataMap(row, meta, true));
        return vo;
    }

    /**
     * 构建动态列值 Map：跳过固定列（name/code）与附件类；
     * type=join 生成 {@code field${X}Id}，联表时追加 {@code field${X}Name}/{@code field${X}Code} 显示值；
     * 附件类明细从 {@code field_data} json 合并。
     */
    private Map<String, Object> buildFieldDataMap(Map<String, Object> row, WikiMainDataDo meta,
                                                  boolean joinMode) {
        Map<String, Object> fieldData = new LinkedHashMap<>();
        for (WikiDataDetailDo detail : DynamicTableSqlBuilder.parseDetails(meta.getFieldDataJson())) {
            String dataName = detail.getDataName();
            String type = detail.getType();
            if ("name".equals(dataName) || "code".equals(dataName) || "attachment".equals(type)) {
                continue;
            }
            String prefix = "field" + upperFirst(dataName);
            if (WikiConstants.DATA_TYPE_JOIN.equals(type)) {
                fieldData.put(prefix + "Id", toDisplayValue(cell(row, "field_" + dataName + "_id")));
                if (joinMode) {
                    fieldData.put(prefix + "Name", toDisplayValue(cell(row, dataName + "_name")));
                    fieldData.put(prefix + "Code", toDisplayValue(cell(row, dataName + "_code")));
                }
            } else {
                fieldData.put(prefix, toDisplayValue(cell(row, "field_" + dataName)));
            }
        }
        mergeAttachments(fieldData, cell(row, "field_data"));
        return fieldData.isEmpty() ? null : fieldData;
    }

    /**
     * 合并 {@code field_data} json（附件等非物理列数据）到动态列 Map。
     */
    private void mergeAttachments(Map<String, Object> fieldData, Object rawFieldDataJson) {
        if (rawFieldDataJson == null) {
            return;
        }
        if (rawFieldDataJson instanceof Map<?, ?> map) {
            map.forEach((k, v) -> fieldData.put(String.valueOf(k), v));
            return;
        }
        String json = rawFieldDataJson instanceof String ? (String) rawFieldDataJson
                : String.valueOf(rawFieldDataJson);
        Map<String, Object> attachments = JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {
        });
        if (attachments != null) {
            fieldData.putAll(attachments);
        }
    }

    // ===== 内部：校验 =====

    private WikiMainDataDo loadMeta(String fieldDataId) {
        if (fieldDataId == null || fieldDataId.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据项ID不能为空");
        }
        WikiMainDataDo meta = wikiMainDataDao.selectById(fieldDataId);
        if (meta == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 数据项不存在");
        }
        return meta;
    }

    private WikiMainDo loadMain(String fieldMainId) {
        WikiMainDo wikiMain = wikiMainDao.selectById(fieldMainId);
        if (wikiMain == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 项目不存在");
        }
        return wikiMain;
    }

    private void ensureTableExists(String tableName) {
        if (!wikiDynamicDataDao.tableExists(tableName)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据项表不存在：" + tableName);
        }
    }

    private void ensureRecordExists(String tableName, String id) {
        long count = wikiDynamicDataDao.countByCondition(tableName, null,
                "field_id = #{id}", Map.of(ROW_ID_PARAM, id));
        if (count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据明细不存在：" + id);
        }
    }

    /**
     * 关联列值校验：目标动态表记录存在性（值为空时跳过）。
     */
    private void validateJoinValue(WikiMainDo wikiMain, WikiDataDetailDo detail, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        WikiMainDataDo target = wikiMainDataDao.selectById(detail.getJoin());
        if (target == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "关联目标数据项不存在");
        }
        String targetTable = DynamicTableSqlBuilder.buildTableName(wikiMain, target);
        if (!wikiDynamicDataDao.tableExists(targetTable)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "关联目标数据项表不存在：" + targetTable);
        }
        long count = wikiDynamicDataDao.countByCondition(targetTable, null,
                "field_id = #{id}", Map.of(ROW_ID_PARAM, value));
        if (count == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "关联数据不存在：" + value);
        }
    }

    private void requireValue(String value, String label) {
        if (value == null || value.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不能为空");
        }
    }

    private String strValue(Map<String, Object> fieldData, String key) {
        if (fieldData == null) {
            return null;
        }
        Object value = fieldData.get(key);
        return value == null ? null : String.valueOf(value);
    }

    // ===== 内部：行取值（兼容 map-underscore-to-camel-case 的驼峰键） =====

    /**
     * 从行 Map 取值：优先精确匹配，未命中时回退到驼峰转换后的键。
     * 兼容 MyBatis {@code map-underscore-to-camel-case} 对 Map 键的影响。
     */
    private Object cell(Map<String, Object> row, String column) {
        if (row == null) {
            return null;
        }
        Object value = row.get(column);
        if (value != null || row.containsKey(column)) {
            return value;
        }
        return row.get(camelize(column));
    }

    private String camelize(String column) {
        StringBuilder sb = new StringBuilder(column.length());
        boolean upper = false;
        for (char c : column.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * blob/longblob 等字节列转为字符串显示值。
     */
    private Object toDisplayValue(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return value;
    }

    private String toDisplayString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }

    private String upperFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
