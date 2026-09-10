package com.wiki.admin.wiki.model.dto;

import lombok.Data;

import java.util.List;

/**
 * wiki 数据项列表/详情响应 VO，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} API-W101 / API-W103 响应。
 * <p>
 * 与 {@link WikiMainDataDo} 的区别：{@code fieldDataJson} 以结构化明细列表承载，
 * 供 Controller 序列化为 JSON 响应报文；DO 中该字段为 JSON 字符串，由 Service 在出参时解析转换。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiMainDataVo {

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

    /** 数据项明细定义列表，驱动动态建表，结构见数据库设计文档 3.2.1 */
    private List<WikiDataDetailDo> fieldDataJson;
}
