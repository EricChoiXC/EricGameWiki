package com.wiki.admin.wiki.dao.impl;

import com.wiki.admin.wiki.dao.IWikiMainDataDao;
import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.mapper.WikiMainDataMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * wiki-项目-数据项 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Repository
public class WikiMainDataDaoImpl implements IWikiMainDataDao {

    private final WikiMainDataMapper mapper;

    public WikiMainDataDaoImpl(WikiMainDataMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public WikiMainDataDo selectById(String fieldId) {
        return mapper.selectById(fieldId);
    }

    @Override
    public List<WikiMainDataDo> selectByMainId(String fieldMainId) {
        return mapper.selectByMainId(fieldMainId);
    }

    @Override
    public WikiMainDataDo selectByMainIdAndDataName(String fieldMainId, String fieldDataName) {
        return mapper.selectByMainIdAndDataName(fieldMainId, fieldDataName);
    }

    @Override
    public List<WikiMainDataDo> selectByCondition(String whereSql, String orderBySql, long offset,
                                                   int pageSize, boolean needPage, Map<String, Object> params) {
        return mapper.selectByCondition(whereSql, orderBySql, offset, pageSize, needPage, params);
    }

    @Override
    public long countByCondition(String whereSql, Map<String, Object> params) {
        return mapper.countByCondition(whereSql, params);
    }

    @Override
    public int insert(WikiMainDataDo wikiMainData) {
        return mapper.insert(wikiMainData);
    }

    @Override
    public int update(WikiMainDataDo wikiMainData) {
        return mapper.update(wikiMainData);
    }

    @Override
    public int deleteById(String fieldId) {
        return mapper.deleteById(fieldId);
    }
}
