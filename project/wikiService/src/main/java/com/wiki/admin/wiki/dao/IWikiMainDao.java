package com.wiki.admin.wiki.dao;

import com.wiki.admin.wiki.model.dto.WikiMainDo;

import java.util.List;
import java.util.Map;

/**
 * wiki-项目 Dao 接口。仅与 {@code WikiMainMapper} 一对一对接，不承载业务逻辑。
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface IWikiMainDao {

    WikiMainDo selectById(String fieldId);

    WikiMainDo selectBySimpleName(String fieldSimpleName);

    List<WikiMainDo> selectByCondition(String whereSql, String orderBySql, long offset,
                                       int pageSize, boolean needPage, Map<String, Object> params);

    long countByCondition(String whereSql, Map<String, Object> params);

    int insert(WikiMainDo wikiMain);

    int update(WikiMainDo wikiMain);
}
