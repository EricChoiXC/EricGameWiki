package com.wiki.admin.wiki.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * wiki 数据项模块字段白名单：Java 属性名 → 数据库列名。
 * <p>
 * 仅允许这些字段出现在 {@code query.data} 查询条件树中以及 {@code sortField} 排序中，
 * 由 {@code QueryConditionBuilder} / {@code SqlSortBuilder} 在渲染 SQL 前校验。
 * <p>
 * 数据项列表查询使用联表别名前缀 {@code m.}（wiki_main_data），
 * 因此查询条件渲染的列名需携带表别名，避免联表时列名歧义。
 *
 * @author Eric
 * @date 2026/9/20
 */
public final class WikiMainDataFieldMaps {

    private WikiMainDataFieldMaps() {
    }

    /** wiki_main_data 数据项列表查询字段白名单（列名携带 m. 前缀） */
    public static final Map<String, String> MAIN_DATA = buildMainDataMap();

    private static Map<String, String> buildMainDataMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("fieldId", "m.field_id");
        map.put("fieldMainId", "m.field_main_id");
        map.put("fieldName", "m.field_name");
        map.put("fieldDataName", "m.field_data_name");
        map.put("fieldDataType", "m.field_data_type");
        return map;
    }
}
