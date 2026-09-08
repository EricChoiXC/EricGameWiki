package com.wiki.common.constant;

/**
 * 通用常量基线，供各模块直接引用。
 * 字段名常量与 {@code docs/common/数据库公共规范.md} 中公共字段命名保持一致。
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /**
     * 公共字段属性名（Java 侧 lowerCamelCase，与 JSON 一致）。
     * 数据库列名经 MyBatis map-underscore-to-camel-case 自动映射到这些属性。
     */
    public static final String FIELD_ID = "fieldId";
    public static final String FIELD_NO = "fieldNo";
    public static final String FIELD_NAME = "fieldName";
    public static final String FIELD_CREATOR_ID = "fieldCreatorId";
    public static final String FIELD_CREATE_TIME = "fieldCreateTime";
    public static final String FIELD_UPDATOR_ID = "fieldUpdatorId";
    public static final String FIELD_UPDATE_TIME = "fieldUpdateTime";
    public static final String FIELD_ENABLE = "fieldEnable";
    public static final String FIELD_VERSION = "fieldVersion";

    /**
     * 启用/禁用码值，对应 field_enable。
     */
    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    /**
     * 鉴权相关请求头与前缀。
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * 链路追踪 MDC 键名。
     */
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_USER_ID = "userId";

    /**
     * 查询条件逻辑操作符 key，与 {@code docs/common/查询标准.md} 对齐。
     */
    public static final String QUERY_LOGICAL_AND = "and";
    public static final String QUERY_LOGICAL_OR = "or";

    /**
     * 默认分页参数。
     */
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 15;
}
