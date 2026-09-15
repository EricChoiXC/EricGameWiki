package com.wiki.admin.wiki.util;

import com.wiki.admin.sys.attachment.properties.AttachmentProperties;
import com.wiki.admin.wiki.dao.IWikiMainDataDao;
import com.wiki.admin.wiki.dao.dynamic.WikiDynamicDataDao;
import com.wiki.admin.wiki.model.dto.ImportResult;
import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link ImportExportProcessor}（TASK-W3-02）：
 * 模板生成（关联数据显示 ${关联数据}编号、不列附件类）、导入（读取→校验→分批 batchInsert、
 * 异常/失败跳过开关）、导出（关联数据列显示目标记录编号）、批次事务经 TransactionTemplate 控制。
 */
class ImportExportProcessorTest {

    @TempDir
    Path tempDir;

    private WikiDynamicDataDao wikiDynamicDataDao;
    private IWikiMainDataDao wikiMainDataDao;
    private AttachmentProperties attachmentProperties;
    private ImportExportProcessor processor;

    @BeforeEach
    void setUp() {
        wikiDynamicDataDao = mock(WikiDynamicDataDao.class);
        wikiMainDataDao = mock(IWikiMainDataDao.class);
        attachmentProperties = new AttachmentProperties();
        attachmentProperties.setStorageRoot(tempDir.toString());
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenAnswer(inv -> new DefaultTransactionStatus(null, true, false, false, false, null));
        processor = new ImportExportProcessor(wikiDynamicDataDao, wikiMainDataDao,
                attachmentProperties, transactionManager);
    }

    // ===== 测试数据构建 =====

    private WikiMainDo main() {
        WikiMainDo main = new WikiMainDo();
        main.setFieldId("mainId");
        main.setFieldSimpleName("re9");
        return main;
    }

    private WikiMainDataDo joinItem() {
        WikiMainDataDo data = new WikiMainDataDo();
        data.setFieldId("dropId");
        data.setFieldMainId("mainId");
        data.setFieldName("敌人掉落");
        data.setFieldDataName("enemy_drop");
        data.setFieldDataType(WikiConstants.DATA_TYPE_JOIN);
        data.setFieldDataJson("""
                [
                  {"name": "敌人", "dataName": "enemy", "type": "join", "join": "monsterId"},
                  {"name": "道具", "dataName": "item", "type": "join", "join": "itemId"},
                  {"name": "掉落概率", "dataName": "rate", "type": "number"}
                ]
                """);
        return data;
    }

    private WikiMainDataDo target(String id, String name, String dataName, String type) {
        WikiMainDataDo data = new WikiMainDataDo();
        data.setFieldId(id);
        data.setFieldName(name);
        data.setFieldDataName(dataName);
        data.setFieldDataType(type);
        return data;
    }

    private Map<String, Object> row(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    private Path writeXlsx(String[] header, String[][] dataRows) throws IOException {
        Path file = tempDir.resolve("import_" + System.nanoTime() + ".xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             OutputStream os = Files.newOutputStream(file)) {
            Sheet sheet = workbook.createSheet("import");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < header.length; i++) {
                headerRow.createCell(i).setCellValue(header[i]);
            }
            for (int r = 0; r < dataRows.length; r++) {
                Row row = sheet.createRow(r + 1);
                String[] cells = dataRows[r];
                for (int c = 0; c < cells.length; c++) {
                    row.createCell(c).setCellValue(cells[c]);
                }
            }
            workbook.write(os);
        }
        return file;
    }

    private void stubJoinTargetLookup() {
        when(wikiMainDataDao.selectById("monsterId"))
                .thenReturn(target("monsterId", "怪物", "monster", WikiConstants.DATA_TYPE_DATA));
        when(wikiMainDataDao.selectById("itemId"))
                .thenReturn(target("itemId", "道具", "item", WikiConstants.DATA_TYPE_DATA));
    }

    private void stubTargetCodeLookup() {
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_monster"), anyString(), isNull(),
                eq("field_code = #{code}"), isNull(), eq(0L), eq(1), eq(false), anyMap()))
                .thenReturn(List.of(row("field_id", "m1", "field_code", "s001")));
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_item"), anyString(), isNull(),
                eq("field_code = #{code}"), isNull(), eq(0L), eq(1), eq(false), anyMap()))
                .thenReturn(List.of(row("field_id", "i1", "field_code", "p001")));
    }

    // ===== 模板生成 =====

    @Test
    void buildTemplateNamesJoinColumnsWithTargetNameAndSkipsAttachment() throws Exception {
        WikiMainDataDo meta = joinItem();
        meta.setFieldDataJson("""
                [
                  {"name": "敌人", "dataName": "enemy", "type": "join", "join": "monsterId"},
                  {"name": "道具", "dataName": "item", "type": "join", "join": "itemId"},
                  {"name": "掉落概率", "dataName": "rate", "type": "number"},
                  {"name": "截图", "dataName": "shot", "type": "attachment"}
                ]
                """);
        stubJoinTargetLookup();

        byte[] bytes = processor.buildTemplate(meta);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("怪物编号", header.getCell(0).getStringCellValue());
            assertEquals("道具编号", header.getCell(1).getStringCellValue());
            assertEquals("掉落概率", header.getCell(2).getStringCellValue());
            assertEquals(3, header.getLastCellNum());
        }
    }

    // ===== 导入：正常流程 =====

    @Test
    void importRowsTranslatesJoinCodeToTargetIdAndInserts() throws Exception {
        stubJoinTargetLookup();
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_monster")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);
        stubTargetCodeLookup();

        Path file = writeXlsx(new String[]{"怪物编号", "道具编号", "掉落概率"},
                new String[][]{
                        {"s001", "p001", "0.5"},
                        {"s001", "p001", "0.3"}
                });

        ImportResult result = processor.importRows(main(), joinItem(),
                tempDir.relativize(file).toString(), false, false);

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getSkipCount());

        ArgumentCaptor<List> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(wikiDynamicDataDao).batchInsert(eq("wiki_re9_enemy_drop"), batchCaptor.capture());
        List<Map<String, Object>> batch = batchCaptor.getValue();
        assertEquals(2, batch.size());
        assertEquals("m1", batch.get(0).get("field_enemy_id"));
        assertEquals("i1", batch.get(0).get("field_item_id"));
        assertEquals(new BigDecimal("0.5"), batch.get(0).get("field_rate"));
    }

    @Test
    void importRowsSkipsBlankRows() throws Exception {
        stubJoinTargetLookup();
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_monster")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);
        stubTargetCodeLookup();

        Path file = writeXlsx(new String[]{"怪物编号", "道具编号", "掉落概率"},
                new String[][]{
                        {"s001", "p001", "0.5"},
                        {"", "", ""},
                        {"s001", "p001", "0.1"}
                });

        ImportResult result = processor.importRows(main(), joinItem(),
                tempDir.relativize(file).toString(), false, false);

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
    }

    // ===== 导入：异常数据跳过 =====

    @Test
    void importRowsThrowsBadRequestWhenJoinCodeMissingAndSkipErrorOff() throws Exception {
        stubJoinTargetLookup();
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_monster")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_monster"), anyString(), isNull(),
                eq("field_code = #{code}"), isNull(), eq(0L), eq(1), eq(false), anyMap()))
                .thenReturn(List.of());

        Path file = writeXlsx(new String[]{"怪物编号", "道具编号", "掉落概率"},
                new String[][]{{"ghost", "p001", "0.5"}});

        BusinessException ex = assertThrows(BusinessException.class,
                () -> processor.importRows(main(), joinItem(),
                        tempDir.relativize(file).toString(), false, false));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("第2行"));
        verify(wikiDynamicDataDao, never()).batchInsert(anyString(), anyList());
    }

    @Test
    void importRowsSkipsInvalidRowWhenSkipErrorOn() throws Exception {
        stubJoinTargetLookup();
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_monster")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_monster"), anyString(), isNull(),
                eq("field_code = #{code}"), isNull(), eq(0L), eq(1), eq(false), anyMap()))
                .thenAnswer(inv -> {
                    Map<String, Object> params = inv.getArgument(8);
                    return "s001".equals(String.valueOf(params.get("code")))
                            ? List.of(row("field_id", "m1", "field_code", "s001"))
                            : List.of();
                });
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_item"), anyString(), isNull(),
                eq("field_code = #{code}"), isNull(), eq(0L), eq(1), eq(false), anyMap()))
                .thenReturn(List.of(row("field_id", "i1", "field_code", "p001")));

        Path file = writeXlsx(new String[]{"怪物编号", "道具编号", "掉落概率"},
                new String[][]{
                        {"s001", "p001", "0.5"},
                        {"ghost", "p001", "0.5"}
                });

        ImportResult result = processor.importRows(main(), joinItem(),
                tempDir.relativize(file).toString(), false, true);

        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getSkipCount());
        assertEquals(1, result.getFailDetails().size());
        assertEquals(3, result.getFailDetails().get(0).getRow());
        assertTrue(result.getFailDetails().get(0).getReason().contains("关联数据编号不存在"));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(wikiDynamicDataDao).batchInsert(eq("wiki_re9_enemy_drop"), captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    void importRowsRejectsInvalidNumberWithoutSkipError() throws Exception {
        stubJoinTargetLookup();
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_monster")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);
        stubTargetCodeLookup();

        Path file = writeXlsx(new String[]{"怪物编号", "道具编号", "掉落概率"},
                new String[][]{{"s001", "p001", "abc"}});

        BusinessException ex = assertThrows(BusinessException.class,
                () -> processor.importRows(main(), joinItem(),
                        tempDir.relativize(file).toString(), false, false));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("数字格式不正确"));
    }

    @Test
    void importRowsRejectsMissingTemplateColumn() throws Exception {
        stubJoinTargetLookup();
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);

        Path file = writeXlsx(new String[]{"怪物编号", "掉落概率"},
                new String[][]{{"s001", "0.5"}});

        BusinessException ex = assertThrows(BusinessException.class,
                () -> processor.importRows(main(), joinItem(),
                        tempDir.relativize(file).toString(), false, false));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("缺少模板列"));
    }

    // ===== 导入：失败批次跳过与回滚 =====

    @Test
    void importRowsThrowsInternalErrorWhenBatchFailsAndSkipFailOff() throws Exception {
        stubJoinTargetLookup();
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_monster")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);
        stubTargetCodeLookup();
        when(wikiDynamicDataDao.batchInsert(anyString(), anyList()))
                .thenThrow(new RuntimeException("db error"));

        Path file = writeXlsx(new String[]{"怪物编号", "道具编号", "掉落概率"},
                new String[][]{{"s001", "p001", "0.5"}});

        BusinessException ex = assertThrows(BusinessException.class,
                () -> processor.importRows(main(), joinItem(),
                        tempDir.relativize(file).toString(), false, false));
        assertEquals(ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
    }

    @Test
    void importRowsSkipsFailedBatchWhenSkipFailOn() throws Exception {
        stubJoinTargetLookup();
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_monster")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_item")).thenReturn(true);
        stubTargetCodeLookup();
        when(wikiDynamicDataDao.batchInsert(anyString(), anyList()))
                .thenThrow(new RuntimeException("batch error"))
                .thenReturn(1);

        String[][] rows = new String[ImportExportProcessor.BATCH_SIZE + 1][];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new String[]{"s001", "p001", "0.5"};
        }
        Path file = writeXlsx(new String[]{"怪物编号", "道具编号", "掉落概率"}, rows);

        ImportResult result = processor.importRows(main(), joinItem(),
                tempDir.relativize(file).toString(), true, false);

        assertEquals(ImportExportProcessor.BATCH_SIZE + 1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(ImportExportProcessor.BATCH_SIZE, result.getSkipCount());
        assertEquals(1, result.getFailDetails().size());
        assertEquals(2, result.getFailDetails().get(0).getRow());
    }

    // ===== 导出 =====

    @Test
    void exportWritesJoinTargetCodes() throws Exception {
        stubJoinTargetLookup();
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiMainDataDao.selectByMainId("mainId")).thenReturn(List.of(
                target("monsterId", "怪物", "monster", WikiConstants.DATA_TYPE_DATA),
                target("itemId", "道具", "item", WikiConstants.DATA_TYPE_DATA),
                joinItem()));
        when(wikiDynamicDataDao.selectByCondition(eq("wiki_re9_enemy_drop"), anyString(), anyList(),
                isNull(), eq("ORDER BY base.field_id"), eq(0L), eq(0), eq(false), isNull()))
                .thenReturn(List.of(row(
                        "field_id", "rec1",
                        "field_enemy_id", "m1", "enemy_name", "史莱姆", "enemy_code", "s001",
                        "field_item_id", "i1", "item_name", "药水", "item_code", "p001",
                        "field_rate", new BigDecimal("0.5000"))));

        byte[] bytes = processor.export(main(), joinItem());

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("怪物编号", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("道具编号", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("掉落概率", sheet.getRow(0).getCell(2).getStringCellValue());
            assertEquals("s001", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("p001", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(0.5, sheet.getRow(1).getCell(2).getNumericCellValue());
        }
    }

    // ===== 类型与存在性限制 =====

    @Test
    void importRejectsNonJoinDataItem() {
        WikiMainDataDo dataItem = target("d1", "道具", "item", WikiConstants.DATA_TYPE_DATA);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> processor.importRows(main(), dataItem, "x.xlsx", false, false));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void exportRejectsNonJoinDataItem() {
        WikiMainDataDo dataItem = target("d1", "道具", "item", WikiConstants.DATA_TYPE_DATA);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> processor.export(main(), dataItem));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void importThrowsNotFoundWhenTableMissing() throws Exception {
        stubJoinTargetLookup();
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(false);

        Path file = writeXlsx(new String[]{"怪物编号"}, new String[][]{{"s001"}});

        BusinessException ex = assertThrows(BusinessException.class,
                () -> processor.importRows(main(), joinItem(),
                        tempDir.relativize(file).toString(), false, false));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void importThrowsNotFoundWhenTargetTableMissing() throws Exception {
        stubJoinTargetLookup();
        when(wikiDynamicDataDao.tableExists("wiki_re9_enemy_drop")).thenReturn(true);
        when(wikiDynamicDataDao.tableExists("wiki_re9_monster")).thenReturn(false);

        Path file = writeXlsx(new String[]{"怪物编号", "道具编号", "掉落概率"},
                new String[][]{{"s001", "p001", "0.5"}});

        BusinessException ex = assertThrows(BusinessException.class,
                () -> processor.importRows(main(), joinItem(),
                        tempDir.relativize(file).toString(), false, false));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }
}
