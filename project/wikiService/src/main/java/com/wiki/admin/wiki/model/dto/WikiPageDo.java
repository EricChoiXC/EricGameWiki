package com.wiki.admin.wiki.model.dto;

import lombok.Data;

/**
 * wiki-项目-数据项-wiki 页面配置 DO，对应 {@code docs/admin/wiki/wiki数据库设计.md} 3.3 中
 * {@code wiki_main_data_wiki_page} 表。
 * <p>
 * {@code field_wiki_page} 存储 wiki 页面配置 json（blob），结构见数据库设计文档 3.3.1，
 * 在 DO 层以 byte[] 承载，由 Service 层负责序列化/反序列化。
 * {@code field_data_id} 唯一约束，一个数据项对应一份页面配置。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiPageDo {

    /** id */
    private String fieldId;

    /** 所属 wiki 项目 id */
    private String fieldMainId;

    /** 数据项 id（关联 wiki_main_data.field_id） */
    private String fieldDataId;

    /** wiki 页面配置 json，结构见数据库设计文档 3.3.1 */
    private byte[] fieldWikiPage;
}
