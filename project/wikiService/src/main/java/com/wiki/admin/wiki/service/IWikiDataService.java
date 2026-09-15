package com.wiki.admin.wiki.service;

import com.wiki.admin.wiki.model.request.WikiDataRequest;
import com.wiki.admin.wiki.model.response.WikiDataVo;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ListResult;

import java.util.List;

/**
 * wiki 数据明细 Service 接口，承载业务逻辑，对应 {@code docs/admin/wiki/wiki业务逻辑.md} 6.3（FLOW-W003）。
 * <p>
 * 数据维护接口以 {@code fieldDataId}（数据项 id）为入口，后端加载数据项元数据后
 * 根据 {@code fieldDataType} 分发处理：
 * <ul>
 *   <li>图鉴类（data）：固定 name/code + 动态列，名称/编号模糊筛选，fieldCode 降序</li>
 *   <li>关联项（join）：动态 join 列，关联数据模糊筛选（联表），fieldId 降序，支持批量删除</li>
 *   <li>文档类（doc）：固定 name/code/context，名称/编号模糊筛选，fieldCode 降序</li>
 * </ul>
 * 动态表数据通过 {@code WikiDynamicDataDao} 通用动态查询层访问（技术方案 ARCH-W02）。
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface IWikiDataService {

    /**
     * 数据明细列表查询。
     * <p>
     * 以 {@code data.fieldDataId} 为入口加载元数据并按类型分发；
     * 查询条件由 {@code query.data} 标准查询树传入，经动态 FieldMaps 白名单校验。
     * 图鉴类/文档类固定 fieldCode 降序，关联项固定 fieldId 降序。
     */
    ListResult<WikiDataVo> list(ApiRequest<WikiDataRequest> request);

    /**
     * 新建数据明细。
     * <p>
     * 图鉴类/文档类校验名称/编号必填；动态列值经元数据白名单落列；
     * 关联列校验目标动态表记录存在性；附件类明细写入 {@code field_data} json。
     * 返回新建记录 id（动态表 field_id）。
     */
    String save(WikiDataRequest request);

    /**
     * 加载单条数据明细（含动态列值）。
     * <p>
     * 关联项联表查询，返回关联目标显示值。
     */
    WikiDataVo load(String fieldId, String fieldDataId);

    /**
     * 更新数据明细。
     * <p>
     * 校验记录存在，更新动态列值与固定列；记录不存在抛 NOT_FOUND。
     */
    void update(WikiDataRequest request);

    /**
     * 删除单条数据明细。
     */
    void delete(String fieldId, String fieldDataId);

    /**
     * 批量删除数据明细（仅关联项支持）。
     * <p>
     * 非关联项数据项调用抛 BAD_REQUEST。
     */
    void batchDelete(String fieldDataId, List<String> fieldIds);
}
