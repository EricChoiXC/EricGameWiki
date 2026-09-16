package com.wiki.web.wiki.model.dto;

import com.wiki.admin.wiki.model.dto.WikiPageConfigDo;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * wiki 前台数据明细详情响应，对应数据项详情页
 * （{@code /wiki/${fieldSimpleName}/${fieldDataName}/${fieldId}}）。
 * <p>
 * 图鉴类按 {@code pageConfig}（wiki_main_data_wiki_page）配置以表格展示；
 * 文档类展示标题与正文。{@code relatedRecords} 供关联展示块（displayInfos 中
 * {@code type=join}）渲染使用，键为关联类数据项 id。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WebWikiRecordDetailVo {

    /** 数据项元数据 */
    private WebWikiDataItemVo dataItem;

    /** 数据明细记录 */
    private WebWikiRecordVo record;

    /** wiki 页面配置（未配置时为 null） */
    private WikiPageConfigDo pageConfig;

    /** 引用本记录的关联项记录，键为关联类数据项 id，值为关联记录列表 */
    private Map<String, List<WebWikiRecordVo>> relatedRecords;
}
