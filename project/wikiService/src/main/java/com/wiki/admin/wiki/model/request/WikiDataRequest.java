package com.wiki.admin.wiki.model.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * wiki 数据明细维护请求对象，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} 第 6 节。
 * <p>
 * 数据明细接口以 {@code fieldDataId}（数据项 id）为入口，后端加载数据项元数据后
 * 根据 {@code fieldDataType} 分发处理（图鉴类/关联项/文档类）。
 * <p>
 * 动态列值统一放入 {@code fieldData} Map，key 使用小驼峰（如 {@code fieldPrice}、
 * {@code fieldEnemyId}），与前端 JSON 交互命名一致（AGENTS.md 7.4）。
 * 图鉴类/文档类固定列使用强类型字段 {@code fieldName}/{@code fieldCode}/{@code fieldContext}。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class WikiDataRequest {

    /** 数据项 id（入口参数，必填；用于元数据加载与鉴权） */
    private String fieldDataId;

    /** 明细记录 id（动态表 field_id，update/delete 必填，save 新建时可空） */
    private String fieldId;

    /** 名称（图鉴类/文档类固定列 field_name） */
    private String fieldName;

    /** 编号（图鉴类/文档类固定列 field_code） */
    private String fieldCode;

    /** 内容（文档类固定列 field_context，富文本正文） */
    private String fieldContext;

    /** 动态列值 Map（小驼峰 key，如 fieldPrice / fieldEnemyId / fieldDesc） */
    private Map<String, Object> fieldData;

    /** 待批量删除的记录 id 列表（仅关联项 batch-delete 使用） */
    private List<String> fieldIds;

    /** 导入附件 id（仅 API-W208 导入使用；附件组件上传 xlsx 后获得的附件信息 id） */
    private String fieldAttachmentId;

    /** 导入失败数据跳过开关（仅 API-W208 导入使用；true 移除失败批次继续，false 整体回滚） */
    private Boolean skipFail;

    /** 导入异常数据跳过开关（仅 API-W208 导入使用；true 移除异常数据继续，false 返回错误） */
    private Boolean skipError;
}
