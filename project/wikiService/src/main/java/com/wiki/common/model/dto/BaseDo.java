package com.wiki.common.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库公共字段基类，对应 {@code docs/common/数据库公共规范.md} 中公共字段。
 * <p>
 * 属性名保持 {@code field} 前缀的 lowerCamelCase（如 fieldId），
 * 由 MyBatis map-underscore-to-camel-case 自动映射数据库列 field_id。
 * JSON 交互同样使用这些属性名，遵循 {@code docs/common/接口公共规范.md} JSON 小驼峰约定。
 * <p>
 * 各模块 DO 如需复用公共字段，直接继承本类即可；
 * 模块特有字段在子类中声明。
 */
@Data
public class BaseDo {

    /** 主键，varchar(32) */
    private String fieldId;

    /** 编号，varchar(50) */
    private String fieldNo;

    /** 名称/标题，varchar(200) */
    private String fieldName;

    /** 创建人ID，varchar(32) */
    private String fieldCreatorId;

    /** 创建时间，datetime */
    private LocalDateTime fieldCreateTime;

    /** 更新人ID，varchar(32) */
    private String fieldUpdatorId;

    /** 更新时间，datetime */
    private LocalDateTime fieldUpdateTime;

    /** 是否启用，tinyint（1启用 / 0禁用） */
    private Integer fieldEnable;

    /** 版本号，int，用于乐观锁 */
    private Integer fieldVersion;
}
