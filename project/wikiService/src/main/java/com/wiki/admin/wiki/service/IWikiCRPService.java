package com.wiki.admin.wiki.service;

/**
 * wiki 模块跨模块调用入口 Service 接口，对应 {@code docs/admin/wiki/wiki技术方案.md} 7。
 * <p>
 * 遵循 {@code docs/common/业务流转公共规范.md} 4.2：仅做跨模块调用入口与参数校验，
 * 不承载业务逻辑、不直接访问 Dao。
 * 跨模块调用参数禁止直接传递 Do 对象，使用 String/Dto/Request 等值类型。
 * 跨模块调用的异常由本接口实现统一捕获并封装为 BusinessException。
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface IWikiCRPService {

    /**
     * 校验指定用户是否有效（存在且状态为启用）。
     * <p>
     * 通过调用 sys.org 模块 IOrgUserService 查询用户，禁止 wiki 模块直接访问 admin_org_user 表。
     * 供 WikiResolver 维护人员鉴权使用。
     *
     * @param userId 用户ID
     * @return true 用户有效；false 用户不存在或已停用
     */
    boolean checkUserValid(String userId);

    /**
     * 加载导入附件的物理文件路径，供后续导入处理读取文件流。
     * <p>
     * 通过调用 sys.attachment 模块 IAdminAttachmentService 读取附件文件元数据，
     * 禁止 wiki 模块直接访问 admin_attachment 表。供 ImportExportProcessor 导入使用。
     *
     * @param attachmentId 附件信息id（admin_attachment_main.field_id）
     * @return 附件物理文件路径
     */
    String loadAttachmentFilePath(String attachmentId);
}
