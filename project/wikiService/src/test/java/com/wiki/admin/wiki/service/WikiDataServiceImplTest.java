package com.wiki.admin.wiki.service;

import com.wiki.admin.wiki.dao.IWikiMainDataDao;
import com.wiki.admin.wiki.dao.IWikiMainDao;
import com.wiki.admin.wiki.dao.dynamic.WikiDynamicDataDao;
import com.wiki.admin.wiki.model.dto.ImportResult;
import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.admin.wiki.model.request.WikiDataRequest;
import com.wiki.admin.wiki.model.response.WikiDataVo;
import com.wiki.admin.wiki.service.impl.WikiDataServiceImpl;
import com.wiki.admin.wiki.util.ImportExportProcessor;
import com.wiki.admin.wiki.util.WikiConstants;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.query.LogicalOperator;
import com.wiki.common.model.query.QueryCondition;
import com.wiki.common.model.query.QueryOperator;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.request.QueryRequest;
import com.wiki.common.model.response.ListResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link WikiDataServiceImpl} 三类型分发 CRUD（TASK-W2-02）：
 * 图鉴类/文档类固定 fieldCode 降序、关联项联表查询 fieldId 降序、
 * 名称/编号模糊筛选、批量删除限制、关联数据存在性校验等。
 */
class WikiDataServiceImplTest {

    private IWikiMainDataDao wikiMainDataDao;
    private IWikiMainDao wikiMainDao;
    private WikiDynamicDataDao wikiDynamicDataDao;
    private ImportExportProcessor importExportProcessor;
    private IWikiCRPService wikiCRPService;
    private WikiDataServiceImpl service;

    @BeforeEach
    void setUp() {
        wikiMainDataDao = mock(IWikiMainDataDao.class);
        wikiMainDao = mock(IWikiMainDao.class);
        wikiDynamicDataDao = mock(WikiDynamicDataDao.class);
        importExportProcessor = mock(ImportExportProcessor.class);
        wikiCRPService = mock(IWikiCRPService.class);
        service = new WikiDataServiceImpl(wikiMainDataDao, wikiMainDao, wikiDynamicDataDao,
                importExportProcessor, wikiCRPService);
    }

    // ===== 测试数据构建 =====

    private WikiMainDo main() {
        WikiMainDo main = new WikiMainDo();
        main.setFieldId("mainId");
        main.setFieldSimpleName("re9");
        return main;
    }

    private WikiMainDataDo dataItem(String id, String dataName, String type, String dataJson) {
        WikiMainDataDo data = new WikiMainDataDo();
        data.setFieldId(id);
        data.setFieldMainId("mainId");
        data.setFieldDataName(dataName);
        data.setFieldDataType(type);
        data.setFieldDataJson(dataJson);
        return data;
    }

    private ApiRequest<WikiDataRequest> apiRequestForList(String fieldDataId) {
        ApiRequest<WikiDataRequest> request = new ApiRequest<>();
        WikiDataRequest data = new WikiDataRequest();
        data.setFieldDataId(fieldDataId);
        request.setData(data);
        QueryRequest query = new QueryRequest();
        query.setPageNum(1);
        query.setPageSize(15);
        query.setNeedPage(true);
        request.setQuery(query);
        return request;
    }

    private Map<String, Object> row(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    // ===== 图鉴类列表 =====

    @Test
    void listDataSortsByFieldCodeDescAndMapsVo() {
        WikiMainDataDo meta = dataItem("dataId", "item", WikiConstants.DATA_TYPE_DATA,
                """
                [{"name": "价格", "dataName": "price", "type": "number"}]
                """);
        when(wikiMainDataDao.selectById("dataId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);
        when(wikiDynamicDataDao.countByCondition(eq("wiki_re9_item"), isNull(), isNull(), anyMap()))
                .thenReturn(1L);
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_item"), isNull(), isNull(), isNull(),
                eq("ORDER BY field_code DESC"), eq(0L), eq(15), eq(true), anyMap()))
                .thenReturn(List.of(row("field_id", "rec1", "field_name", "长剑",
                        "field_code", "sword001", "field_price", new BigDecimal("100.0000"))));

        ListResult<WikiDataVo> result = service.list(apiRequestForList("dataId"));

        assertEquals(1, result.getRecords().size());
        WikiDataVo vo = result.getRecords().get(0);
        assertEquals("rec1", vo.getFieldId());
        assertEquals("长剑", vo.getFieldName());
        assertEquals("sword001", vo.getFieldCode());
        assertEquals(new BigDecimal("100.0000"), vo.getFieldData().get("fieldPrice"));
    }

    @Test
    void listDataAppliesNameCodeFuzzyFilter() {
        WikiMainDataDo meta = dataItem("dataId", "item", WikiConstants.DATA_TYPE_DATA,
                """
                [{"name": "价格", "dataName": "price", "type": "number"}]
                """);
        when(wikiMainDataDao.selectById("dataId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);
        when(wikiDynamicDataDao.countByCondition(eq("wiki_re9_item"), isNull(), any(), anyMap())).thenReturn(1L);
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_item"), isNull(), isNull(), any(),
                eq("ORDER BY field_code DESC"), eq(0L), eq(15), eq(true), anyMap()))
                .thenReturn(List.of(row("field_id", "rec1", "field_name", "长剑", "field_code", "sword001")));

        ApiRequest<WikiDataRequest> request = apiRequestForList("dataId");
        QueryCondition root = QueryCondition.group(LogicalOperator.OR, List.of(
                QueryCondition.leaf(QueryOperator.LIKE, "fieldName", "长剑"),
                QueryCondition.leaf(QueryOperator.LIKE, "fieldCode", "sword")));
        request.getQuery().setData(root);

        ListResult<WikiDataVo> result = service.list(request);

        assertEquals(1, result.getRecords().size());
        // 未在白名单中的字段拒绝
        request.getQuery().setData(QueryCondition.leaf(QueryOperator.LIKE, "fieldHack", "x"));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.list(request));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    // ===== 文档类列表 =====

    @Test
    void listDocSortsByFieldCodeDescAndReturnsContext() {
        WikiMainDataDo meta = dataItem("docId", "walkthrough", WikiConstants.DATA_TYPE_DOC, null);
        when(wikiMainDataDao.selectById("docId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiDynamicDataDao.tableExists("wiki_re9_walkthrough")).thenReturn(true);
        when(wikiDynamicDataDao.countByCondition(eq("wiki_re9_walkthrough"), isNull(), isNull(), anyMap()))
                .thenReturn(1L);
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_walkthrough"), isNull(), isNull(), isNull(),
                eq("ORDER BY field_code DESC"), eq(0L), eq(15), eq(true), anyMap()))
                .thenReturn(List.of(row("field_id", "d1", "field_name", "序章攻略",
                        "field_code", "wt001", "field_context", "欢迎来到最终幻想9")));

        ListResult<WikiDataVo> result = service.list(apiRequestForList("docId"));

        WikiDataVo vo = result.getRecords().get(0);
        assertEquals("d1", vo.getFieldId());
        assertEquals("序章攻略", vo.getFieldName());
        assertEquals("wt001", vo.getFieldCode());
        assertEquals("欢迎来到最终幻想9", vo.getFieldContext());
        assertNull(vo.getFieldData());
    }

    // ===== 关联项列表 =====

    @Test
    void listJoinBuildsJoinQueryAndSortsByIdDesc() {
        WikiMainDataDo meta = dataItem("joinId", "enemy_drop", WikiConstants.DATA_TYPE_JOIN,
                """
                [
                  {"name": "敌人", "dataName": "enemy", "type": "join", "join": "monsterId"},
                  {"name": "道具", "dataName": "item", "type": "join", "join": "itemId"},
                  {"name": "掉落概率", "dataName": "rate", "type": "number"}
                ]
                """);
        WikiMainDataDo monster = dataItem("monsterId", "monster", WikiConstants.DATA_TYPE_DATA, "[]");
        WikiMainDataDo item = dataItem("itemId", "item", WikiConstants.DATA_TYPE_DATA, "[]");
        when(wikiMainDataDao.selectById("joinId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiMainDataDao.selectByMainId("mainId")).thenReturn(List.of(monster, item, meta));
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiDynamicDataDao.countByCondition(eq("wiki_re9_enemy_drop"), anyList(), isNull(), anyMap()))
                .thenReturn(2L);
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_enemy_drop"), anyString(), anyList(), isNull(),
                eq("ORDER BY base.field_id DESC"), eq(0L), eq(15), eq(true), anyMap()))
                .thenReturn(List.of(row(
                        "field_id", "rec1",
                        "field_enemy_id", "m1", "enemy_name", "史莱姆", "enemy_code", "slime001",
                        "field_item_id", "i1", "item_name", "药水", "item_code", "potion001",
                        "field_rate", new BigDecimal("0.5000"))));

        ListResult<WikiDataVo> result = service.list(apiRequestForList("joinId"));

        assertEquals(2L, result.getQuery().getTotal());
        WikiDataVo vo = result.getRecords().get(0);
        assertEquals("rec1", vo.getFieldId());
        assertNull(vo.getFieldName());
        Map<String, Object> fieldData = vo.getFieldData();
        assertEquals("m1", fieldData.get("fieldEnemyId"));
        assertEquals("史莱姆", fieldData.get("fieldEnemyName"));
        assertEquals("slime001", fieldData.get("fieldEnemyCode"));
        assertEquals("i1", fieldData.get("fieldItemId"));
        assertEquals(new BigDecimal("0.5000"), fieldData.get("fieldRate"));
    }

    @Test
    void listJoinWithoutJoinDetailsQueriesWithoutAlias() {
        WikiMainDataDo meta = dataItem("plainJoin", "log", WikiConstants.DATA_TYPE_JOIN, "[]");
        when(wikiMainDataDao.selectById("plainJoin")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiMainDataDao.selectByMainId("mainId")).thenReturn(List.of(meta));
        when(wikiDynamicDataDao.tableExists("wiki_re9_log")).thenReturn(true);
        when(wikiDynamicDataDao.countByCondition(eq("wiki_re9_log"), isNull(), isNull(), anyMap())).thenReturn(1L);
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_log"), isNull(), isNull(), isNull(),
                eq("ORDER BY field_id DESC"), eq(0L), eq(15), eq(true), anyMap()))
                .thenReturn(List.of(row("field_id", "log1")));

        ListResult<WikiDataVo> result = service.list(apiRequestForList("plainJoin"));

        assertEquals("log1", result.getRecords().get(0).getFieldId());
    }

    // ===== 关联项模糊筛选 =====

    @Test
    void listJoinAppliesJoinDataFuzzyFilter() {
        WikiMainDataDo meta = dataItem("joinId", "enemy_drop", WikiConstants.DATA_TYPE_JOIN,
                """
                [{"name": "敌人", "dataName": "enemy", "type": "join", "join": "monsterId"}]
                """);
        WikiMainDataDo monster = dataItem("monsterId", "monster", WikiConstants.DATA_TYPE_DATA, "[]");
        when(wikiMainDataDao.selectById("joinId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiMainDataDao.selectByMainId("mainId")).thenReturn(List.of(monster, meta));
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiDynamicDataDao.countByCondition(eq("wiki_re9_enemy_drop"), anyList(), any(), anyMap()))
                .thenReturn(1L);
        ArgumentCaptor<String> whereCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_enemy_drop"), anyString(), anyList(),
                whereCaptor.capture(), eq("ORDER BY base.field_id DESC"), eq(0L), eq(15), eq(true),
                paramsCaptor.capture()))
                .thenReturn(List.of(row("field_id", "rec1", "field_enemy_id", "m1",
                        "enemy_name", "史莱姆", "enemy_code", "slime001")));

        ApiRequest<WikiDataRequest> request = apiRequestForList("joinId");
        QueryCondition root = QueryCondition.group(LogicalOperator.OR, List.of(
                QueryCondition.leaf(QueryOperator.LIKE, "fieldEnemyName", "恶"),
                QueryCondition.leaf(QueryOperator.LIKE, "fieldEnemyCode", "evil")));
        request.getQuery().setData(root);

        ListResult<WikiDataVo> result = service.list(request);

        assertEquals(1, result.getRecords().size());
        String whereSql = whereCaptor.getValue();
        assertTrue(whereSql.contains("t1.field_name LIKE CONCAT('%', #{p0}, '%')"));
        assertTrue(whereSql.contains("t1.field_code LIKE CONCAT('%', #{p1}, '%')"));
        assertEquals("恶", paramsCaptor.getValue().get("p0"));
    }

    // ===== 新建 =====

    @Test
    void saveDataInsertsRowWithFixedAndDynamicColumns() {
        WikiMainDataDo meta = dataItem("dataId", "item", WikiConstants.DATA_TYPE_DATA,
                """
                [{"name": "价格", "dataName": "price", "type": "number"}]
                """);
        when(wikiMainDataDao.selectById("dataId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);

        WikiDataRequest request = new WikiDataRequest();
        request.setFieldDataId("dataId");
        request.setFieldName("长剑");
        request.setFieldCode("sword001");
        request.setFieldData(new LinkedHashMap<>(Map.of("fieldPrice", 100)));

        String id = service.save(request);

        assertNotNull(id);
        ArgumentCaptor<Map> rowCaptor = ArgumentCaptor.forClass(Map.class);
        verify(wikiDynamicDataDao).insert(eq("wiki_re9_item"), rowCaptor.capture());
        Map<String, Object> row = rowCaptor.getValue();
        assertEquals(id, row.get("field_id"));
        assertEquals("长剑", row.get("field_name"));
        assertEquals("sword001", row.get("field_code"));
        assertEquals(100, row.get("field_price"));
    }

    @Test
    void saveDataRejectsMissingRequiredFixedColumns() {
        WikiMainDataDo meta = dataItem("dataId", "item", WikiConstants.DATA_TYPE_DATA, "[]");
        when(wikiMainDataDao.selectById("dataId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);

        WikiDataRequest request = new WikiDataRequest();
        request.setFieldDataId("dataId");
        request.setFieldCode("sword001");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(request));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void saveJoinRejectsMissingJoinValue() {
        WikiMainDataDo meta = dataItem("joinId", "enemy_drop", WikiConstants.DATA_TYPE_JOIN,
                """
                [{"name": "敌人", "dataName": "enemy", "type": "join", "join": "monsterId"}]
                """);
        WikiMainDataDo monster = dataItem("monsterId", "monster", WikiConstants.DATA_TYPE_DATA, "[]");
        when(wikiMainDataDao.selectById("joinId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiMainDataDao.selectById("monsterId")).thenReturn(monster);
        when(wikiDynamicDataDao.tableExists("wiki_re9_monster")).thenReturn(true);
        when(wikiDynamicDataDao.countByCondition(eq("wiki_re9_monster"), isNull(),
                eq("field_id = #{id}"), anyMap())).thenReturn(0L);

        WikiDataRequest request = new WikiDataRequest();
        request.setFieldDataId("joinId");
        request.setFieldData(new LinkedHashMap<>(Map.of("fieldEnemyId", "ghost")));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(request));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    // ===== 更新 / 删除 / 批量删除 =====

    @Test
    void updateThrowsNotFoundWhenRecordMissing() {
        WikiMainDataDo meta = dataItem("dataId", "item", WikiConstants.DATA_TYPE_DATA, "[]");
        when(wikiMainDataDao.selectById("dataId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);
        when(wikiDynamicDataDao.countByCondition(eq("wiki_re9_item"), isNull(),
                eq("field_id = #{id}"), anyMap())).thenReturn(0L);

        WikiDataRequest request = new WikiDataRequest();
        request.setFieldDataId("dataId");
        request.setFieldId("missing");
        request.setFieldName("长剑");
        request.setFieldCode("sword001");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(request));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deleteByIdCallsDynamicDao() {
        WikiMainDataDo meta = dataItem("dataId", "item", WikiConstants.DATA_TYPE_DATA, "[]");
        when(wikiMainDataDao.selectById("dataId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);
        when(wikiDynamicDataDao.countByCondition(eq("wiki_re9_item"), isNull(),
                eq("field_id = #{id}"), anyMap())).thenReturn(1L);

        service.delete("rec1", "dataId");

        verify(wikiDynamicDataDao).deleteById("wiki_re9_item", "rec1");
    }

    @Test
    void batchDeleteRejectsNonJoinDataItem() {
        WikiMainDataDo meta = dataItem("dataId", "item", WikiConstants.DATA_TYPE_DATA, "[]");
        when(wikiMainDataDao.selectById("dataId")).thenReturn(meta);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.batchDelete("dataId", List.of("r1")));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void batchDeleteDeletesForJoinDataItem() {
        WikiMainDataDo meta = dataItem("joinId", "enemy_drop", WikiConstants.DATA_TYPE_JOIN, "[]");
        when(wikiMainDataDao.selectById("joinId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);

        service.batchDelete("joinId", List.of("r1", "r2"));

        verify(wikiDynamicDataDao).batchDeleteByIds("wiki_re9_enemy_drop", List.of("r1", "r2"));
    }

    // ===== 表不存在 =====

    @Test
    void listThrowsNotFoundWhenTableMissing() {
        WikiMainDataDo meta = dataItem("dataId", "item", WikiConstants.DATA_TYPE_DATA, "[]");
        when(wikiMainDataDao.selectById("dataId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.list(apiRequestForList("dataId")));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    // ===== 导入导出（TASK-W3-03，委托 ImportExportProcessor） =====

    @Test
    void templateDelegatesToProcessor() {
        WikiMainDataDo meta = dataItem("joinId", "enemy_drop", WikiConstants.DATA_TYPE_JOIN, "[]");
        when(wikiMainDataDao.selectById("joinId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(importExportProcessor.buildTemplate(meta)).thenReturn(new byte[]{1, 2, 3});

        byte[] bytes = service.template("joinId");

        assertArrayEquals(new byte[]{1, 2, 3}, bytes);
        verify(importExportProcessor).buildTemplate(meta);
    }

    @Test
    void importDataLoadsAttachmentPathAndDelegatesWithSkipFlags() {
        WikiMainDataDo meta = dataItem("joinId", "enemy_drop", WikiConstants.DATA_TYPE_JOIN, "[]");
        when(wikiMainDataDao.selectById("joinId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(wikiCRPService.loadAttachmentFilePath("att1")).thenReturn("import/x.xlsx");
        ImportResult expected = new ImportResult();
        expected.countTotal();
        expected.countSuccess(1);
        when(importExportProcessor.importRows(eq(main()), eq(meta), eq("import/x.xlsx"),
                eq(true), eq(true))).thenReturn(expected);

        WikiDataRequest request = new WikiDataRequest();
        request.setFieldDataId("joinId");
        request.setFieldAttachmentId("att1");
        request.setSkipFail(true);
        request.setSkipError(true);

        ImportResult result = service.importData(request);

        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        verify(wikiCRPService).loadAttachmentFilePath("att1");
        verify(importExportProcessor).importRows(main(), meta, "import/x.xlsx", true, true);
    }

    @Test
    void importDataRejectsNullRequest() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.importData(null));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void exportDelegatesToProcessor() {
        WikiMainDataDo meta = dataItem("joinId", "enemy_drop", WikiConstants.DATA_TYPE_JOIN, "[]");
        when(wikiMainDataDao.selectById("joinId")).thenReturn(meta);
        when(wikiMainDao.selectById("mainId")).thenReturn(main());
        when(importExportProcessor.export(main(), meta)).thenReturn(new byte[]{4, 5});

        byte[] bytes = service.export("joinId");

        assertArrayEquals(new byte[]{4, 5}, bytes);
        verify(importExportProcessor).export(main(), meta);
    }
}
