package com.wiki.admin.sys.attachment.service;

import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentListVo;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentMainDo;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ListResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 附件 Service 接口，承载业务逻辑。
 * <p>
 * 业务逻辑遵循 {@code docs/admin/附件机制.md}：
 * <ul>
 *   <li>附件关联：通过 field_model_name、field_model_id、field_key 三个字段关联</li>
 *   <li>附件保存路径：${file_path}/${year}/${month}/${fileId}</li>
 *   <li>附件大小限制：从配置项读取，为 0 时不限制</li>
 * </ul>
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IAdminAttachmentService {

    /**
     * 附件列表分页查询（联表 file 与 uploader 展示）。
     */
    ListResult<AdminAttachmentListVo> list(ApiRequest<AdminAttachmentMainDo> request);

    /**
     * 上传附件：保存文件元数据 + 物理文件 + 关联信息；返回附件信息id。
     *
     * @param file          上传的文件
     * @param modelName     模型名称（必填）
     * @param modelId       模型id（可空）
     * @param key           关联key（可空）
     * @param uploaderId    上传者id
     * @return 附件信息id（admin_attachment_main.field_id）
     */
    String upload(MultipartFile file, String modelName, String modelId, String key, String uploaderId);

    /**
     * 下载附件：返回文件元数据，供 Controller 读取物理文件流。
     *
     * @param fieldId 附件信息id
     * @return 附件文件元数据
     */
    com.wiki.admin.sys.attachment.model.dto.AdminAttachmentFileDo loadForDownload(String fieldId);

    /**
     * 逻辑删除附件（设置 field_delete_flag = 1）。
     *
     * @param fieldId 附件信息id
     */
    void delete(String fieldId);

    /**
     * 按模型名称 + 模型id + key 查询附件列表（未删除）。
     */
    List<AdminAttachmentMainDo> listByModel(String modelName, String modelId, String key);

    /**
     * 更新排序。
     *
     * @param fieldId    附件信息id
     * @param fieldOrder 排序值
     */
    void updateOrder(String fieldId, Integer fieldOrder);
}
