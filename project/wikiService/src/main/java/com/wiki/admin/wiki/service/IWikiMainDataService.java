package com.wiki.admin.wiki.service;

import com.wiki.admin.wiki.model.dto.WikiMainDataVo;
import com.wiki.admin.wiki.model.request.WikiMainDataSaveRequest;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ListResult;

import java.util.Map;

/**
 * wiki 数据项 Service 接口，承载业务逻辑，对应 {@code wiki_main_data} 表。
 * <p>
 * 业务逻辑遵循 {@code docs/admin/wiki/wiki业务逻辑.md} 6.2：
 * <ul>
 *   <li>save：校验简称项目内唯一 → 补充图鉴类固定列 → 保存元数据 → 触发动态建表；DDL 失败补偿删除元数据</li>
 *   <li>update：fieldDataName/fieldDataType 不可改；明细变更仅允许 ALTER TABLE ADD COLUMN</li>
 *   <li>delete：校验被引用关系 → 删元数据 → DROP TABLE；被引用时返回 CONFLICT</li>
 * </ul>
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface IWikiMainDataService {

    /**
     * 数据项列表查询。
     * <p>
     * 固定按 field_id 降序排序，支持按 fieldMainId 过滤。
     */
    ListResult<WikiMainDataVo> list(ApiRequest<WikiMainDataVo> request);

    /**
     * 新建数据项。
     * <p>
     * 校验简称项目内唯一 → 图鉴类自动补充 name/code 固定列到 fieldDataJson 前部 →
     * 保存 wiki_main_data → 触发动态建表引擎建表；DDL 失败需补偿删除元数据记录。
     * 返回新建数据项 id。
     */
    String save(WikiMainDataSaveRequest request);

    /**
     * 加载数据项详情（含 fieldDataJson 结构化明细列表）。
     */
    WikiMainDataVo load(String fieldId);

    /**
     * 更新数据项。
     * <p>
     * fieldDataName/fieldDataType 不可改；明细变更仅允许新增列（ALTER TABLE ADD COLUMN），
     * 禁止删除/修改已存在列以保护存量数据。
     */
    void update(WikiMainDataSaveRequest request);

    /**
     * 删除数据项。
     * <p>
     * 校验该数据项是否被其他关联项明细引用，存在引用时抛 CONFLICT；
     * 无引用时删除元数据记录并 DROP TABLE。
     */
    void delete(String fieldId);

    /**
     * 页面初始化：返回数据项类型枚举、该项目已有图鉴类/文档类数据项列表（关联项选择源）。
     */
    Map<String, Object> init(String fieldMainId);
}
