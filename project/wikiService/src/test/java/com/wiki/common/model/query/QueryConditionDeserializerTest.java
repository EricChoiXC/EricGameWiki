package com.wiki.common.model.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiki.common.model.request.ApiRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link QueryConditionDeserializer} 能正确解析 {@code docs/common/查询标准.md} 中的样例报文。
 */
class QueryConditionDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializeStandardQuerySample() throws Exception {
        // 查询标准.md 中的样例
        String json = """
                {
                  "query": {
                    "pageNum": 1,
                    "pageSize": 15,
                    "needPage": true,
                    "data": {
                      "and": [
                        {"or": [
                            {"eq": {"fieldName": "张三"}},
                            {"ne": {"fieldCode": "ZhangSan"}}
                          ]},
                        {"or": [
                            {"like": {"fieldAddress": "福建省"}},
                            {"start": {"fieldAddress": "广东省"}}
                          ]}
                      ]
                    }
                  }
                }
                """;

        ApiRequest<Object> request = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(ApiRequest.class, Object.class));

        assertNotNull(request.getQuery());
        assertEquals(1, request.getQuery().getPageNum());
        assertEquals(15, request.getQuery().getPageSize());
        assertTrue(request.getQuery().getNeedPage());

        QueryCondition root = request.getQuery().getData();
        assertNotNull(root);
        assertTrue(root.isGroup());
        assertEquals(LogicalOperator.AND, root.getLogical());
        assertEquals(2, root.getChildren().size());

        QueryCondition firstChild = root.getChildren().get(0);
        assertTrue(firstChild.isGroup());
        assertEquals(LogicalOperator.OR, firstChild.getLogical());
        assertEquals(2, firstChild.getChildren().size());

        QueryCondition eqLeaf = firstChild.getChildren().get(0);
        assertTrue(eqLeaf.isLeaf());
        assertEquals(QueryOperator.EQ, eqLeaf.getOperator());
        assertEquals("fieldName", eqLeaf.getFieldName());
        assertEquals("张三", eqLeaf.getValue());

        QueryCondition neLeaf = firstChild.getChildren().get(1);
        assertEquals(QueryOperator.NE, neLeaf.getOperator());
        assertEquals("fieldCode", neLeaf.getFieldName());
        assertEquals("ZhangSan", neLeaf.getValue());

        QueryCondition secondChild = root.getChildren().get(1);
        assertEquals(LogicalOperator.OR, secondChild.getLogical());

        QueryCondition likeLeaf = secondChild.getChildren().get(0);
        assertEquals(QueryOperator.LIKE, likeLeaf.getOperator());
        assertEquals("福建省", likeLeaf.getValue());

        QueryCondition startLeaf = secondChild.getChildren().get(1);
        assertEquals(QueryOperator.START, startLeaf.getOperator());
        assertEquals("广东省", startLeaf.getValue());
    }

    @Test
    void deserializeInOperator() throws Exception {
        String json = """
                {"in": {"fieldId": ["id1", "id2", "id3"]}}
                """;

        QueryCondition condition = mapper.readValue(json, QueryCondition.class);
        assertTrue(condition.isLeaf());
        assertEquals(QueryOperator.IN, condition.getOperator());
        assertEquals("fieldId", condition.getFieldName());
        assertInstanceOf(List.class, condition.getValue());
        List<?> values = (List<?>) condition.getValue();
        assertEquals(3, values.size());
        assertTrue(values.contains("id1"));
        assertTrue(values.contains("id2"));
        assertTrue(values.contains("id3"));
    }

    @Test
    void deserializeSingleLeaf() throws Exception {
        String json = """
                {"lte": {"fieldAge": 30}}
                """;

        QueryCondition condition = mapper.readValue(json, QueryCondition.class);
        assertTrue(condition.isLeaf());
        assertEquals(QueryOperator.LTE, condition.getOperator());
        assertEquals("fieldAge", condition.getFieldName());
        assertEquals(30, condition.getValue());
    }

    @Test
    void deserializeBetweenOperator() throws Exception {
        String json = """
                {"between": {"fieldName": ["张三", "王五"]}}
                """;

        QueryCondition condition = mapper.readValue(json, QueryCondition.class);
        assertTrue(condition.isLeaf());
        assertEquals(QueryOperator.BETWEEN, condition.getOperator());
        assertEquals("fieldName", condition.getFieldName());
        assertInstanceOf(List.class, condition.getValue());
        List<?> values = (List<?>) condition.getValue();
        assertEquals(2, values.size());
        assertEquals("张三", values.get(0));
        assertEquals("王五", values.get(1));
    }
}
