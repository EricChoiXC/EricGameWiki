package com.wiki.admin.wiki.model.mapper;

import com.wiki.admin.wiki.model.dto.WikiPageDo;
import org.apache.ibatis.annotations.Param;

/**
 * wiki-项目-数据项-wiki 页面配置 MyBatis Mapper，对应 {@code wiki_main_data_wiki_page} 表。
 * <p>
 * 每个数据项对应一份页面配置（{@code field_data_id} 唯一）。
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface WikiPageMapper {

    /** 按主键查询 */
    WikiPageDo selectById(@Param("fieldId") String fieldId);

    /** 按数据项 id 查询页面配置（field_data_id 唯一） */
    WikiPageDo selectByDataId(@Param("fieldDataId") String fieldDataId);

    /** 新增页面配置 */
    int insert(WikiPageDo wikiPage);

    /** 更新页面配置 */
    int update(WikiPageDo wikiPage);
}
