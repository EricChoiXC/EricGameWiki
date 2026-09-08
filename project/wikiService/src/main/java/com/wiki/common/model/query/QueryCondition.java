package com.wiki.common.model.query;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.List;

/**
 * 标准查询条件模型，对应 {@code docs/common/查询标准.md} 报文格式。
 * <p>
 * 一个 QueryCondition 节点只能是分组节点或叶子节点：
 * <ul>
 *   <li>分组节点：{@link #logical} + {@link #children}，对应 {@code {"and": [...]}} / {@code {"or": [...]}}</li>
 *   <li>叶子节点：{@link #operator} + {@link #fieldName} + {@link #value}，对应 {@code {"eq": {"fieldName": "张三"}}}</li>
 * </ul>
 * JSON 反序列化由 {@link QueryConditionDeserializer} 处理，支持操作符作为 key 的报文格式。
 */
@Data
@JsonDeserialize(using = QueryConditionDeserializer.class)
public class QueryCondition {

    /** 分组逻辑操作符，仅分组节点非空 */
    private LogicalOperator logical;

    /** 子条件列表，仅分组节点非空 */
    private List<QueryCondition> children;

    /** 叶子操作符，仅叶子节点非空 */
    private QueryOperator operator;

    /** 字段名（Java/JSON 属性名，lowerCamelCase），仅叶子节点非空 */
    private String fieldName;

    /** 比较值；in / notIn 为 List，其他操作符为单值 */
    private Object value;

    /**
     * 构造分组节点。
     */
    public static QueryCondition group(LogicalOperator logical, List<QueryCondition> children) {
        QueryCondition condition = new QueryCondition();
        condition.logical = logical;
        condition.children = children;
        return condition;
    }

    /**
     * 构造叶子节点。
     */
    public static QueryCondition leaf(QueryOperator operator, String fieldName, Object value) {
        QueryCondition condition = new QueryCondition();
        condition.operator = operator;
        condition.fieldName = fieldName;
        condition.value = value;
        return condition;
    }

    public boolean isGroup() {
        return logical != null;
    }

    public boolean isLeaf() {
        return operator != null;
    }
}
