package com.wiki.admin.wiki.dao;

import com.wiki.admin.wiki.model.dto.WikiPageDo;

/**
 * wiki-项目-数据项-wiki 页面配置 Dao 接口。仅与 {@code WikiPageMapper} 一对一对接，不承载业务逻辑。
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface IWikiPageDao {

    WikiPageDo selectById(String fieldId);

    WikiPageDo selectByDataId(String fieldDataId);

    int insert(WikiPageDo wikiPage);

    int update(WikiPageDo wikiPage);
}
