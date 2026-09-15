package com.wiki.admin.wiki.model.response;

import lombok.Data;

import java.util.Map;

/**
 * wiki 数据明细响应 VO，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} 第 6 节。
 * <p>
 * 动态表查询结果为 {@code Map}，经 Service 层映射为本 VO：
 * <ul>
 *   <li>固定列映射为强类型字段：{@code fieldId}、{@code fieldName}、{@code fieldCode}、{@code fieldContext}（文档类）</li>
 *   <li>动态列（含关联列值）映射为 {@code fieldData}（Map，key 小驼峰）</li>
 *   <li>附件类明细存储在 {@code field_data} json 中，合并到 {@code fieldData} 返回</li>
 * </ul>
 * 关联项列表额外在 {@code fieldData} 中返回关联目标显示值（如 {@code fieldEnemyName}/{@code fieldEnemyCode}）。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiDataVo {

    /** 明细记录 id（动态表 field_id） */
    private String fieldId;

    /** 名称（图鉴类/文档类固定列 field_name） */
    private String fieldName;

    /** 编号（图鉴类/文档类固定列 field_code） */
    private String fieldCode;

    /** 内容（文档类固定列 field_context，富文本正文） */
    private String fieldContext;

    /** 动态列值 Map（key 小驼峰，如 fieldPrice / fieldEnemyId / enemyName） */
    private Map<String, Object> fieldData;
}
