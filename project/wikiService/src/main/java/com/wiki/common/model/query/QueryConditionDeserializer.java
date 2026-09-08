package com.wiki.common.model.query;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * {@link QueryCondition} 自定义反序列化器，解析 {@code docs/common/查询标准.md} 定义的操作符作为 key 的报文。
 * <p>
 * 支持的报文形式：
 * <pre>
 * {"and": [{"or": [{"eq": {"fieldName": "张三"}}]}, ...]}   // 分组节点
 * {"eq": {"fieldName": "张三"}}                                // 叶子节点
 * {"in": {"fieldId": ["id1", "id2"]}}                          // in/notIn 叶子节点
 * </pre>
 */
public class QueryConditionDeserializer extends JsonDeserializer<QueryCondition> {

    @Override
    public QueryCondition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);
        return parseNode(mapper, node);
    }

    private QueryCondition parseNode(ObjectMapper mapper, JsonNode node) throws IOException {
        if (node == null || !node.isObject()) {
            throw new IOException("QueryCondition 必须是 JSON 对象");
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        if (!fields.hasNext()) {
            throw new IOException("QueryCondition 必须包含且仅包含一个操作符 key");
        }
        Map.Entry<String, JsonNode> entry = fields.next();
        if (fields.hasNext()) {
            throw new IOException("QueryCondition 只能包含一个操作符 key，实际包含多个");
        }
        String key = entry.getKey();
        JsonNode value = entry.getValue();

        LogicalOperator logical = LogicalOperator.fromKey(key);
        if (logical != null) {
            if (!value.isArray()) {
                throw new IOException("逻辑操作符 [" + key + "] 的值必须是数组");
            }
            List<QueryCondition> children = new ArrayList<>();
            for (JsonNode child : value) {
                children.add(parseNode(mapper, child));
            }
            return QueryCondition.group(logical, children);
        }

        QueryOperator operator = QueryOperator.fromKey(key);
        if (operator == null) {
            throw new IOException("未知的查询操作符: " + key);
        }

        if (!value.isObject()) {
            throw new IOException("叶子操作符 [" + key + "] 的值必须是字段-值映射对象");
        }
        Iterator<Map.Entry<String, JsonNode>> fieldEntries = value.fields();
        if (!fieldEntries.hasNext()) {
            throw new IOException("叶子操作符 [" + key + "] 的字段-值映射不能为空");
        }
        Map.Entry<String, JsonNode> fieldEntry = fieldEntries.next();
        String fieldName = fieldEntry.getKey();
        Object fieldValue = toValue(fieldEntry.getValue());

        return QueryCondition.leaf(operator, fieldName, fieldValue);
    }

    /**
     * 将 JsonNode 转为 Java 值；数组转为 List，标量转为对应包装类型。
     */
    private Object toValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode element : valueNode) {
                list.add(toValue(element));
            }
            return list;
        }
        if (valueNode.isTextual()) {
            return valueNode.asText();
        }
        if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        }
        if (valueNode.isInt()) {
            return valueNode.asInt();
        }
        if (valueNode.isLong()) {
            return valueNode.asLong();
        }
        if (valueNode.isDouble() || valueNode.isFloat()) {
            return valueNode.asDouble();
        }
        return valueNode.toString();
    }
}
