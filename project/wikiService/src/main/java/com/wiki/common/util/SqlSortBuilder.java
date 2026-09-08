package com.wiki.common.util;

import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;

import java.util.Map;

/**
 * 排序 SQL 构建器，配合 {@link QueryConditionBuilder} 使用。
 * <p>
 * 排序字段必须经白名单（fieldColumnMap）映射为列名，杜绝 SQL 注入；
 * 排序方向仅允许 {@code asc}/{@code desc}（大小写不敏感）。
 *
 * @author Eric
 * @date 2026/9/8
 */
public final class SqlSortBuilder {

    private SqlSortBuilder() {
    }

    private static final String ASC = "asc";
    private static final String DESC = "desc";

    /**
     * 构建 ORDER BY 片段（不含 "ORDER BY" 失败时返回 null）。
     *
     * @param sortField      Java 属性名
     * @param sortOrder      asc / desc
     * @param fieldColumnMap 字段白名单映射
     * @return 如 {@code ORDER BY field_create_time DESC}；无排序时为 null
     */
    public static String buildOrderBy(String sortField, String sortOrder, Map<String, String> fieldColumnMap) {
        if (sortField == null || sortField.isBlank()) {
            return null;
        }
        String column = fieldColumnMap.get(sortField);
        if (column == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的排序字段: " + sortField);
        }
        String direction = ASC;
        if (sortOrder != null && !sortOrder.isBlank()) {
            String normalized = sortOrder.trim().toLowerCase();
            if (ASC.equals(normalized) || DESC.equals(normalized)) {
                direction = normalized;
            } else {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "排序方向仅支持 asc/desc");
            }
        }
        return "ORDER BY " + column + " " + direction.toUpperCase();
    }
}
