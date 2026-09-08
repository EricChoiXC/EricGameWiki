package com.wiki.admin.sys.common.model.dto;

import lombok.Data;

/**
 * 公共服务-配置表 DO，对应 {@code docs/admin/公共服务.md} 中 {@code admin_common_setting} 表。
 * <p>
 * 字段名保持 {@code field} 前缀 lowerCamelCase，由 MyBatis 自动映射数据库列 field_*。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class CommonSettingDo {

    /** id */
    private String fieldId;

    /** 中文名 */
    private String fieldName;

    /** 配置项 */
    private String fieldCode;

    /** 配置值 */
    private String fieldValue;

    /** 默认值 */
    private String fieldDefault;

    /** 配置类型 */
    private String fieldType;
}
