package com.wiki.admin.wiki.model.mapper;

import com.wiki.admin.wiki.model.dto.WikiMainDo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * wiki-项目 MyBatis Mapper，对应 {@code wiki_main} 表。
 * <p>
 * 列表查询使用联表别名前缀 {@code m.}（wiki_main），与 {@link com.wiki.admin.wiki.util.WikiFieldMaps} 一致。
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface WikiMainMapper {

    /** 按主键查询 */
    WikiMainDo selectById(@Param("fieldId") String fieldId);

    /** 按简称查询（简称全局唯一） */
    WikiMainDo selectBySimpleName(@Param("fieldSimpleName") String fieldSimpleName);

    /** 通用查询（含动态条件 + 分页 + 排序），whereSql / orderBySql 由 Service 通过 Map 注入 */
    List<WikiMainDo> selectByCondition(@Param("whereSql") String whereSql,
                                       @Param("orderBySql") String orderBySql,
                                       @Param("offset") long offset,
                                       @Param("pageSize") int pageSize,
                                       @Param("needPage") boolean needPage,
                                       @Param("params") Map<String, Object> params);

    /** 通用查询对应的总数 */
    long countByCondition(@Param("whereSql") String whereSql, @Param("params") Map<String, Object> params);

    /** 新增项目 */
    int insert(WikiMainDo wikiMain);

    /** 更新项目（部分字段） */
    int update(WikiMainDo wikiMain);
}
