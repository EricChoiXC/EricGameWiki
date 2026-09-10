package com.wiki.admin.wiki.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * wiki-项目 DO，对应 {@code docs/admin/wiki/wiki数据库设计.md} 3.1 中 {@code wiki_main} 表。
 * <p>
 * {@code field_extend} 存储拓展信息明细行数组 json（{@code [{name, type, value}]}），
 * {@code field_managers} 存储维护人员 id 数组 json（{@code ["userId1", "userId2"]}），
 * 两个字段在 DO 层以 String 承载，由 Service 层负责 json 序列化/反序列化。
 * {@code field_simple_name} 全局唯一，限小写英文和数字，作为动态表名前缀。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiMainDo {

    /** id */
    private String fieldId;

    /** 名称 */
    private String fieldName;

    /** 英文名称 */
    private String fieldEnName;

    /** 日文名称 */
    private String fieldJpName;

    /** 简称，全局唯一，限小写英文和数字，作为动态表名前缀 */
    private String fieldSimpleName;

    /** 发布时间 */
    private LocalDateTime fieldPublishDate;

    /** 创建时间 */
    private LocalDateTime fieldCreateTime;

    /** 开启状态（1 开启 / 0 停用） */
    private Integer fieldStatus;

    /** 扩展信息，存储拓展信息明细行数组 json [{name, type, value}] */
    private String fieldExtend;

    /** 维护人员，存储 AdminOrgUser id 数组 json ["userId1", "userId2"] */
    private String fieldManagers;
}
