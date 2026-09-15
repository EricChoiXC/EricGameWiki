package com.wiki.common.util;

import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.query.LogicalOperator;
import com.wiki.common.model.query.QueryCondition;
import com.wiki.common.model.query.QueryOperator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 标准查询条件树 → SQL WHERE 片段构建器，对应 {@code docs/common/查询标准.md}。
 * <p>
 * 输出包含：
 * <ul>
 *   <li>{@code whereSql}：以 {@code #{params.paramN}} 命名占位的 SQL 片段（不含 {@code WHERE} 关键字）</li>
 *   <li>{@code params}：占位符到值的映射</li>
 * </ul>
 * 字段名经白名单（fieldColumnMap）映射为列名，杜绝 SQL 注入；
 * 占位符值由 MyBatis 绑定，遵循 {@code docs/common/业务流转公共规范.md} 4.8 异常流转。
 * <p>
 * 纯函数工具，不依赖业务层 / DB。
 *
 * @author Eric
 * @date 2026/9/8
 */
public final class QueryConditionBuilder {

    private QueryConditionBuilder() {
    }

    /**
     * 构建结果。
     */
    public static class Built {

        /** WHERE 片段（不含 WHERE 关键字），无条件时为 null */
        private final String whereSql;

        /** 命名参数 → 值 */
        private final Map<String, Object> params;

        public Built(String whereSql, Map<String, Object> params) {
            this.whereSql = whereSql;
            this.params = params;
        }

        public String getWhereSql() {
            return whereSql;
        }

        public Map<String, Object> getParams() {
            return params;
        }
    }

    /**
     * 将查询条件树构建为 SQL WHERE 片段。
     *
     * @param condition       查询条件树，可为 null
     * @param fieldColumnMap  Java 属性名 → 数据库列名 的白名单映射
     * @return 构建结果；无条件时 whereSql 为 null
     */
    public static Built build(QueryCondition condition, Map<String, String> fieldColumnMap) {
        Map<String, Object> params = new LinkedHashMap<>();
        Context ctx = new Context(params, fieldColumnMap);
        String sql = condition == null ? null : render(condition, ctx);
        String whereSql = (sql == null || sql.isBlank()) ? null : sql;
        return new Built(whereSql, params);
    }

    private static String render(QueryCondition node, Context ctx) {
        if (node.isGroup()) {
            return renderGroup(node, ctx);
        }
        if (node.isLeaf()) {
            return renderLeaf(node, ctx);
        }
        return null;
    }

    private static String renderGroup(QueryCondition node, Context ctx) {
        List<QueryCondition> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>(children.size());
        for (QueryCondition child : children) {
            String part = render(child, ctx);
            if (part != null && !part.isBlank()) {
                parts.add(part);
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        String joined = String.join(node.getLogical() == LogicalOperator.OR ? " OR " : " AND ", parts);
        return "(" + joined + ")";
    }

    private static String renderLeaf(QueryCondition node, Context ctx) {
        String column = resolveColumn(node.getFieldName(), ctx.fieldColumnMap);
        Object value = node.getValue();
        QueryOperator operator = node.getOperator();
        switch (operator) {
            case EQ:
                return column + " = " + ctx.bind(value);
            case NE:
                return column + " != " + ctx.bind(value);
            case LIKE:
                return column + " LIKE CONCAT('%', " + ctx.bind(value) + ", '%')";
            case START:
                return column + " LIKE CONCAT(" + ctx.bind(value) + ", '%')";
            case END:
                return column + " LIKE CONCAT('%', " + ctx.bind(value) + ")";
            case LT:
                return column + " < " + ctx.bind(value);
            case LTE:
                return column + " <= " + ctx.bind(value);
            case GT:
                return column + " > " + ctx.bind(value);
            case GTE:
                return column + " >= " + ctx.bind(value);
            case IN:
                return column + " IN (" + bindList(ctx, value) + ")";
            case NOT_IN:
                return column + " NOT IN (" + bindList(ctx, value) + ")";
            case BETWEEN:
                return renderBetween(column, ctx, value);
            default:
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的查询操作符: " + operator);
        }
    }

    private static String renderBetween(String column, Context ctx, Object value) {
        List<?> bounds = asList(value);
        if (bounds == null || bounds.size() != 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "between 操作符需要长度为 2 的数组");
        }
        return column + " BETWEEN " + ctx.bind(bounds.get(0)) + " AND " + ctx.bind(bounds.get(1));
    }

    private static String bindList(Context ctx, Object value) {
        List<?> list = asList(value);
        if (list == null || list.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "in/notIn 操作符的值不能为空数组");
        }
        List<String> placeholders = new ArrayList<>(list.size());
        for (Object element : list) {
            placeholders.add(ctx.bind(element));
        }
        return String.join(", ", placeholders);
    }

    @SuppressWarnings("unchecked")
    private static List<?> asList(Object value) {
        if (value instanceof List<?>) {
            return (List<?>) value;
        }
        return null;
    }

    private static String resolveColumn(String fieldName, Map<String, String> fieldColumnMap) {
        if (fieldName == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "查询条件缺少字段名");
        }
        String column = fieldColumnMap.get(fieldName);
        if (column == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的字段: " + fieldName);
        }
        return column;
    }

    private static class Context {

        private final Map<String, Object> params;
        private final Map<String, String> fieldColumnMap;
        private int seq = 0;

        Context(Map<String, Object> params, Map<String, String> fieldColumnMap) {
            this.params = params;
            this.fieldColumnMap = fieldColumnMap;
        }

        String bind(Object value) {
            String name = "p" + (seq++);
            params.put(name, value);
            return "#{params." + name + "}";
        }
    }
}
