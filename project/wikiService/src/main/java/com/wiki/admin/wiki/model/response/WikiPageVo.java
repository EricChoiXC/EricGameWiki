package com.wiki.admin.wiki.model.response;

import com.wiki.admin.wiki.model.dto.WikiPageConfigDo;
import lombok.Data;

/**
 * wiki 页面配置响应 VO，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} API-W301。
 * <p>
 * {@code fieldWikiPage} 为结构化配置（displayInfos / displayFields），由 Service 从
 * blob 列 {@code field_wiki_page} 反序列化得到。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiPageVo {

    /** 配置 id */
    private String fieldId;

    /** 数据项 id */
    private String fieldDataId;

    /** wiki 页面配置（结构见数据库设计文档 3.3.1） */
    private WikiPageConfigDo fieldWikiPage;
}
