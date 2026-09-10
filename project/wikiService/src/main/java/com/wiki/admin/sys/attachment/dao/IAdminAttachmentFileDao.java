package com.wiki.admin.sys.attachment.dao;

import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentFileDo;

/**
 * 附件-文件 Dao 接口。仅与 {@code AdminAttachmentFileMapper} 一对一对接，不承载业务逻辑。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IAdminAttachmentFileDao {

    AdminAttachmentFileDo selectById(String fieldId);

    int insert(AdminAttachmentFileDo file);

    int deleteById(String fieldId);
}
