package com.wiki.web.wiki.service;

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
import com.wiki.admin.wiki.util.WikiConstants;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.query.LogicalOperator;
import com.wiki.common.model.query.QueryCondition;
import com.wiki.common.model.query.QueryOperator;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ListResult;
import com.wiki.common.util.JsonUtil;
import com.wiki.web.wiki.model.dto.WebWikiDataItemVo;
import com.wiki.web.wiki.model.dto.WebWikiProjectVo;
import com.wiki.web.wiki.model.dto.WebWikiRecordDetailVo;
import com.wiki.web.wiki.model.dto.WebWikiRecordListVo;
import com.wiki.web.wiki.model.dto.WebWikiRecordVo;
import com.wiki.web.wiki.service.impl.WebWikiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link WebWikiServiceImpl} 前台只读编排逻辑：
 * 首页项目列表关键字过滤、项目加载状态过滤、数据项列表过滤、
 * 记录列表按类型构建关键字条件、详情页页面配置与关联记录组装。
 */
class WebWikiServiceImplTest {

    private IWikiMainService wikiMainService;
    private IWikiMainDataService wikiMainDataService;
    private IWikiDataService wikiDataService;
    private IWikiPageService wikiPageService;
    private IWikiMainDao wikiMainDao;
    private IWikiMainDataDao wikiMainDataDao;
    private WebWikiServiceImpl service;

    @BeforeEach
    void setUp() {
        wikiMainService = mock(IWikiMainService.class);
        wikiMainDataService = mock(IWikiMainDataService.class);
        wikiDataService = mock(IWikiDataService.class);
        wikiPageService = mock(IWikiPageService.class);
        wikiMainDao = mock(IWikiMainDao.class);
        wikiMainDataDao = mock(IWikiMainDataDao.class);
        service = new WebWikiServiceImpl(wikiMainService, wikiMainDataService, wikiDataService,
                wikiPageService, wikiMainDao, wikiMainDataDao);
    }

    // ===== 测试数据构建 =====

    private WikiMainDo enabledMain() {
        WikiMainDo main = new WikiMainDo();
        main.setFieldId("mainId");
        main.setFieldName("异度神剑3");
        main.setFieldEnName("Xenoblade 3");
        main.setFieldJpName("ゼノブレイド3");
        main.setFieldSimpleName("re3");
        main.setFieldStatus(WikiConstants.STATUS_ENABLED);
        return main;
    }

    private WikiMainDo disabledMain() {
        WikiMainDo main = enabledMain();
        main.setFieldStatus(WikiConstants.STATUS_DISABLED);
        return main;
    }

    private WikiMainVo mainVo() {
        WikiMainVo vo = new WikiMainVo();
        vo.setFieldId("mainId");
        vo.setFieldName("异度神剑3");
        vo.setFieldEnName("Xenoblade 3");
        vo.setFieldJpName("ゼノブレイド3");
        vo.setFieldSimpleName("re3");
        return vo;
    }

    private WikiMainDataDo dataItem(String id, String name, String dataName, String dataType) {
        WikiMainDataDo item = new WikiMainDataDo();
        item.setFieldId(id);
        item.setFieldMainId("mainId");
        item.setFieldName(name);
        item.setFieldDataName(dataName);
        item.setFieldDataType(dataType);
        return item;
    }

    private WikiDataVo record(String id, String name, String code) {
        WikiDataVo vo = new WikiDataVo();
        vo.setFieldId(id);
        vo.setFieldName(name);
        vo.setFieldCode(code);
        return vo;
    }

    // ===== 首页项目列表 =====

    @Test
    void homeProjectsBuildsStatusAndKeywordConditions() {
        when(wikiMainService.list(any())).thenReturn(ListResult.of(List.of(mainVo())));

        List<WebWikiProjectVo> result = service.homeProjects("异度");

        assertEquals(1, result.size());
        WebWikiProjectVo vo = result.get(0);
        assertEquals("异度神剑3", vo.getFieldName());
        assertEquals("re3", vo.getFieldSimpleName());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ApiRequest<WikiMainDo>> captor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(wikiMainService).list(captor.capture());
        ApiRequest<WikiMainDo> captured = captor.getValue();
        assertEquals(Boolean.FALSE, captured.getQuery().getNeedPage());
        QueryCondition root = captured.getQuery().getData();
        assertNotNull(root);
        assertEquals(LogicalOperator.AND, root.getLogical());
        assertEquals(2, root.getChildren().size());

        QueryCondition statusCond = root.getChildren().get(0);
        assertEquals(QueryOperator.EQ, statusCond.getOperator());
        assertEquals("fieldStatus", statusCond.getFieldName());
        assertEquals(WikiConstants.STATUS_ENABLED, statusCond.getValue());

        QueryCondition keywordGroup = root.getChildren().get(1);
        assertEquals(LogicalOperator.OR, keywordGroup.getLogical());
        assertEquals(4, keywordGroup.getChildren().size());
    }

    @Test
    void homeProjectsWithoutKeywordOnlyFiltersEnabled() {
        when(wikiMainService.list(any())).thenReturn(ListResult.of(List.of(mainVo())));

        service.homeProjects(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ApiRequest<WikiMainDo>> captor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(wikiMainService).list(captor.capture());
        QueryCondition root = captor.getValue().getQuery().getData();
        assertEquals(1, root.getChildren().size());
    }

    // ===== 项目加载 =====

    @Test
    void loadProjectReturnsEnabledProject() {
        when(wikiMainDao.selectBySimpleName("re3")).thenReturn(enabledMain());

        WebWikiProjectVo vo = service.loadProject("re3");

        assertEquals("异度神剑3", vo.getFieldName());
        assertEquals("re3", vo.getFieldSimpleName());
    }

    @Test
    void loadProjectThrowsNotFoundWhenDisabled() {
        when(wikiMainDao.selectBySimpleName("re3")).thenReturn(disabledMain());

        BusinessException e = assertThrows(BusinessException.class, () -> service.loadProject("re3"));
        assertEquals(ErrorCode.NOT_FOUND, e.getErrorCode());
    }

    // ===== 项目数据项列表 =====

    @Test
    void listDataItemsBuildsMainAndTypeConditions() {
        when(wikiMainDao.selectBySimpleName("re3")).thenReturn(enabledMain());
        WikiMainDataVo item = new WikiMainDataVo();
        item.setFieldId("dataId");
        item.setFieldMainId("mainId");
        item.setFieldName("怪物图鉴");
        item.setFieldDataName("monster");
        item.setFieldDataType(WikiConstants.DATA_TYPE_DATA);
        when(wikiMainDataService.list(any())).thenReturn(ListResult.of(List.of(item)));

        List<WebWikiDataItemVo> result = service.listDataItems("re3", "怪物", WikiConstants.DATA_TYPE_DATA);

        assertEquals(1, result.size());
        assertEquals("monster", result.get(0).getFieldDataName());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ApiRequest<WikiMainDataVo>> captor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(wikiMainDataService).list(captor.capture());
        QueryCondition root = captor.getValue().getQuery().getData();
        assertEquals(LogicalOperator.AND, root.getLogical());
        assertEquals(3, root.getChildren().size());
        assertEquals("fieldMainId", root.getChildren().get(0).getFieldName());
        assertEquals("fieldDataType", root.getChildren().get(2).getFieldName());
    }

    // ===== 记录列表 =====

    @Test
    void listRecordsForDocFiltersByTitle() {
        when(wikiMainDao.selectBySimpleName("re3")).thenReturn(enabledMain());
        when(wikiMainDataDao.selectByMainIdAndDataName("mainId", "guide"))
                .thenReturn(dataItem("dataId", "攻略指南", "guide", WikiConstants.DATA_TYPE_DOC));
        when(wikiDataService.list(any())).thenReturn(ListResult.of(List.of(record("r1", "新手攻略", "001"))));

        WebWikiRecordListVo vo = service.listRecords("re3", "guide", "攻略");

        assertEquals(WikiConstants.DATA_TYPE_DOC, vo.getDataItem().getFieldDataType());
        assertEquals(1, vo.getRecords().size());
        assertEquals("新手攻略", vo.getRecords().get(0).getFieldName());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ApiRequest<WikiDataRequest>> captor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(wikiDataService).list(captor.capture());
        ApiRequest<WikiDataRequest> captured = captor.getValue();
        assertEquals("dataId", captured.getData().getFieldDataId());
        assertEquals(Boolean.FALSE, captured.getQuery().getNeedPage());
        QueryCondition cond = captured.getQuery().getData();
        assertEquals(QueryOperator.LIKE, cond.getOperator());
        assertEquals("fieldName", cond.getFieldName());
    }

    @Test
    void listRecordsForDataFiltersByNameOrCode() {
        when(wikiMainDao.selectBySimpleName("re3")).thenReturn(enabledMain());
        when(wikiMainDataDao.selectByMainIdAndDataName("mainId", "monster"))
                .thenReturn(dataItem("dataId", "怪物图鉴", "monster", WikiConstants.DATA_TYPE_DATA));
        when(wikiDataService.list(any())).thenReturn(ListResult.of(List.of(record("r1", "古拉兽", "101"))));

        WebWikiRecordListVo vo = service.listRecords("re3", "monster", "兽");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ApiRequest<WikiDataRequest>> captor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(wikiDataService).list(captor.capture());
        QueryCondition group = captor.getValue().getQuery().getData();
        assertEquals(LogicalOperator.OR, group.getLogical());
        assertEquals(2, group.getChildren().size());
        assertEquals("fieldCode", group.getChildren().get(1).getFieldName());
        assertEquals(1, vo.getRecords().size());
    }

    // ===== 记录详情 =====

    @Test
    void loadRecordAssemblesDetailWithPageConfigAndRelatedRecords() {
        when(wikiMainDao.selectBySimpleName("re3")).thenReturn(enabledMain());
        WikiMainDataDo self = dataItem("dataId", "怪物图鉴", "monster", WikiConstants.DATA_TYPE_DATA);
        when(wikiMainDataDao.selectByMainIdAndDataName("mainId", "monster")).thenReturn(self);

        Map<String, Object> fieldData = new LinkedHashMap<>();
        fieldData.put("fieldPrice", "100");
        WikiDataVo record = record("r1", "古拉兽", "101");
        record.setFieldData(fieldData);
        when(wikiDataService.load("r1", "dataId")).thenReturn(record);

        // 页面配置存在
        WikiPageConfigDo config = new WikiPageConfigDo();
        when(wikiPageService.load("dataId")).thenReturn(pageVo(config));

        // 同项目存在引用本数据项的关联项
        WikiMainDataDo join = dataItem("joinId", "敌人掉落", "drops", WikiConstants.DATA_TYPE_JOIN);
        join.setFieldDataJson(JsonUtil.toJson(List.of(detail("敌人", "enemy", "join", "dataId"))));
        when(wikiMainDataDao.selectByMainId("mainId")).thenReturn(List.of(self, join));
        when(wikiDataService.list(any())).thenReturn(ListResult.of(List.of(record("j1", "古拉兽掉落", null))));

        WebWikiRecordDetailVo vo = service.loadRecord("re3", "monster", "r1");

        assertEquals("r1", vo.getRecord().getFieldId());
        assertEquals(config, vo.getPageConfig());
        assertNotNull(vo.getRelatedRecords());
        assertEquals(1, vo.getRelatedRecords().size());
        assertEquals(1, vo.getRelatedRecords().get("joinId").size());
    }

    @Test
    void loadRecordReturnsNullPageConfigWhenNotConfigured() {
        when(wikiMainDao.selectBySimpleName("re3")).thenReturn(enabledMain());
        when(wikiMainDataDao.selectByMainIdAndDataName("mainId", "monster"))
                .thenReturn(dataItem("dataId", "怪物图鉴", "monster", WikiConstants.DATA_TYPE_DATA));
        when(wikiDataService.load("r1", "dataId")).thenReturn(record("r1", "古拉兽", "101"));
        when(wikiPageService.load("dataId")).thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "未配置"));
        when(wikiMainDataDao.selectByMainId("mainId")).thenReturn(List.of());

        WebWikiRecordDetailVo vo = service.loadRecord("re3", "monster", "r1");

        assertNull(vo.getPageConfig());
        assertNotNull(vo.getRelatedRecords());
        assertEquals(0, vo.getRelatedRecords().size());
    }

    private WikiPageVo pageVo(WikiPageConfigDo config) {
        WikiPageVo vo = new WikiPageVo();
        vo.setFieldId("pageId");
        vo.setFieldDataId("dataId");
        vo.setFieldWikiPage(config);
        return vo;
    }

    private WikiDataDetailDo detail(String name, String dataName, String type, String join) {
        WikiDataDetailDo d = new WikiDataDetailDo();
        d.setName(name);
        d.setDataName(dataName);
        d.setType(type);
        d.setJoin(join);
        return d;
    }
}
