package com.wiki.admin.wiki.model.request;

import com.wiki.admin.wiki.model.dto.WikiDataDetailDo;
import lombok.Data;

import java.util.List;

/**
 * wiki 数据项新建/更新请求对象，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} API-W102 / API-W104。
 * <p>
 * 新建时：{@code fieldMainId}、{@code fieldName}、{@code fieldDataName}、{@code fieldDataType} 必填；
 * 图鉴类（data）/关联项（join）需传 {@code fieldDataJson}；文档类（doc）不传 {@code fieldDataJson}。
 * <p>
 * 更新时：{@code fieldId} 必填；{@code fieldDataName}、{@code fieldDataType} 不可改，更新请求中传原值；
 * 明细变更仅允许新增列（{@code ALTER TABLE ADD COLUMN}），首版禁止删除/修改已存在列。
 * <p>
 * 图鉴类固定列 {@code name}（名称）、{@code code}（编号）由后端自动补充到 {@code fieldDataJson} 前部，前端不传。
 * <p>
 * 遵循 {@code docs/common/接口公共规范.md} 3.5：data 对象仅传入数据库表有的字段。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiMainDataSaveRequest {

    /** id（更新时必填，新建时可空） */
    private String fieldId;

    /** 所属 wiki 项目 id（新建时必填） */
    private String fieldMainId;

    /** 数据项名称 */
    private String fieldName;

    /** 数据项简称，项目内唯一，限小写英文和数字，仅新建时可设置 */
    private String fieldDataName;

    /** 数据项类型（data 图鉴类 / join 关联项 / doc 文档类），仅新建时可设置 */
    private String fieldDataType;

    /** 数据项明细定义列表，驱动动态建表；文档类不传 */
    private List<WikiDataDetailDo> fieldDataJson;
}
