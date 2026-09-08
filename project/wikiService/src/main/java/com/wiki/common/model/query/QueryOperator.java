package com.wiki.common.model.query;

/**
 * 标准查询操作符，与 {@code docs/common/查询标准.md} 中操作符 key 一一对应。
 * 用于 {@link QueryCondition} 叶子节点。
 */
public enum QueryOperator {

    /** 等于 */
    EQ("eq"),
    /** 不等于 */
    NE("ne"),
    /** 模糊匹配（两端 %） */
    LIKE("like"),
    /** 开头匹配（后缀 %） */
    START("start"),
    /** 结尾匹配（前缀 %） */
    END("end"),
    /** 小于 */
    LT("lt"),
    /** 小于等于 */
    LTE("lte"),
    /** 大于 */
    GT("gt"),
    /** 大于等于 */
    GTE("gte"),
    /** 集合包含 */
    IN("in"),
    /** 集合不包含 */
    NOT_IN("notIn"),
    /** 区间，值为长度 2 的数组 [下界, 上界] */
    BETWEEN("between");

    private final String key;

    QueryOperator(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * 根据 JSON key 解析操作符，未匹配返回 null。
     */
    public static QueryOperator fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (QueryOperator operator : values()) {
            if (operator.key.equals(key)) {
                return operator;
            }
        }
        return null;
    }
}
