package com.wiki.web.wiki.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * wiki 前台项目展示 VO，供 wikiWeb 首页 / 项目首页使用，对应
 * {@code docs/wiki/首页.md} 与 {@code docs/wiki/项目首页.md}。
 * <p>
 * 仅暴露对外可读字段，维护人员（fieldManagers）等内部字段不返回。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WebWikiProjectVo {

    /** id */
    private String fieldId;

    /** 名称 */
    private String fieldName;

    /** 英文名称 */
    private String fieldEnName;

    /** 日文名称 */
    private String fieldJpName;

    /** 简称，全局唯一，用于前端页面路径 */
    private String fieldSimpleName;

    /** 发布时间 */
    private LocalDateTime fieldPublishDate;

    /** 拓展信息明细行数组 [{name, type, value}] */
    private List<Map<String, Object>> fieldExtend;
}
