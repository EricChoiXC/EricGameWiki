package com.wiki.admin.sys.attachment.model.mapper;

import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentFileDo;
import org.apache.ibatis.annotations.Param;

/**
 * 附件-文件 MyBatis Mapper，对应 {@code admin_attachment_file} 表。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface AdminAttachmentFileMapper {

    /** 按主键查询文件信息 */
    AdminAttachmentFileDo selectById(@Param("fieldId") String fieldId);

    /** 新增文件信息 */
    int insert(AdminAttachmentFileDo file);

    /** 按主键删除文件信息（物理删除，配合文件清理） */
    int deleteById(@Param("fieldId") String fieldId);
}
