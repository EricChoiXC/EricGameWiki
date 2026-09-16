package com.wiki.web.wiki.model.dto;

import lombok.Data;

import java.util.List;

/**
 * wiki 前台引用本记录的关联类数据项展示 VO，供详情页关联展示块
 * （displayInfos 中 {@code type=join}）使用：{@code fieldName} 作为块标题，
 * {@code details} 提供关联字段的显示名（{@code fieldKey} → {@code name}）。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WebWikiJoinItemVo {

    /** 关联类数据项 id */
    private String fieldId;

    /** 关联类数据项名称（wiki_main_data.field_name） */
    private String fieldName;

    /** 关联类数据项简称 */
    private String fieldDataName;

    /** 关联类数据项类型 */
    private String fieldDataType;

    /** 关联类数据项字段元数据（含 fieldKey → name） */
    private List<WebWikiDetailVo> details;
}
