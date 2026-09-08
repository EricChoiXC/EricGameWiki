package com.wiki.common.model.query;

/**
 * 查询条件逻辑操作符，用于 {@link QueryCondition} 分组节点。
 * 与 {@code docs/common/查询标准.md} 中 and / or 对应。
 */
public enum LogicalOperator {

    /** 逻辑与 */
    AND("and"),
    /** 逻辑或 */
    OR("or");

    private final String key;

    LogicalOperator(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * 根据 JSON key 解析逻辑操作符，未匹配返回 null。
     */
    public static LogicalOperator fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (LogicalOperator operator : values()) {
            if (operator.key.equals(key)) {
                return operator;
            }
        }
        return null;
    }
}
