package com.wiki.admin.wiki.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wiki.admin.wiki.model.dto.WikiDataDetailDo;
import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.util.JsonUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 动态建表 SQL 构建器，对应 {@code docs/admin/wiki/wiki技术方案.md} 3（ARCH-W01）。
 * <p>
 * 纯函数工具：根据 {@link WikiMainDo}（简称）+ {@link WikiMainDataDo}（简称、类型、明细 json）
 * 拼接 {@code CREATE TABLE} / {@code ALTER TABLE ADD COLUMN} / {@code DROP TABLE} SQL，
 * 不依赖业务层 / DB，仅做字符串拼接与白名单校验。
 * <ul>
 *   <li>表名/列名经白名单正则 {@code ^[a-z0-9_]+$} 校验，杜绝 SQL 注入</li>
 *   <li>列类型由 {@link ColumnType} 枚举映射，不直接接受外部输入</li>
 *   <li>DDL 幂等（{@code CREATE TABLE IF NOT EXISTS}）</li>
 *   <li>{@code ALTER TABLE} 仅支持 {@code ADD COLUMN}（首版约束，保护存量数据）</li>
 * </ul>
 *
 * @author Eric
 * @date 2026/9/20
 */
public final class DynamicTableSqlBuilder {

    private DynamicTableSqlBuilder() {
    }

    /** 表名/列名白名单正则：仅小写英文、数字、下划线 */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");

    /** 表名前缀固定 */
    private static final String TABLE_PREFIX = "wiki_";

    // ===== 固定列定义（与数据库设计文档 4.1-4.3 一致） =====

    /** 图鉴类固定列 */
    private static final String DATA_COMMENT = "wiki图鉴类数据表";
    /** 关联项固定列 */
    private static final String JOIN_COMMENT = "wiki关联项数据表";
    /** 文档类固定列 */
    private static final String DOC_COMMENT = "wiki文档类数据表";

    /** 建表尾缀 */
    private static final String TABLE_TAIL =
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='";

    /**
     * 解析数据项明细定义 json 为明细行列表。
     *
     * @param dataJson {@link WikiMainDataDo#getFieldDataJson()}
     * @return 明细行列表，json 为空时返回空列表
     */
    public static List<WikiDataDetailDo> parseDetails(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return Collections.emptyList();
        }
        List<WikiDataDetailDo> details = JsonUtil.fromJson(dataJson,
                new TypeReference<List<WikiDataDetailDo>>() {
                });
        return details == null ? Collections.emptyList() : details;
    }

    /**
     * 构建动态表名：{@code wiki_${simpleName}_${dataName}}。
     *
     * @param wikiMain     项目（提供简称）
     * @param wikiMainData 数据项（提供简称）
     * @return 动态表名
     */
    public static String buildTableName(WikiMainDo wikiMain, WikiMainDataDo wikiMainData) {
        return buildTableName(wikiMain.getFieldSimpleName(), wikiMainData.getFieldDataName());
    }

    /**
     * 构建动态表名：{@code wiki_${simpleName}_${dataName}}。
     * <p>
     * 简称与数据项简称经白名单正则校验。
     *
     * @param simpleName 项目简称
     * @param dataName   数据项简称
     * @return 动态表名
     */
    public static String buildTableName(String simpleName, String dataName) {
        validateName(simpleName, "项目简称");
        validateName(dataName, "数据项简称");
        return TABLE_PREFIX + simpleName + "_" + dataName;
    }

    /**
     * 生成 CREATE TABLE IF NOT EXISTS DDL，对应技术方案 3.5。
     * <p>
     * 按数据项类型生成固定列与动态列：
     * <ul>
     *   <li>{@code data} 图鉴类：field_id + field_name + field_code + 动态列 + field_data</li>
     *   <li>{@code join} 关联项：field_id + 动态列（type=join 生成 field_${dataName}_id）+ field_data</li>
     *   <li>{@code doc} 文档类：field_id + field_name + field_code + field_context（无动态列、无 field_data）</li>
     * </ul>
     *
     * @param wikiMain     项目（提供简称）
     * @param wikiMainData 数据项（提供简称、类型、明细 json）
     * @return CREATE TABLE DDL
     */
    public static String buildCreateTable(WikiMainDo wikiMain, WikiMainDataDo wikiMainData) {
        String tableName = buildTableName(wikiMain, wikiMainData);
        String dataType = validateDataType(wikiMainData.getFieldDataType());
        List<WikiDataDetailDo> details = parseDetails(wikiMainData.getFieldDataJson());

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");

        // 主键列（统一）
        sql.append("    `field_id` VARCHAR(32) NOT NULL COMMENT 'id',\n");

        switch (dataType) {
            case WikiConstants.DATA_TYPE_DATA -> appendDataColumns(sql, details);
            case WikiConstants.DATA_TYPE_JOIN -> appendJoinColumns(sql, details);
            case WikiConstants.DATA_TYPE_DOC -> appendDocColumns(sql);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的数据项类型: " + dataType);
        }

        sql.append("    PRIMARY KEY (`field_id`)\n");
        sql.append(TABLE_TAIL).append(getTableComment(dataType)).append("'");

        return sql.toString();
    }

    /**
     * 生成 ALTER TABLE ADD COLUMN DDL（仅新增列），对应技术方案 3.7。
     * <p>
     * 首版约束：仅支持 ADD COLUMN，保护存量数据。列名经白名单校验。
     *
     * @param tableName 目标表名
     * @param detail    新增明细行
     * @return ALTER TABLE ADD COLUMN DDL
     */
    public static String buildAddColumn(String tableName, WikiDataDetailDo detail) {
        validateName(tableName, "表名");
        validateDetail(detail);
        String columnName = buildColumnName(detail);
        String columnType = mapColumnType(detail.getType());
        String comment = detail.getName() == null ? "" : escapeComment(detail.getName());
        return "ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName
                + "` " + columnType + " COMMENT '" + comment + "'";
    }

    /**
     * 生成 DROP TABLE DDL。
     *
     * @param tableName 目标表名
     * @return DROP TABLE DDL
     */
    public static String buildDropTable(String tableName) {
        validateName(tableName, "表名");
        return "DROP TABLE IF EXISTS `" + tableName + "`";
    }

    // ===== 内部：列拼接 =====

    /**
     * 图鉴类列拼接：field_name + field_code + 动态列 + field_data。
     */
    private static void appendDataColumns(StringBuilder sql, List<WikiDataDetailDo> details) {
        sql.append("    `field_name` VARCHAR(200) COMMENT '名称',\n");
        sql.append("    `field_code` VARCHAR(200) COMMENT '编号',\n");
        Set<String> fixedNames = Set.of("name", "code");
        appendDynamicColumns(sql, details, fixedNames);
        sql.append("    `field_data` JSON COMMENT '附件等非物理列数据',\n");
    }

    /**
     * 关联项列拼接：动态列（type=join 生成 field_${dataName}_id）+ field_data。
     */
    private static void appendJoinColumns(StringBuilder sql, List<WikiDataDetailDo> details) {
        appendDynamicColumns(sql, details, Collections.emptySet());
        sql.append("    `field_data` JSON COMMENT '附件等非物理列数据',\n");
    }

    /**
     * 文档类列拼接：field_name + field_code + field_context（无动态列、无 field_data）。
     */
    private static void appendDocColumns(StringBuilder sql) {
        sql.append("    `field_name` VARCHAR(200) COMMENT '标题',\n");
        sql.append("    `field_code` VARCHAR(200) COMMENT '编号',\n");
        sql.append("    `field_context` LONGBLOB COMMENT '内容',\n");
    }

    /**
     * 动态列拼接：排除固定列与附件类明细后的每行明细生成一列。
     *
     * @param sql        SQL 构建器
     * @param details    明细行列表
     * @param fixedNames 固定列简称集合（重名时拒绝）
     */
    private static void appendDynamicColumns(StringBuilder sql, List<WikiDataDetailDo> details,
                                             Set<String> fixedNames) {
        Set<String> seen = new HashSet<>();
        for (WikiDataDetailDo detail : details) {
            validateDetail(detail);
            String dataName = detail.getDataName();
            // 固定列重名校验（图鉴类 name/code 由后端自动补充，明细行不应再出现）
            if (fixedNames.contains(dataName)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "明细行简称与固定列重名: " + dataName);
            }
            // 重复列名校验
            if (!seen.add(dataName)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "明细行简称重复: " + dataName);
            }
            String columnName = buildColumnName(detail);
            String columnType = mapColumnType(detail.getType());
            String comment = detail.getName() == null ? "" : escapeComment(detail.getName());
            sql.append("    `").append(columnName).append("` ")
                    .append(columnType).append(" COMMENT '").append(comment).append("',\n");
        }
    }

    /**
     * 构建动态列名：type=join 生成 {@code field_${dataName}_id}，其余 {@code field_${dataName}}。
     */
    private static String buildColumnName(WikiDataDetailDo detail) {
        String dataName = detail.getDataName();
        validateName(dataName, "明细行简称");
        if (WikiConstants.DATA_TYPE_JOIN.equals(detail.getType())) {
            return "field_" + dataName + "_id";
        }
        return "field_" + dataName;
    }

    /**
     * 数据类型 → MySQL 列类型映射，对应数据库设计文档 4.4。
     */
    private static String mapColumnType(String type) {
        ColumnType columnType = ColumnType.fromCode(type);
        if (columnType == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的数据类型: " + type);
        }
        return columnType.getColumnType();
    }

    /**
     * 校验明细行：简称、类型必填，join 类型必须有 join 目标。
     */
    private static void validateDetail(WikiDataDetailDo detail) {
        if (detail == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "明细行不能为空");
        }
        if (detail.getDataName() == null || detail.getDataName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "明细行简称不能为空");
        }
        if (detail.getType() == null || detail.getType().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "明细行数据类型不能为空");
        }
        if (WikiConstants.DATA_TYPE_JOIN.equals(detail.getType())
                && (detail.getJoin() == null || detail.getJoin().isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "关联类明细行必须指定关联目标数据项");
        }
        validateName(detail.getDataName(), "明细行简称");
    }

    /**
     * 校验数据项类型合法。
     */
    private static String validateDataType(String dataType) {
        if (dataType == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据项类型不能为空");
        }
        if (!WikiConstants.DATA_TYPE_DATA.equals(dataType)
                && !WikiConstants.DATA_TYPE_JOIN.equals(dataType)
                && !WikiConstants.DATA_TYPE_DOC.equals(dataType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的数据项类型: " + dataType);
        }
        return dataType;
    }

    /**
     * 白名单正则校验名称（表名/列名/简称）。
     */
    private static void validateName(String name, String label) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不能为空");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    label + "仅支持小写英文、数字和下划线: " + name);
        }
    }

    /**
     * 转义 COMMENT 中的单引号，防止注入。
     */
    private static String escapeComment(String comment) {
        return comment.replace("'", "''");
    }

    private static String getTableComment(String dataType) {
        return switch (dataType) {
            case WikiConstants.DATA_TYPE_DATA -> DATA_COMMENT;
            case WikiConstants.DATA_TYPE_JOIN -> JOIN_COMMENT;
            case WikiConstants.DATA_TYPE_DOC -> DOC_COMMENT;
            default -> "wiki动态数据表";
        };
    }

    /**
     * 数据类型 → MySQL 列类型枚举，对应数据库设计文档 4.4。
     */
    private enum ColumnType {
        TEXT("text", "VARCHAR(200)"),
        BLOB("blob", "LONGBLOB"),
        NUMBER("number", "DECIMAL(20,4)"),
        DATE("date", "DATE"),
        DATETIME("datetime", "DATETIME"),
        TIME("time", "TIME"),
        BOOLEAN("boolean", "TINYINT"),
        ENUM("enum", "VARCHAR(50)"),
        JOIN("join", "VARCHAR(32)"),
        // attachment 无独立物理列（存入 field_data json），不参与动态列生成
        ATTACHMENT("attachment", null);

        private final String code;
        private final String columnType;

        ColumnType(String code, String columnType) {
            this.code = code;
            this.columnType = columnType;
        }

        public String getColumnType() {
            return columnType;
        }

        static ColumnType fromCode(String code) {
            for (ColumnType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }
}
