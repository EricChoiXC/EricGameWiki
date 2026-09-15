package com.wiki.admin.wiki.service;

import com.wiki.admin.wiki.model.dto.ImportResult;
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

    /**
     * API-W207 导入模板下载。
     * <p>
     * 仅关联项支持；生成首行标题行的 xlsx 模板，关联数据显示 {@code ${关联数据}编号}，
     * 不列附件类明细。
     *
     * @param fieldDataId 数据项 id（需为关联项）
     * @return xlsx 字节流
     */
    byte[] template(String fieldDataId);

    /**
     * API-W208 数据明细导入。
     * <p>
     * 经 {@code IWikiCRPService} 调用 sys.attachment 读取上传的 xlsx 附件路径，
     * 委托 {@code ImportExportProcessor} 执行读取 → 数据合理性校验 → 分批导入（每批 200 条）。
     * 导入主流程标注 {@code @Transactional}，未选失败跳过时批次 REQUIRED 加入主事务整体回滚。
     * 返回成功数 / 跳过数 / 失败明细。
     *
     * @param request 导入请求（含 fieldDataId、fieldAttachmentId、skipFail、skipError）
     * @return 导入结果
     */
    ImportResult importData(WikiDataRequest request);

    /**
     * API-W209 数据明细导出。
     * <p>
     * 仅关联项支持；导出该数据项全部数据明细为 xlsx，首行标题行与导入模板一致，
     * 关联数据列显示目标记录编号。
     *
     * @param fieldDataId 数据项 id（需为关联项）
     * @return xlsx 字节流
     */
    byte[] export(String fieldDataId);
}
