package com.wiki.admin.wiki.util;

import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link DynamicTableSqlBuilder} 建表 DDL 拼接：
 * 图鉴类固定列 name/code 已由 Service 归一化写入 field_data_json，
 * 动态列生成必须跳过固定列（数据库设计文档 3.2.1）。
 */
class DynamicTableSqlBuilderTest {

    private WikiMainDo main() {
        WikiMainDo wikiMain = new WikiMainDo();
        wikiMain.setFieldId("mainId");
        wikiMain.setFieldSimpleName("re9");
        return wikiMain;
    }

    private WikiMainDataDo dataItem(String dataName, String dataType, String dataJson) {
        WikiMainDataDo data = new WikiMainDataDo();
        data.setFieldId("dataId");
        data.setFieldMainId("mainId");
        data.setFieldName("测试数据项");
        data.setFieldDataName(dataName);
        data.setFieldDataType(dataType);
        data.setFieldDataJson(dataJson);
        return data;
    }

    /**
     * 图鉴类归一化后的 json 含固定列 name/code，建表 DDL 必须跳过而非拒绝，
     * field_name/field_code 仅由固定列生成一次。
     */
    @Test
    void buildCreateTableSkipsFixedNameCodeRows() {
        WikiMainDataDo data = dataItem("item", WikiConstants.DATA_TYPE_DATA, """
                [
                  {"name": "名称", "dataName": "name", "type": "text"},
                  {"name": "编号", "dataName": "code", "type": "text"},
                  {"name": "攻击", "dataName": "atk", "type": "number"},
                  {"name": "介绍", "dataName": "intro", "type": "text"}
                ]
                """);
        String sql = DynamicTableSqlBuilder.buildCreateTable(main(), data);
        assertEquals(1, countOccurrences(sql, "`field_name`"));
        assertEquals(1, countOccurrences(sql, "`field_code`"));
        assertTrue(sql.contains("`field_atk` DECIMAL(20,4)"));
        assertTrue(sql.contains("`field_intro` VARCHAR(200)"));
    }

    /**
     * 非固定列之间简称重复仍需拒绝。
     */
    @Test
    void buildCreateTableRejectsDuplicateDynamicColumn() {
        WikiMainDataDo data = dataItem("item", WikiConstants.DATA_TYPE_DATA, """
                [
                  {"name": "攻击", "dataName": "atk", "type": "number"},
                  {"name": "防御", "dataName": "atk", "type": "number"}
                ]
                """);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DynamicTableSqlBuilder.buildCreateTable(main(), data));
        assertTrue(ex.getMessage().contains("明细行简称重复"));
    }

    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) >= 0) {
            count++;
            index += keyword.length();
        }
        return count;
    }
}
