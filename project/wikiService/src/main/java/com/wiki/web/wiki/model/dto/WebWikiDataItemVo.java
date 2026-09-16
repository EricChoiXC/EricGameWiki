package com.wiki.web.wiki.model.dto;

import lombok.Data;

/**
 * wiki 前台数据项展示 VO，供 wikiWeb 项目首页数据项列表 / 顶部下拉使用，对应
 * {@code docs/wiki/项目首页.md}。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WebWikiDataItemVo {

    /** 数据项 id */
    private String fieldId;

    /** 所属 wiki 项目 id */
    private String fieldMainId;

    /** 数据项名称 */
    private String fieldName;

    /** 数据项简称，项目内唯一，作为前端页面路径后缀 */
    private String fieldDataName;

    /** 数据项类型（data 图鉴类 / join 关联项 / doc 文档类） */
    private String fieldDataType;
}
