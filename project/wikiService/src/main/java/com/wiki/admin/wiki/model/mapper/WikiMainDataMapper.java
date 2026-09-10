package com.wiki.admin.wiki.model.mapper;

import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * wiki-项目-数据项 MyBatis Mapper，对应 {@code wiki_main_data} 表。
 * <p>
 * 列表查询使用联表别名前缀 {@code m.}（wiki_main_data），与 {@link com.wiki.admin.wiki.util.WikiFieldMaps} 一致。
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface WikiMainDataMapper {

    /** 按主键查询 */
    WikiMainDataDo selectById(@Param("fieldId") String fieldId);

    /** 按项目 id 查询数据项列表 */
    List<WikiMainDataDo> selectByMainId(@Param("fieldMainId") String fieldMainId);

    /** 按项目 id + 数据项简称查询（项目内唯一） */
    WikiMainDataDo selectByMainIdAndDataName(@Param("fieldMainId") String fieldMainId,
                                             @Param("fieldDataName") String fieldDataName);

    /** 通用查询（含动态条件 + 分页 + 排序），whereSql / orderBySql 由 Service 通过 Map 注入 */
    List<WikiMainDataDo> selectByCondition(@Param("whereSql") String whereSql,
                                           @Param("orderBySql") String orderBySql,
                                           @Param("offset") long offset,
                                           @Param("pageSize") int pageSize,
                                           @Param("needPage") boolean needPage,
                                           @Param("params") Map<String, Object> params);

    /** 通用查询对应的总数 */
    long countByCondition(@Param("whereSql") String whereSql, @Param("params") Map<String, Object> params);

    /** 新增数据项 */
    int insert(WikiMainDataDo wikiMainData);

    /** 更新数据项（部分字段） */
    int update(WikiMainDataDo wikiMainData);

    /** 删除数据项 */
    int deleteById(@Param("fieldId") String fieldId);
}
