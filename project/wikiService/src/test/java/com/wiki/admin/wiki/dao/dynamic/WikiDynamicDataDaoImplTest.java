package com.wiki.admin.wiki.dao.dynamic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 {@link WikiDynamicDataDaoImpl#renderFrom} 的联表 FROM 子句拼接
 * （技术方案 4.5，TASK-W2-01）：联表时基础表使用别名 {@code base}。
 */
class WikiDynamicDataDaoImplTest {

    @Test
    void renderFromWithoutJoins() {
        assertEquals(" FROM `wiki_re9_item`", WikiDynamicDataDaoImpl.renderFrom("wiki_re9_item", null));
        assertEquals(" FROM `wiki_re9_item`", WikiDynamicDataDaoImpl.renderFrom("wiki_re9_item", List.of()));
    }

    @Test
    void renderFromWithJoins() {
        List<String> joins = List.of(
                "LEFT JOIN `wiki_re9_monster` t1 ON t1.field_id = base.field_enemy_id",
                "LEFT JOIN `wiki_re9_item` t2 ON t2.field_id = base.field_item_id");
        String expected = " FROM `wiki_re9_enemy_drop` base"
                + " LEFT JOIN `wiki_re9_monster` t1 ON t1.field_id = base.field_enemy_id"
                + " LEFT JOIN `wiki_re9_item` t2 ON t2.field_id = base.field_item_id";
        assertEquals(expected, WikiDynamicDataDaoImpl.renderFrom("wiki_re9_enemy_drop", joins));
    }

    @Test
    void renderFromIgnoresBlankJoinClauses() {
        List<String> joins = List.of("", "LEFT JOIN `wiki_re9_monster` t1 ON t1.field_id = base.field_enemy_id");
        assertEquals(" FROM `wiki_re9_enemy_drop` base"
                + " LEFT JOIN `wiki_re9_monster` t1 ON t1.field_id = base.field_enemy_id",
                WikiDynamicDataDaoImpl.renderFrom("wiki_re9_enemy_drop", joins));
    }
}
