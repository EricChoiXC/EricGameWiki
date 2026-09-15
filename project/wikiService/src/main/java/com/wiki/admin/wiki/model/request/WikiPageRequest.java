package com.wiki.admin.wiki.model.request;

import com.wiki.admin.wiki.model.dto.WikiPageConfigDo;
import lombok.Data;

/**
 * wiki 页面配置保存请求对象，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} API-W302。
 * <p>
 * 一个数据项对应一份页面配置（唯一约束 {@code fieldDataId}），已存在则更新，不存在则新建。
 * {@code fieldWikiPage} 以结构化配置承载，保存时由 Service 序列化为 json 存入 blob 列
 * {@code field_wiki_page}。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiPageRequest {

    /** 数据项 id（入口参数，必填；用于元数据加载与鉴权） */
    private String fieldDataId;

    /** wiki 页面配置（displayInfos / displayFields，结构见数据库设计文档 3.3.1） */
    private WikiPageConfigDo fieldWikiPage;
}
