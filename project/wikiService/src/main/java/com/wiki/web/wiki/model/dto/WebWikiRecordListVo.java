package com.wiki.web.wiki.model.dto;

import lombok.Data;

import java.util.List;

/**
 * wiki 前台数据明细列表响应，对应数据项列表页
 * （{@code /wiki/${fieldSimpleName}/${fieldDataName}}）。
 * <p>
 * 同时返回数据项元数据（前端据 {@code fieldDataType} 决定渲染形态）与记录列表。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WebWikiRecordListVo {

    /** 数据项元数据 */
    private WebWikiDataItemVo dataItem;

    /** 数据明细记录列表 */
    private List<WebWikiRecordVo> records;
}
