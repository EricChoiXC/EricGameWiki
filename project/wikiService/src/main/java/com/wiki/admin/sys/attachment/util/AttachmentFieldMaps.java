package com.wiki.admin.sys.attachment.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 附件模块字段白名单：Java 属性名 → 数据库列名。
 * <p>
 * 仅允许这些字段出现在 {@code query.data} 查询条件树中以及 {@code sortField} 排序中，
 * 由 {@code QueryConditionBuilder} / {@code SqlSortBuilder} 在渲染 SQL 前校验。
 * <p>
 * 列表查询使用联表别名前缀 {@code m.}（admin_attachment_main），
 * 因此查询条件渲染的列名需携带表别名，避免联表时列名歧义。
 *
 * @author Eric
 * @date 2026/9/8
 */
public final class AttachmentFieldMaps {

    private AttachmentFieldMaps() {
    }

    /**
     * 附件列表查询字段白名单（联表查询，列名携带 {@code m.} 前缀）。
     * <p>
     * 查询条件仅针对 admin_attachment_main 表字段，避免联表字段歧义。
     */
    public static final Map<String, String> ATTACHMENT_MAIN = buildAttachmentMainMap();

    private static Map<String, String> buildAttachmentMainMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("fieldId", "m.field_id");
        map.put("fieldFileId", "m.field_file_id");
        map.put("fieldName", "m.field_name");
        map.put("fieldCreateTime", "m.field_create_time");
        map.put("fieldUploaderId", "m.field_uploader_id");
        map.put("fieldDeleteFlag", "m.field_delete_flag");
        map.put("fieldOrder", "m.field_order");
        map.put("fieldModelName", "m.field_model_name");
        map.put("fieldModelId", "m.field_model_id");
        map.put("fieldKey", "m.field_key");
        return map;
    }
}
