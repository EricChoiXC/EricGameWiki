package com.wiki.admin.wiki.util;

import com.wiki.admin.wiki.model.dto.WikiDataDetailDo;
import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态查询字段白名单构建器，对应 {@code docs/admin/wiki/wiki技术方案.md} 4.1-4.2（ARCH-W02）。
 * <p>
 * 静态表的查询字段白名单（如 {@link WikiFieldMaps}）在编译期确定；动态表的列由元数据运行时决定，
 * 需在运行时由元数据构建 {@code Java 属性名 → 数据库列名} 白名单。
 * <p>
 * 构建结果直接传入 {@code QueryConditionBuilder.build(root, fieldColumnMap)}
 * 与 {@code SqlSortBuilder.buildOrderBy(...)}，未在白名单中的字段查询抛 {@code BAD_REQUEST}。
 * <ul>
 *   <li>列名经白名单正则校验（由 {@link DynamicTableSqlBuilder#buildColumnName} 保证）</li>
 *   <li>图鉴类包含固定列 fieldName/fieldCode</li>
 *   <li>关联项的 join 明细生成 field${DataName}Id 属性</li>
 *   <li>文档类包含固定列 fieldName/fieldCode/fieldContext</li>
 *   <li>关联项联表查询经 {@link #buildJoinQuery} 生成带表别名前缀的白名单与 LEFT JOIN 片段（技术方案 4.5）</li>
 * </ul>
 *
 * @author Eric
 * @date 2026/9/20
 */
public final class DynamicFieldMaps {

    private DynamicFieldMaps() {
    }

    /** 固定列：field_id（所有动态表统一） */
    public static final String FIELD_ID = "fieldId";
    /** 固定列：field_name（图鉴类/文档类） */
    public static final String FIELD_NAME = "fieldName";
    /** 固定列：field_code（图鉴类/文档类） */
    public static final String FIELD_CODE = "fieldCode";
    /** 固定列：field_context（文档类） */
    public static final String FIELD_CONTEXT = "fieldContext";
    /** 附件等非物理列数据：field_data（图鉴类/关联项） */
    public static final String FIELD_DATA = "fieldData";

    /** 联表查询时基础表固定别名（对应技术方案 4.5） */
    public static final String BASE_ALIAS = "base";

    /** 联表查询时关联目标表别名前缀（t1、t2...） */
    private static final String JOIN_ALIAS_PREFIX = "t";

    /**
     * 根据数据项元数据构建查询字段白名单，对应技术方案 4.2。
     * <p>
     * 构建规则：
     * <ul>
     *   <li>所有类型：{@code fieldId → field_id}</li>
     *   <li>图鉴类（data）：{@code fieldName → field_name}、{@code fieldCode → field_code}
     *       + 明细行动态列（排除固定列 name/code、附件类）</li>
     *   <li>关联项（join）：明细行动态列（type=join 生成 {@code field${DataName}Id}）</li>
     *   <li>文档类（doc）：{@code fieldName → field_name}、{@code fieldCode → field_code}、
     *       {@code fieldContext → field_context}</li>
     * </ul>
     *
     * @param wikiMainData 数据项元数据（含已解析的明细列表 + 固定列）
     * @return Java 属性名 → 数据库列名 白名单映射
     */
    public static Map<String, String> build(WikiMainDataDo wikiMainData) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(FIELD_ID, "field_id");

        String dataType = wikiMainData.getFieldDataType();
        switch (dataType) {
            case WikiConstants.DATA_TYPE_DATA -> buildDataMap(map, wikiMainData);
            case WikiConstants.DATA_TYPE_JOIN -> buildJoinMap(map, wikiMainData);
            case WikiConstants.DATA_TYPE_DOC -> buildDocMap(map);
            default -> {
                // 类型不匹配时仅返回 fieldId，由 Service 层兜底校验
            }
        }
        return map;
    }

    /**
     * 构建关联查询场景的带前缀白名单，对应技术方案 4.5。
     * <p>
     * 关联项查询时列名需携带表别名前缀（如 {@code base.field_name}、
     * {@code t1.field_name AS enemy_name}），由本方法在联表场景下生成带前缀的白名单。
     *
     * @param wikiMainData 数据项元数据
     * @param alias        表别名（如 {@code base}、{@code t1}）
     * @return Java 属性名 → 带前缀列名 白名单映射
     */
    public static Map<String, String> buildWithAlias(WikiMainDataDo wikiMainData, String alias) {
        Map<String, String> base = build(wikiMainData);
        Map<String, String> prefixed = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : base.entrySet()) {
            prefixed.put(entry.getKey(), alias + "." + entry.getValue());
        }
        return prefixed;
    }

    // ===== 内部：各类型白名单构建 =====

    /**
     * 图鉴类白名单：固定列 + 动态列。
     */
    private static void buildDataMap(Map<String, String> map, WikiMainDataDo wikiMainData) {
        map.put(FIELD_NAME, "field_name");
        map.put(FIELD_CODE, "field_code");
        map.put(FIELD_DATA, "field_data");
        appendDynamicColumns(map, wikiMainData);
    }

    /**
     * 关联项白名单：动态列（type=join 生成 field${DataName}Id）。
     */
    private static void buildJoinMap(Map<String, String> map, WikiMainDataDo wikiMainData) {
        map.put(FIELD_DATA, "field_data");
        appendDynamicColumns(map, wikiMainData);
    }

    /**
     * 文档类白名单：固定列（无动态列）。
     */
    private static void buildDocMap(Map<String, String> map) {
        map.put(FIELD_NAME, "field_name");
        map.put(FIELD_CODE, "field_code");
        map.put(FIELD_CONTEXT, "field_context");
    }

    /**
     * 追加动态列白名单：排除固定列（name/code）与附件类明细后，每行生成一个属性映射。
     * <p>
     * type=join 的明细生成 {@code field${DataName}Id → field_${dataName}_id}，
     * 其余生成 {@code field${DataName} → field_${dataName}}。
     */
    private static void appendDynamicColumns(Map<String, String> map, WikiMainDataDo wikiMainData) {
        for (WikiDataDetailDo detail : DynamicTableSqlBuilder.parseDetails(wikiMainData.getFieldDataJson())) {
            // 附件类无独立物理列，不加入白名单
            if ("attachment".equals(detail.getType())) {
                continue;
            }
            String dataName = detail.getDataName();
            // 跳过与固定列重名的明细（图鉴类 name/code）
            if ("name".equals(dataName) || "code".equals(dataName)) {
                continue;
            }
            boolean isJoin = WikiConstants.DATA_TYPE_JOIN.equals(detail.getType());
            String javaName = isJoin
                    ? "field" + upperFirst(dataName) + "Id"
                    : "field" + upperFirst(dataName);
            String columnName = isJoin
                    ? "field_" + dataName + "_id"
                    : "field_" + dataName;
            map.put(javaName, columnName);
        }
    }

    /**
     * 首字母大写：用于 dataName → Java 属性名转换。
     */
    private static String upperFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 空白名单常量（兜底）。
     */
    public static Map<String, String> empty() {
        return Collections.emptyMap();
    }

    // ===== 关联项联表查询（技术方案 4.5） =====

    /**
     * 关联项联表查询上下文。
     * <p>
     * 一次构建同时产出三件套，供 {@code WikiDataService}（TASK-W2-02）直接消费：
     * <ul>
     *   <li>{@code fieldColumnMap}：过滤/排序白名单（列名携带 {@code base.} / {@code tN.} 前缀），
     *       直接传入 {@code QueryConditionBuilder.build(...)} 与 {@code SqlSortBuilder.buildOrderBy(...)}</li>
     *   <li>{@code selectColumns}：SELECT 列片段（含 {@code tN.field_name AS enemy_name} 联表列）</li>
     *   <li>{@code joinClauses}：LEFT JOIN 片段，传入 {@code WikiDynamicDataDao}</li>
     * </ul>
     */
    public static class JoinQuery {

        private final Map<String, String> fieldColumnMap;
        private final List<String> selectColumns;
        private final List<String> joinClauses;

        JoinQuery(Map<String, String> fieldColumnMap, List<String> selectColumns, List<String> joinClauses) {
            this.fieldColumnMap = fieldColumnMap;
            this.selectColumns = selectColumns;
            this.joinClauses = joinClauses;
        }

        /**
         * 过滤/排序白名单：Java 属性名 → 带表别名前缀的列名。
         */
        public Map<String, String> getFieldColumnMap() {
            return fieldColumnMap;
        }

        /**
         * SELECT 列片段（如 {@code base.field_id}、{@code t1.field_name AS enemy_name}）。
         */
        public List<String> getSelectColumns() {
            return selectColumns;
        }

        /**
         * LEFT JOIN 片段列表（如 {@code LEFT JOIN `wiki_re9_monster` t1 ON base.field_enemy_id = t1.field_id}）。
         */
        public List<String> getJoinClauses() {
            return joinClauses;
        }

        /**
         * 拼接后的 SELECT 列片段（逗号 + 空格分隔），可直接传入 {@code WikiDynamicDataDao.selectByCondition}。
         */
        public String getColumnsSql() {
            return String.join(", ", selectColumns);
        }
    }

    /**
     * 构建关联项联表查询上下文，对应技术方案 4.5。
     * <p>
     * 基础表别名固定 {@code base}，每个 {@code type=join} 明细按顺序生成 {@code t1}/{@code t2}...
     * 目标表别名，并：
     * <ul>
     *   <li>生成 {@code LEFT JOIN `wiki_${simpleName}_${targetDataName}` tN ON tN.field_id = base.field_${dataName}_id}</li>
     *   <li>在 SELECT 列中追加 {@code tN.field_name AS ${dataName}_name}、{@code tN.field_code AS ${dataName}_code}</li>
     *   <li>在白名单中追加 {@code field${DataName}Name} / {@code field${DataName}Code}（关联数据模糊筛选）</li>
     * </ul>
     * 表名/列名经 {@code DynamicTableSqlBuilder} 白名单正则校验，杜绝 SQL 注入。
     *
     * @param wikiMain     所属项目（提供简称，用于目标动态表名）
     * @param base         关联项数据项元数据（{@code fieldDataType = join}）
     * @param joinTargets  关联目标数据项列表（按明细 {@code join} id 匹配）
     * @return 联表查询上下文
     */
    public static JoinQuery buildJoinQuery(WikiMainDo wikiMain, WikiMainDataDo base,
                                           List<WikiMainDataDo> joinTargets) {
        if (base == null || !WikiConstants.DATA_TYPE_JOIN.equals(base.getFieldDataType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅关联项数据项支持联表查询");
        }
        if (wikiMain == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "联表查询缺少所属项目");
        }
        Map<String, WikiMainDataDo> targetById = new LinkedHashMap<>();
        if (joinTargets != null) {
            for (WikiMainDataDo target : joinTargets) {
                if (target != null && target.getFieldId() != null) {
                    targetById.put(target.getFieldId(), target);
                }
            }
        }

        Map<String, String> fieldMap = new LinkedHashMap<>(buildWithAlias(base, BASE_ALIAS));
        List<String> selectColumns = new ArrayList<>();
        List<String> joinClauses = new ArrayList<>();
        selectColumns.add(BASE_ALIAS + ".field_id");

        int aliasIndex = 1;
        for (WikiDataDetailDo detail : DynamicTableSqlBuilder.parseDetails(base.getFieldDataJson())) {
            String dataName = detail.getDataName();
            if (WikiConstants.DATA_TYPE_JOIN.equals(detail.getType())) {
                WikiMainDataDo target = targetById.get(detail.getJoin());
                if (target == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "关联目标数据项不存在: " + detail.getJoin());
                }
                String alias = JOIN_ALIAS_PREFIX + aliasIndex++;
                String columnId = "field_" + dataName + "_id";
                selectColumns.add(BASE_ALIAS + "." + columnId);
                selectColumns.add(alias + ".field_name AS " + dataName + "_name");
                selectColumns.add(alias + ".field_code AS " + dataName + "_code");
                String targetTable = DynamicTableSqlBuilder.buildTableName(wikiMain, target);
                joinClauses.add("LEFT JOIN `" + targetTable + "` " + alias
                        + " ON " + alias + ".field_id = " + BASE_ALIAS + "." + columnId);
                String prefix = "field" + upperFirst(dataName);
                fieldMap.put(prefix + "Name", alias + ".field_name");
                fieldMap.put(prefix + "Code", alias + ".field_code");
            } else if (!"attachment".equals(detail.getType())) {
                selectColumns.add(BASE_ALIAS + ".field_" + dataName);
            }
        }
        selectColumns.add(BASE_ALIAS + ".field_data");
        return new JoinQuery(fieldMap, selectColumns, joinClauses);
    }
}
