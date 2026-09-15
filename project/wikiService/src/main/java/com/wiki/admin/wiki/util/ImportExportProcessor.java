package com.wiki.admin.wiki.util;

import com.wiki.admin.sys.attachment.properties.AttachmentProperties;
import com.wiki.admin.wiki.dao.IWikiMainDataDao;
import com.wiki.admin.wiki.dao.dynamic.WikiDynamicDataDao;
import com.wiki.admin.wiki.model.dto.ImportResult;
import com.wiki.admin.wiki.model.dto.WikiDataDetailDo;
import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.util.IDUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关联数据批量导入导出组件，对应 {@code docs/admin/wiki/wiki技术方案.md} 6（ARCH-W04）。
 * <p>
 * 仅关联项（{@code join}）数据项支持批量导入导出（技术方案 6.1）。本组件负责：
 * <ul>
 *   <li>模板生成：首行标题行，过滤附件类明细，关联数据显示 {@code ${关联目标数据项名称}编号}</li>
 *   <li>文件解析：Apache POI 读取 xlsx，按标题行映射数据列</li>
 *   <li>数据校验：必填（关联数据列）、类型、枚举值、关联数据存在性</li>
 *   <li>分批导入：每批 {@value #BATCH_SIZE} 条，批次事务通过 {@code TransactionTemplate} 编程式控制</li>
 *   <li>导出：全量查询动态表数据，关联数据列显示目标记录的 field_code 值</li>
 * </ul>
 * <p>
 * <b>事务策略</b>（技术方案 6.5）：导入主流程由 Service 层标注
 * {@code @Transactional(rollbackFor = Exception.class)}；批次通过本组件内的
 * {@code TransactionTemplate} 控制：
 * <ul>
 *   <li>未选"失败数据跳过"：批次加入主事务（REQUIRED），任一批次失败抛出 {@code INTERNAL_ERROR}，
 *       由主事务整体回滚</li>
 *   <li>选"失败数据跳过"：批次使用 {@code REQUIRES_NEW} 独立提交，失败批次移除并记录明细，已成功批次保留</li>
 * </ul>
 * <p>
 * 导入文件读取复用 {@code WikiCRPService} 调用 {@code sys.attachment} 模块取得附件相对路径，
 * 本组件结合 {@link AttachmentProperties#getStorageRoot()} 解析物理文件后以 POI 读取（技术方案 6.7）。
 * <p>
 * 所有表名 / 列名经 {@link DynamicTableSqlBuilder} / {@link DynamicFieldMaps} 白名单校验后拼接；
 * 数据值通过 {@code WikiDynamicDataDao} 命名参数绑定，杜绝 SQL 注入。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Slf4j
@Component
public class ImportExportProcessor {

    /** 每批导入条数（技术方案 6.4 步骤 5） */
    public static final int BATCH_SIZE = 200;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 附件类明细类型（不参与模板 / 导入 / 导出） */
    private static final String TYPE_ATTACHMENT = "attachment";

    private final WikiDynamicDataDao wikiDynamicDataDao;
    private final IWikiMainDataDao wikiMainDataDao;
    private final AttachmentProperties attachmentProperties;
    private final TransactionTemplate requiredTemplate;
    private final TransactionTemplate requiresNewTemplate;

    public ImportExportProcessor(WikiDynamicDataDao wikiDynamicDataDao,
                                 IWikiMainDataDao wikiMainDataDao,
                                 AttachmentProperties attachmentProperties,
                                 PlatformTransactionManager transactionManager) {
        this.wikiDynamicDataDao = wikiDynamicDataDao;
        this.wikiMainDataDao = wikiMainDataDao;
        this.attachmentProperties = attachmentProperties;
        this.requiredTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ===== 模板生成（技术方案 6.3 / 业务逻辑 6.5 FLOW-W005-01） =====

    /**
     * 生成导入模板 xlsx。
     * <p>
     * 首行为标题行：过滤附件类明细；关联数据显示 {@code ${关联目标数据项名称}编号}，其余显示明细显示名。
     *
     * @param meta 数据项元数据（需为关联项）
     * @return xlsx 字节流
     */
    public byte[] buildTemplate(WikiMainDataDo meta) {
        validateJoinType(meta);
        List<ColumnBinding> bindings = buildBindings(meta);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("import");
            Row header = sheet.createRow(0);
            for (int i = 0; i < bindings.size(); i++) {
                header.createCell(i).setCellValue(bindings.get(i).title);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("[wiki] 导入模板生成失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导入模板生成失败", e);
        }
    }

    // ===== 导入处理（技术方案 6.4 / 业务逻辑 6.5 FLOW-W005-02） =====

    /**
     * 关联数据批量导入。
     * <p>
     * 流程：读取 xlsx → 逐行解析并按模板列名映射 → 数据合理性校验（必填、类型、枚举值、
     * 关联数据存在性）→ 关联数据编号转换为目标记录 field_id → 分批 batchInsert（每批 200 条）。
     * <ul>
     *   <li>{@code skipError}：数据不合理时移除异常项继续，否则抛 {@code BAD_REQUEST}（含错误明细）</li>
     *   <li>{@code skipFail}：批次失败时移除失败批次继续，否则抛 {@code INTERNAL_ERROR}（主事务整体回滚）</li>
     * </ul>
     *
     * @param wikiMain  所属项目（提供简称，用于动态表名）
     * @param meta      数据项元数据（需为关联项）
     * @param filePath  附件相对路径（由 {@code WikiCRPService} 调用 {@code sys.attachment} 取得）
     * @param skipFail  失败数据跳过
     * @param skipError 异常数据跳过
     * @return 导入结果（成功数、跳过数、失败明细）
     */
    public ImportResult importRows(WikiMainDo wikiMain, WikiMainDataDo meta, String filePath,
                                   boolean skipFail, boolean skipError) {
        validateJoinType(meta);
        String tableName = DynamicTableSqlBuilder.buildTableName(wikiMain, meta);
        ensureTableExists(tableName);

        List<ColumnBinding> bindings = buildBindings(meta);
        try (Workbook workbook = openWorkbook(filePath)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() < 1) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "导入文件没有数据行");
            }
            List<ColumnBinding> resolved = resolveBindings(bindings, sheet.getRow(0));

            List<ParsedRow> rows = new ArrayList<>();
            ImportResult result = new ImportResult();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                int rowNo = r + 1;
                result.countTotal();
                try {
                    rows.add(new ParsedRow(parseRow(row, resolved, wikiMain), rowNo));
                } catch (BusinessException e) {
                    // 资源缺失类错误（如关联目标动态表不存在）保持原错误码，不参与异常跳过
                    if (ErrorCode.NOT_FOUND.equals(e.getErrorCode())) {
                        throw e;
                    }
                    if (!skipError) {
                        throw new BusinessException(ErrorCode.BAD_REQUEST,
                                "第" + rowNo + "行数据不合理：" + e.getMessage());
                    }
                    log.warn("[wiki] 导入跳过异常行 row={} reason={}", rowNo, e.getMessage());
                    result.addFail(rowNo, e.getMessage());
                    result.countSkip(1);
                }
            }
            insertBatches(tableName, rows, skipFail, result);
            return result;
        } catch (IOException e) {
            log.error("[wiki] 导入处理失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导入处理失败", e);
        }
    }

    // ===== 导出处理（技术方案 6.6 / 业务逻辑 6.6 FLOW-W006） =====

    /**
     * 导出关联数据项全部数据明细为 xlsx。
     * <p>
     * 首行为标题行（与导入模板一致）；关联数据列显示目标记录的 field_code 值；附件类明细不导出。
     *
     * @param wikiMain 所属项目（提供简称，用于动态表名）
     * @param meta     数据项元数据（需为关联项）
     * @return xlsx 字节流
     */
    public byte[] export(WikiMainDo wikiMain, WikiMainDataDo meta) {
        validateJoinType(meta);
        String tableName = DynamicTableSqlBuilder.buildTableName(wikiMain, meta);
        ensureTableExists(tableName);

        // 关联数据列需要目标记录的 field_code，经 DynamicFieldMaps 联表查询（技术方案 4.5）
        List<WikiMainDataDo> targets = wikiMainDataDao.selectByMainId(meta.getFieldMainId());
        DynamicFieldMaps.JoinQuery joinQuery = DynamicFieldMaps.buildJoinQuery(wikiMain, meta, targets);
        List<Map<String, Object>> rows = wikiDynamicDataDao.selectByCondition(
                tableName, joinQuery.getColumnsSql(), joinQuery.getJoinClauses(),
                null, "ORDER BY base.field_id", 0, 0, false, null);

        List<ColumnBinding> bindings = buildBindings(meta);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            Row header = sheet.createRow(0);
            for (int i = 0; i < bindings.size(); i++) {
                header.createCell(i).setCellValue(bindings.get(i).title);
            }
            int rowIndex = 1;
            for (Map<String, Object> dbRow : rows) {
                Row outRow = sheet.createRow(rowIndex++);
                for (int i = 0; i < bindings.size(); i++) {
                    ColumnBinding binding = bindings.get(i);
                    Object value = binding.isJoin()
                            ? cell(dbRow, binding.detail.getDataName() + "_code")
                            : cell(dbRow, "field_" + binding.detail.getDataName());
                    setCellValue(outRow.createCell(i), value);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("[wiki] 导出失败 tableName={}", tableName, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败", e);
        }
    }

    // ===== 内部：模板列绑定 =====

    /**
     * 模板列绑定：将数据项明细行映射为模板列，关联类明细解析目标数据项并生成
     * {@code ${关联目标数据项名称}编号} 标题。
     */
    private static final class ColumnBinding {

        private final WikiDataDetailDo detail;
        private final String title;
        private final WikiMainDataDo target;
        private int index;

        ColumnBinding(WikiDataDetailDo detail, String title, WikiMainDataDo target) {
            this.detail = detail;
            this.title = title;
            this.target = target;
        }

        boolean isJoin() {
            return WikiConstants.DATA_TYPE_JOIN.equals(detail.getType());
        }
    }

    /**
     * 解析行：数据行 Map + Excel 行号（用于失败明细定位）。
     */
    private record ParsedRow(Map<String, Object> row, int rowNo) {
    }

    /**
     * 按数据项明细定义构建模板列绑定：过滤附件类明细。
     */
    private List<ColumnBinding> buildBindings(WikiMainDataDo meta) {
        List<ColumnBinding> bindings = new ArrayList<>();
        for (WikiDataDetailDo detail : DynamicTableSqlBuilder.parseDetails(meta.getFieldDataJson())) {
            if (TYPE_ATTACHMENT.equals(detail.getType())) {
                continue;
            }
            if (WikiConstants.DATA_TYPE_JOIN.equals(detail.getType())) {
                WikiMainDataDo target = wikiMainDataDao.selectById(detail.getJoin());
                if (target == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "关联目标数据项不存在：" + detail.getJoin());
                }
                String targetName = target.getFieldName() == null
                        ? target.getFieldDataName() : target.getFieldName();
                bindings.add(new ColumnBinding(detail, targetName + "编号", target));
            } else {
                String title = detail.getName() == null ? detail.getDataName() : detail.getName();
                bindings.add(new ColumnBinding(detail, title, null));
            }
        }
        return bindings;
    }

    /**
     * 将模板列绑定与导入文件标题行对齐：按标题匹配列索引，缺失列抛 BAD_REQUEST。
     */
    private List<ColumnBinding> resolveBindings(List<ColumnBinding> bindings, Row headerRow) {
        Map<String, Integer> headerIndex = new HashMap<>();
        if (headerRow != null) {
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) {
                    continue;
                }
                Object headerValue = readCellValue(cell);
                String title = headerValue == null ? "" : String.valueOf(headerValue).trim();
                if (!title.isEmpty()) {
                    headerIndex.put(title, i);
                }
            }
        }
        List<ColumnBinding> resolved = new ArrayList<>(bindings.size());
        for (ColumnBinding binding : bindings) {
            Integer idx = headerIndex.get(binding.title);
            if (idx == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "导入文件缺少模板列：" + binding.title);
            }
            binding.index = idx;
            resolved.add(binding);
        }
        return resolved;
    }

    // ===== 内部：逐行解析与校验 =====

    /**
     * 解析单行为待导入数据 Map：生成 field_id，逐列转换类型，
     * 关联数据列校验必填并将编号转换为目标记录 field_id。
     *
     * @throws BusinessException 该行数据不合理时抛出（reason 为失败原因）
     */
    private Map<String, Object> parseRow(Row row, List<ColumnBinding> bindings, WikiMainDo wikiMain) {
        Map<String, Object> rowMap = new LinkedHashMap<>();
        rowMap.put("field_id", IDUtil.initID());
        for (ColumnBinding binding : bindings) {
            Object raw = readCellValue(row.getCell(binding.index));
            if (binding.isJoin()) {
                String code = text(raw);
                if (code == null || code.isEmpty()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, binding.title + "不能为空");
                }
                rowMap.put("field_" + binding.detail.getDataName() + "_id",
                        resolveJoinId(wikiMain, binding.target, code));
            } else {
                Object converted = convertDetailValue(binding.detail, raw, binding.title);
                rowMap.put("field_" + binding.detail.getDataName(), converted);
            }
        }
        return rowMap;
    }

    /**
     * 数据合理性校验与值转换：空值返回 null；按明细类型转换 number/date/datetime/time/boolean/enum。
     *
     * @throws BusinessException 类型不合法或枚举值不在范围内时抛出
     */
    private Object convertDetailValue(WikiDataDetailDo detail, Object raw, String title) {
        if (raw == null || (raw instanceof String s && s.isBlank())) {
            return null;
        }
        try {
            return switch (detail.getType()) {
                case "number" -> toDecimal(raw);
                case "date" -> toDate(raw);
                case "datetime" -> toDateTime(raw);
                case "time" -> toTime(raw);
                case "boolean" -> toBoolean(raw);
                case "enum" -> toEnum(text(raw), detail.getEnums());
                default -> text(raw);
            };
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, title + "：" + e.getMessage(), e);
        }
    }

    /**
     * 关联数据存在性校验：在目标动态表中按 field_code 精确匹配，返回目标记录 field_id。
     *
     * @throws BusinessException 目标表不存在（NOT_FOUND）或编号不存在（BAD_REQUEST）
     */
    private String resolveJoinId(WikiMainDo wikiMain, WikiMainDataDo target, String code) {
        String targetTable = DynamicTableSqlBuilder.buildTableName(wikiMain, target);
        if (!wikiDynamicDataDao.tableExists(targetTable)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "关联目标数据项表不存在：" + targetTable);
        }
        List<Map<String, Object>> found = wikiDynamicDataDao.selectByCondition(
                targetTable, "field_code, field_id", null, "field_code = #{code}",
                null, 0, 1, false, Map.of("code", code));
        if (found.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "关联数据编号不存在：" + code);
        }
        Object id = cell(found.get(0), "field_id");
        return id == null ? null : String.valueOf(id);
    }

    // ===== 内部：分批导入与事务 =====

    /**
     * 分批 batchInsert（每批 200 条），批次事务通过 TransactionTemplate 编程式控制（技术方案 6.5）。
     * <ul>
     *   <li>未选失败跳过：批次使用 REQUIRED 加入主事务，失败抛 INTERNAL_ERROR 由主事务整体回滚</li>
     *   <li>选失败跳过：批次使用 REQUIRES_NEW 独立提交，失败批次移除并记录明细继续</li>
     * </ul>
     */
    private void insertBatches(String tableName, List<ParsedRow> rows, boolean skipFail,
                               ImportResult result) {
        if (rows.isEmpty()) {
            return;
        }
        TransactionTemplate batchTx = skipFail ? requiresNewTemplate : requiredTemplate;
        int size = rows.size();
        for (int i = 0; i < size; i += BATCH_SIZE) {
            List<ParsedRow> batch = rows.subList(i, Math.min(i + BATCH_SIZE, size));
            int firstRowNo = batch.get(0).rowNo();
            try {
                batchTx.executeWithoutResult(status -> {
                    List<Map<String, Object>> batchRows = new ArrayList<>(batch.size());
                    for (ParsedRow parsed : batch) {
                        batchRows.add(parsed.row());
                    }
                    wikiDynamicDataDao.batchInsert(tableName, batchRows);
                });
                result.countSuccess(batch.size());
            } catch (RuntimeException e) {
                log.warn("[wiki] 导入批次失败 tableName={} 起始行={} 条数={}",
                        tableName, firstRowNo, batch.size(), e);
                if (!skipFail) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                            "批量导入失败：" + e.getMessage(), e);
                }
                result.addFail(firstRowNo, "批量导入失败：" + e.getMessage());
                result.countSkip(batch.size());
            }
        }
    }

    // ===== 内部：Excel 读写 =====

    /**
     * 打开导入文件工作簿：附件相对路径 + 存储根目录解析物理文件，POI 读取。
     * 读取失败抛 BAD_REQUEST（业务逻辑 7 异常表：导入文件读取失败）。
     */
    private Workbook openWorkbook(String filePath) {
        Path physical;
        try {
            physical = Paths.get(attachmentProperties.getStorageRoot()).resolve(filePath).normalize();
        } catch (InvalidPathException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件路径不合法", e);
        }
        if (!Files.isRegularFile(physical)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件文件不存在：" + filePath);
        }
        try {
            return WorkbookFactory.create(physical.toFile());
        } catch (IOException | EncryptedDocumentException e) {
            log.warn("[wiki] 导入文件读取失败 path={}", filePath, e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件读取失败", e);
        }
    }

    /**
     * 判断数据行是否完全空白（空行跳过，不计入总数）。
     */
    private boolean isBlankRow(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Object value = readCellValue(row.getCell(i));
            if (value == null) {
                continue;
            }
            if (value instanceof String s && s.isBlank()) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * 读取单元格原始值：字符串 / 数值 / 布尔 / 日期（LocalDateTime），公式求值。
     */
    private Object readCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue();
                }
                yield cell.getNumericCellValue();
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> {
                CellValue value = cell.getSheet().getWorkbook().getCreationHelper()
                        .createFormulaEvaluator().evaluate(cell);
                if (value == null) {
                    yield null;
                }
                yield switch (value.getCellType()) {
                    case STRING -> value.getStringValue();
                    case NUMERIC -> value.getNumberValue();
                    case BOOLEAN -> value.getBooleanValue();
                    default -> null;
                };
            }
            default -> null;
        };
    }

    /**
     * 写出单元格值：数值 / 布尔 / 时间类型 / blob 字节数组统一处理。
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date);
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime);
        } else if (value instanceof LocalTime time) {
            cell.setCellValue(time.toString());
        } else if (value instanceof byte[] bytes) {
            cell.setCellValue(new String(bytes, StandardCharsets.UTF_8));
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    // ===== 内部：值转换 =====

    private String text(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String s) {
            return s.trim();
        }
        if (raw instanceof LocalDate d) {
            return d.toString();
        }
        if (raw instanceof LocalDateTime dt) {
            return dt.format(DATE_TIME_FORMATTER);
        }
        if (raw instanceof LocalTime t) {
            return t.toString();
        }
        if (raw instanceof Number n) {
            return new BigDecimal(n.toString()).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(raw).trim();
    }

    private BigDecimal toDecimal(Object raw) {
        if (raw instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        String s = text(raw);
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数字格式不正确：" + s);
        }
    }

    private LocalDate toDate(Object raw) {
        if (raw instanceof LocalDate d) {
            return d;
        }
        if (raw instanceof LocalDateTime dt) {
            return dt.toLocalDate();
        }
        String normalized = normalizeDateText(text(raw));
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "日期格式不正确（应如 2026-09-20）：" + text(raw));
        }
    }

    private LocalDateTime toDateTime(Object raw) {
        if (raw instanceof LocalDateTime dt) {
            return dt;
        }
        if (raw instanceof LocalDate d) {
            return d.atStartOfDay();
        }
        String s = text(raw);
        try {
            return LocalDateTime.parse(s.replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "日期时间格式不正确（应如 2026-09-20 10:00:00）：" + s);
        }
    }

    private LocalTime toTime(Object raw) {
        if (raw instanceof LocalTime t) {
            return t;
        }
        if (raw instanceof LocalDateTime dt) {
            return dt.toLocalTime();
        }
        String s = text(raw);
        try {
            return LocalTime.parse(s);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "时间格式不正确（应如 10:30:00）：" + s);
        }
    }

    private Integer toBoolean(Object raw) {
        if (raw instanceof Boolean b) {
            return b ? 1 : 0;
        }
        String s = text(raw).toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "是".equals(s)) {
            return 1;
        }
        if ("false".equals(s) || "0".equals(s) || "否".equals(s)) {
            return 0;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST,
                "布尔值不正确（应为 true/false 或 1/0）：" + s);
    }

    private String toEnum(String text, List<String> enums) {
        if (enums != null && enums.contains(text)) {
            return text;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "枚举值不在可选范围内：" + text);
    }

    private String normalizeDateText(String s) {
        if (s == null) {
            return "";
        }
        String normalized = s.trim().replace('/', '-');
        int space = normalized.indexOf(' ');
        int tChar = normalized.indexOf('T');
        int cut = space >= 0 ? space : tChar;
        if (cut >= 0) {
            normalized = normalized.substring(0, cut);
        }
        return normalized;
    }

    // ===== 内部：公共 =====

    private void validateJoinType(WikiMainDataDo meta) {
        if (!WikiConstants.DATA_TYPE_JOIN.equals(meta.getFieldDataType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅关联项数据项支持批量导入导出");
        }
    }

    private void ensureTableExists(String tableName) {
        if (!wikiDynamicDataDao.tableExists(tableName)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据项表不存在：" + tableName);
        }
    }

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
}
