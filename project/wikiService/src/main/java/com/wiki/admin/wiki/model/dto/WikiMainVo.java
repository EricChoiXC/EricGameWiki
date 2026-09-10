package com.wiki.admin.wiki.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * wiki 项目列表/详情响应 VO，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} API-W001 / API-W003 响应。
 * <p>
 * 与 {@link WikiMainDo} 的区别：{@code fieldManagers} 与 {@code fieldExtend} 以结构化对象承载，
 * 供 Controller 序列化为 JSON 响应报文；DO 中这两个字段为 JSON 字符串，由 Service 在出参时解析转换。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiMainVo {

    /** id */
    private String fieldId;

    /** 名称 */
    private String fieldName;

    /** 英文名称 */
    private String fieldEnName;

    /** 日文名称 */
    private String fieldJpName;

    /** 简称，全局唯一，限小写英文和数字 */
    private String fieldSimpleName;

    /** 发布时间 */
    private LocalDateTime fieldPublishDate;

    /** 创建时间 */
    private LocalDateTime fieldCreateTime;

    /** 开启状态（1 开启 / 0 停用） */
    private Integer fieldStatus;

    /** 拓展信息明细行数组 [{name, type, value}] */
    private List<Map<String, Object>> fieldExtend;

    /** 维护人员 id 数组 ["userId1", "userId2"] */
    private List<String> fieldManagers;
}
