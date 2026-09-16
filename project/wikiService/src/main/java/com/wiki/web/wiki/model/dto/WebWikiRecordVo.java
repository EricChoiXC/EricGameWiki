package com.wiki.web.wiki.model.dto;

import lombok.Data;

import java.util.Map;

/**
 * wiki 前台数据明细记录展示 VO，对应 {@code docs/wiki/图鉴类数据项页面.md} 与
 * {@code docs/wiki/文档类页面.md} 的列表 / 详情展示。
 * <p>
 * 图鉴类：{@code fieldName}（名称）+ {@code fieldCode}（编号）+ {@code fieldData} 动态列；
 * 文档类：{@code fieldName}（标题）+ {@code fieldCode}（编号）+ {@code fieldContext}（正文）；
 * 关联项：动态列 {@code fieldData}（含关联目标显示值）。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WebWikiRecordVo {

    /** 明细记录 id（动态表 field_id） */
    private String fieldId;

    /** 名称 / 标题（图鉴类 / 文档类固定列 field_name） */
    private String fieldName;

    /** 编号（图鉴类 / 文档类固定列 field_code） */
    private String fieldCode;

    /** 内容（文档类固定列 field_context，富文本正文） */
    private String fieldContext;

    /** 动态列值 Map（key 小驼峰，如 fieldPrice / fieldEnemyId） */
    private Map<String, Object> fieldData;
}
