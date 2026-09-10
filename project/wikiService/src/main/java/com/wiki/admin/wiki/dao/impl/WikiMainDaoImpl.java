package com.wiki.admin.wiki.dao.impl;

import com.wiki.admin.wiki.dao.IWikiMainDao;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.admin.wiki.model.mapper.WikiMainMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * wiki-项目 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Repository
public class WikiMainDaoImpl implements IWikiMainDao {

    private final WikiMainMapper mapper;

    public WikiMainDaoImpl(WikiMainMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public WikiMainDo selectById(String fieldId) {
        return mapper.selectById(fieldId);
    }

    @Override
    public WikiMainDo selectBySimpleName(String fieldSimpleName) {
        return mapper.selectBySimpleName(fieldSimpleName);
    }

    @Override
    public List<WikiMainDo> selectByCondition(String whereSql, String orderBySql, long offset,
                                                int pageSize, boolean needPage, Map<String, Object> params) {
        return mapper.selectByCondition(whereSql, orderBySql, offset, pageSize, needPage, params);
    }

    @Override
    public long countByCondition(String whereSql, Map<String, Object> params) {
        return mapper.countByCondition(whereSql, params);
    }

    @Override
    public int insert(WikiMainDo wikiMain) {
        return mapper.insert(wikiMain);
    }

    @Override
    public int update(WikiMainDo wikiMain) {
        return mapper.update(wikiMain);
    }
}
