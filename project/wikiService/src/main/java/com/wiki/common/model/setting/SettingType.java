package com.wiki.common.model.setting;

/**
 * 配置项类型枚举，与 {@code docs/common/yml配置文件内容规范.md} 中 setting.yml 的 TYPE 字段值一一对应。
 * <p>
 * 用于 {@code admin_common_setting} 表 {@code field_type} 列的值域约束，
 * 以及配置项读取时按类型将字符串值转换为对应 Java 类型。
 */
public enum SettingType {

    /** 整数 */
    INTEGER("integer"),
    /** 字符串 */
    STRING("string"),
    /** 布尔 */
    BOOLEAN("boolean"),
    /** 日期（yyyy-MM-dd） */
    DATE("date"),
    /** 日期时间（yyyy-MM-dd HH:mm:ss） */
    DATETIME("datetime"),
    /** 时间（HH:mm:ss） */
    TIME("time"),
    /** 浮点数 */
    FLOAT("float");

    private final String key;

    SettingType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * 根据 setting.yml 中的 TYPE 值解析枚举，未匹配返回 null。
     */
    public static SettingType fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (SettingType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
