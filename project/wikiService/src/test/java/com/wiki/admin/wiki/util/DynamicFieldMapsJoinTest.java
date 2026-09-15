package com.wiki.admin.wiki.util;

import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.query.LogicalOperator;
import com.wiki.common.model.query.QueryCondition;
import com.wiki.common.model.query.QueryOperator;
import com.wiki.common.util.QueryConditionBuilder;
import com.wiki.common.util.SqlSortBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link DynamicFieldMaps} 关联项联表查询（技术方案 4.5，TASK-W2-01）：
 * 动态 FieldMaps 运行时构建、联表列名携带表别名前缀、
 * 复用 {@link QueryConditionBuilder} / {@link SqlSortBuilder} 渲染带前缀白名单。
 */
class DynamicFieldMapsJoinTest {

    private WikiMainDo main() {
        WikiMainDo wikiMain = new WikiMainDo();
        wikiMain.setFieldId("mainId");
        wikiMain.setFieldSimpleName("re9");
        return wikiMain;
    }

    /**
     * 关联项数据项：敌人掉落（enemy_drop），明细含 enemy(join monster)/item(join item)/drop_rate(number)。
     */
    private WikiMainDataDo joinItem() {
        WikiMainDataDo data = new WikiMainDataDo();
        data.setFieldId("joinId");
        data.setFieldMainId("mainId");
        data.setFieldName("敌人掉落");
        data.setFieldDataName("enemy_drop");
        data.setFieldDataType(WikiConstants.DATA_TYPE_JOIN);
        data.setFieldDataJson("""
                [
                  {"name": "敌人", "dataName": "enemy", "type": "join", "join": "monsterId"},
                  {"name": "道具", "dataName": "item", "type": "join", "join": "itemId"},
                  {"name": "掉落概率", "dataName": "rate", "type": "number"}
                ]
                """);
        return data;
    }

    private WikiMainDataDo target(String id, String dataName, String type) {
        WikiMainDataDo data = new WikiMainDataDo();
        data.setFieldId(id);
        data.setFieldDataName(dataName);
        data.setFieldDataType(type);
        return data;
    }

    @Test
    void buildJoinQueryBuildsPrefixedWhitelist() {
        List<WikiMainDataDo> targets = List.of(
                target("monsterId", "monster", WikiConstants.DATA_TYPE_DATA),
                target("itemId", "item", WikiConstants.DATA_TYPE_DATA));
        DynamicFieldMaps.JoinQuery jq = DynamicFieldMaps.buildJoinQuery(main(), joinItem(), targets);

        Map<String, String> map = jq.getFieldColumnMap();
        // 基础表列携带 base. 前缀
        assertEquals("base.field_id", map.get("fieldId"));
        assertEquals("base.field_enemy_id", map.get("fieldEnemyId"));
        assertEquals("base.field_item_id", map.get("fieldItemId"));
        assertEquals("base.field_rate", map.get("fieldRate"));
        // 关联目标列携带 t1/t2 前缀（关联数据模糊筛选）
        assertEquals("t1.field_name", map.get("fieldEnemyName"));
        assertEquals("t1.field_code", map.get("fieldEnemyCode"));
        assertEquals("t2.field_name", map.get("fieldItemName"));
        assertEquals("t2.field_code", map.get("fieldItemCode"));
    }

    @Test
    void buildJoinQueryBuildsJoinClauses() {
        List<WikiMainDataDo> targets = List.of(
                target("monsterId", "monster", WikiConstants.DATA_TYPE_DATA),
                target("itemId", "item", WikiConstants.DATA_TYPE_DATA));
        DynamicFieldMaps.JoinQuery jq = DynamicFieldMaps.buildJoinQuery(main(), joinItem(), targets);

        List<String> joins = jq.getJoinClauses();
        assertEquals(2, joins.size());
        assertEquals("LEFT JOIN `wiki_re9_monster` t1 ON t1.field_id = base.field_enemy_id", joins.get(0));
        assertEquals("LEFT JOIN `wiki_re9_item` t2 ON t2.field_id = base.field_item_id", joins.get(1));
    }

    @Test
    void buildJoinQueryBuildsSelectColumns() {
        List<WikiMainDataDo> targets = List.of(
                target("monsterId", "monster", WikiConstants.DATA_TYPE_DATA),
                target("itemId", "item", WikiConstants.DATA_TYPE_DATA));
        DynamicFieldMaps.JoinQuery jq = DynamicFieldMaps.buildJoinQuery(main(), joinItem(), targets);

        assertEquals("base.field_id, base.field_enemy_id, "
                + "t1.field_name AS enemy_name, t1.field_code AS enemy_code, "
                + "base.field_item_id, t2.field_name AS item_name, t2.field_code AS item_code, "
                + "base.field_rate, base.field_data", jq.getColumnsSql());
    }

    @Test
    void buildJoinQueryIntegratesWithQueryConditionBuilder() {
        List<WikiMainDataDo> targets = List.of(
                target("monsterId", "monster", WikiConstants.DATA_TYPE_DATA),
                target("itemId", "item", WikiConstants.DATA_TYPE_DATA));
        DynamicFieldMaps.JoinQuery jq = DynamicFieldMaps.buildJoinQuery(main(), joinItem(), targets);

        // 关联数据模糊筛选：enemyName / enemyCode
        QueryCondition root = QueryCondition.group(LogicalOperator.OR, List.of(
                QueryCondition.leaf(QueryOperator.LIKE, "fieldEnemyName", "恶"),
                QueryCondition.leaf(QueryOperator.LIKE, "fieldEnemyCode", "evil")));
        QueryConditionBuilder.Built built = QueryConditionBuilder.build(root, jq.getFieldColumnMap());

        assertTrue(built.getWhereSql().contains("t1.field_name LIKE CONCAT('%', #{params.p0}, '%')"));
        assertTrue(built.getWhereSql().contains("t1.field_code LIKE CONCAT('%', #{params.p1}, '%')"));
        assertEquals("恶", built.getParams().get("p0"));
        assertEquals("evil", built.getParams().get("p1"));
    }

    @Test
    void buildJoinQueryIntegratesWithSqlSortBuilder() {
        List<WikiMainDataDo> targets = List.of(
                target("monsterId", "monster", WikiConstants.DATA_TYPE_DATA),
                target("itemId", "item", WikiConstants.DATA_TYPE_DATA));
        DynamicFieldMaps.JoinQuery jq = DynamicFieldMaps.buildJoinQuery(main(), joinItem(), targets);

        String orderBy = SqlSortBuilder.buildOrderBy("fieldItemId", "desc", jq.getFieldColumnMap());
        assertEquals("ORDER BY base.field_item_id DESC", orderBy);
        // 未在白名单中的字段拒绝
        assertThrows(BusinessException.class,
                () -> SqlSortBuilder.buildOrderBy("fieldEnemy", "asc", jq.getFieldColumnMap()));
    }

    @Test
    void buildJoinQueryRejectsNonJoinBase() {
        WikiMainDataDo dataItem = target("d1", "item", WikiConstants.DATA_TYPE_DATA);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DynamicFieldMaps.buildJoinQuery(main(), dataItem, List.of()));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void buildJoinQueryRejectsMissingTarget() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DynamicFieldMaps.buildJoinQuery(main(), joinItem(), List.of()));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void buildJoinQueryWithoutJoinDetails() {
        WikiMainDataDo join = new WikiMainDataDo();
        join.setFieldId("plainJoinId");
        join.setFieldDataType(WikiConstants.DATA_TYPE_JOIN);
        join.setFieldDataJson("[]");

        DynamicFieldMaps.JoinQuery jq = DynamicFieldMaps.buildJoinQuery(main(), join, List.of());
        assertTrue(jq.getJoinClauses().isEmpty());
        assertEquals("base.field_id, base.field_data", jq.getColumnsSql());
    }

    @Test
    void buildPreservesFixedColumnsByType() {
        WikiMainDataDo data = target("d1", "item", WikiConstants.DATA_TYPE_DATA);
        data.setFieldDataJson("""
                [{"name": "价格", "dataName": "price", "type": "number"}]
                """);
        Map<String, String> map = DynamicFieldMaps.build(data);
        assertEquals("field_id", map.get("fieldId"));
        assertEquals("field_name", map.get("fieldName"));
        assertEquals("field_code", map.get("fieldCode"));
        assertEquals("field_price", map.get("fieldPrice"));

        WikiMainDataDo doc = target("d2", "walkthrough", WikiConstants.DATA_TYPE_DOC);
        Map<String, String> docMap = DynamicFieldMaps.build(doc);
        assertEquals("field_context", docMap.get("fieldContext"));
    }
}
