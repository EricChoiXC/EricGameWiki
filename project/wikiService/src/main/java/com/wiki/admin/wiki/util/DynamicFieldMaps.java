package com.wiki.admin.wiki.util;

import com.wiki.admin.wiki.model.dto.WikiDataDetailDo;
import com.wiki.admin.wiki.model.dto.WikiMainDataDo;

import java.util.Collections;
import java.util.LinkedHashMap;
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
}
