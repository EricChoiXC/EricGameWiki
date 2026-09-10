package com.wiki.admin.wiki.model.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * wiki 项目新建/更新请求对象，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} API-W002 / API-W004。
 * <p>
 * {@code fieldExtend} 为拓展信息明细行数组，每行含 name/type/value；
 * {@code fieldManagers} 为维护人员 id 数组；
 * 两者在 Service 层序列化为 JSON 字符串存储到 {@code wiki_main} 表。
 * <p>
 * 遵循 {@code docs/common/接口公共规范.md} 3.5：data 对象仅传入数据库表有的字段。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiMainSaveRequest {

    /** id（更新时必填，新建时可空） */
    private String fieldId;

    /** 名称 */
    private String fieldName;

    /** 英文名称 */
    private String fieldEnName;

    /** 日文名称 */
    private String fieldJpName;

    /** 简称，全局唯一，限小写英文和数字，仅新建时可设置 */
    private String fieldSimpleName;

    /** 发布时间 */
    private LocalDateTime fieldPublishDate;

    /** 开启状态（1 开启 / 0 停用） */
    private Integer fieldStatus;

    /** 维护人员 id 数组 */
    private List<String> fieldManagers;

    /** 拓展信息明细行数组 [{name, type, value}] */
    private List<Map<String, Object>> fieldExtend;
}
