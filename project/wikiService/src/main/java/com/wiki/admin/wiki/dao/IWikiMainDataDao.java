package com.wiki.admin.wiki.dao;

import com.wiki.admin.wiki.model.dto.WikiMainDataDo;

import java.util.List;
import java.util.Map;

/**
 * wiki-项目-数据项 Dao 接口。仅与 {@code WikiMainDataMapper} 一对一对接，不承载业务逻辑。
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface IWikiMainDataDao {

    WikiMainDataDo selectById(String fieldId);

    List<WikiMainDataDo> selectByMainId(String fieldMainId);

    WikiMainDataDo selectByMainIdAndDataName(String fieldMainId, String fieldDataName);

    List<WikiMainDataDo> selectByCondition(String whereSql, String orderBySql, long offset,
                                           int pageSize, boolean needPage, Map<String, Object> params);

    long countByCondition(String whereSql, Map<String, Object> params);

    int insert(WikiMainDataDo wikiMainData);

    int update(WikiMainDataDo wikiMainData);

    int deleteById(String fieldId);
}
