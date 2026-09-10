package com.wiki.admin.wiki.model.dto;

import lombok.Data;

/**
 * wiki-项目-数据项 DO，对应 {@code docs/admin/wiki/wiki数据库设计.md} 3.2 中 {@code wiki_main_data} 表。
 * <p>
 * {@code field_data_json} 存储数据项明细定义数组 json，驱动动态建表，结构见数据库设计文档 3.2.1。
 * 在 DO 层以 String 承载，由 Service 层负责 json 序列化/反序列化。
 * {@code field_data_name} 项目内唯一，限小写英文和数字，作为动态表名后缀。
 * {@code field_data_type} 取值 {@code data}（图鉴类）/ {@code join}（关联项）/ {@code doc}（文档类）。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiMainDataDo {

    /** id */
    private String fieldId;

    /** 所属 wiki 项目 id（关联 wiki_main.field_id） */
    private String fieldMainId;

    /** 数据项名称 */
    private String fieldName;

    /** 数据项简称，项目内唯一，限小写英文和数字，作为动态表名后缀 */
    private String fieldDataName;

    /** 数据项类型（data 图鉴类 / join 关联项 / doc 文档类） */
    private String fieldDataType;

    /** 数据项明细定义，驱动动态建表，结构见数据库设计文档 3.2.1 */
    private String fieldDataJson;
}
