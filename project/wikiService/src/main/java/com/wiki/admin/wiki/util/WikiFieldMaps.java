package com.wiki.admin.wiki.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * wiki 模块字段白名单：Java 属性名 → 数据库列名。
 * <p>
 * 仅允许这些字段出现在 {@code query.data} 查询条件树中以及 {@code sortField} 排序中，
 * 由 {@code QueryConditionBuilder} / {@code SqlSortBuilder} 在渲染 SQL 前校验。
 * <p>
 * 项目列表查询使用联表别名前缀 {@code m.}（wiki_main），
 * 因此查询条件渲染的列名需携带表别名，避免联表时列名歧义。
 *
 * @author Eric
 * @date 2026/9/20
 */
public final class WikiFieldMaps {

    private WikiFieldMaps() {
    }

    /** wiki_main 项目列表查询字段白名单（列名携带 m. 前缀） */
    public static final Map<String, String> MAIN = buildMainMap();

    private static Map<String, String> buildMainMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("fieldId", "m.field_id");
        map.put("fieldName", "m.field_name");
        map.put("fieldEnName", "m.field_en_name");
        map.put("fieldJpName", "m.field_jp_name");
        map.put("fieldSimpleName", "m.field_simple_name");
        map.put("fieldPublishDate", "m.field_publish_date");
        map.put("fieldCreateTime", "m.field_create_time");
        map.put("fieldStatus", "m.field_status");
        map.put("fieldExtend", "m.field_extend");
        map.put("fieldManagers", "m.field_managers");
        return map;
    }
}
