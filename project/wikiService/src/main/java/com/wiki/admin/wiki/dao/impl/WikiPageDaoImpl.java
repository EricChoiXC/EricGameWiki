package com.wiki.admin.wiki.dao.impl;

import com.wiki.admin.wiki.dao.IWikiPageDao;
import com.wiki.admin.wiki.model.dto.WikiPageDo;
import com.wiki.admin.wiki.model.mapper.WikiPageMapper;
import org.springframework.stereotype.Repository;

/**
 * wiki-项目-数据项-wiki 页面配置 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Repository
public class WikiPageDaoImpl implements IWikiPageDao {

    private final WikiPageMapper mapper;

    public WikiPageDaoImpl(WikiPageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public WikiPageDo selectById(String fieldId) {
        return mapper.selectById(fieldId);
    }

    @Override
    public WikiPageDo selectByDataId(String fieldDataId) {
        return mapper.selectByDataId(fieldDataId);
    }

    @Override
    public int insert(WikiPageDo wikiPage) {
        return mapper.insert(wikiPage);
    }

    @Override
    public int update(WikiPageDo wikiPage) {
        return mapper.update(wikiPage);
    }
}
