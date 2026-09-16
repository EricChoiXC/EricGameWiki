package com.wiki.web.wiki.model.dto;

import lombok.Data;

/**
 * wiki 前台数据项字段元数据展示 VO，由 {@code wiki_main_data.field_data_json}
 * 明细行转换而来，供详情页以字段显示名（{@code name}）替换属性键兜底标签。
 * <p>
 * {@code fieldKey} 为 wiki 页面配置 {@code fields} 数组使用的属性键
 * （小驼峰，type=join 的明细为 {@code field${DataName}Id}），
 * 与 {@code WebWikiRecordVo} 顶层属性 / {@code fieldData} 键一一对应。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WebWikiDetailVo {

    /** 明细列简称（field_data_json.dataName） */
    private String dataName;

    /** 明细列显示名（field_data_json.name） */
    private String name;

    /** 数据类型（field_data_json.type） */
    private String type;

    /** 属性键（页面配置 fields 中使用的字段，如 fieldName / fieldPrice / fieldEnemyId） */
    private String fieldKey;
}
