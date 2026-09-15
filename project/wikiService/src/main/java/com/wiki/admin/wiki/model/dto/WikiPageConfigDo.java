package com.wiki.admin.wiki.model.dto;

import lombok.Data;

import java.util.List;

/**
 * wiki 数据项页面配置结构，对应 {@code wiki_main_data_wiki_page.field_wiki_page} 的 json 结构，
 * 见 {@code docs/admin/wiki/wiki数据库设计.md} 3.3.1 与 {@code docs/admin/wiki/wiki业务逻辑.md} 6.4（FLOW-W004）。
 * <p>
 * 结构说明：
 * <ul>
 *   <li>{@code displayInfos}：显示信息项列表。{@code type=self} 为本数据项明细，{@code type=join}
 *       为包含该数据项的关联类数据项；每项的 {@code fields} 为要显示的字段列表（属性键，如 fieldName）</li>
 *   <li>{@code displayFields}：显示字段项列表。选择"关联类"数据项时按序选择要显示的关联类数据项的数据明细项</li>
 * </ul>
 * 一个数据项对应一份页面配置（{@code field_data_id} 唯一约束）。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiPageConfigDo {

    /** 显示信息项列表 */
    private List<DisplayInfo> displayInfos;

    /** 显示字段项列表 */
    private List<DisplayField> displayFields;

    /**
     * 显示信息项：{@code type} 取值 self（本数据项明细）/ join（包含该数据项的关联类数据项）。
     */
    @Data
    public static class DisplayInfo {

        /** 显示信息来源类型：self / join */
        private String type;

        /** 数据项 id（self 为本数据项 id；join 为关联类数据项 id） */
        private String fieldDataId;

        /** 显示的字段列表（属性键，如 fieldName / fieldCode / fieldPrice） */
        private List<String> fields;
    }

    /**
     * 显示字段项：选择"关联类"数据项后按序选择要显示的数据明细项。
     */
    @Data
    public static class DisplayField {

        /** 关联类数据项 id */
        private String fieldDataId;

        /** 要显示的关联类数据项数据明细项（属性键） */
        private List<String> fields;
    }
}
