package com.wiki.admin.wiki.model.dto;

import lombok.Data;

import java.util.List;

/**
 * wiki 数据项明细行 DTO，对应 {@code wiki_main_data.field_data_json} 数组中的单行。
 * <p>
 * 每行明细对应动态表一列（{@code attachment} 类型除外），结构见
 * {@code docs/admin/wiki/wiki数据库设计.md} 3.2.1。
 * <ul>
 *   <li>{@code name}：明细列显示名，必填，作为动态列 COMMENT</li>
 *   <li>{@code dataName}：简称，限英文/数字/下划线，生成列名 {@code field_${dataName}}</li>
 *   <li>{@code type}：数据类型，见 {@code docs/admin/wiki/wiki业务逻辑.md} 4.3</li>
 *   <li>{@code enums}：枚举项，仅 {@code type=enum}</li>
 *   <li>{@code join}：关联目标数据项 id，仅 {@code type=join}</li>
 * </ul>
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiDataDetailDo {

    /** 明细列显示名 */
    private String name;

    /** 简称，限英文/数字/下划线，生成列名 field_${dataName} */
    private String dataName;

    /** 数据类型（text/blob/number/date/datetime/time/boolean/enum/attachment/join） */
    private String type;

    /** 枚举项，仅 type=enum */
    private List<String> enums;

    /** 关联目标数据项 id，仅 type=join */
    private String join;
}
