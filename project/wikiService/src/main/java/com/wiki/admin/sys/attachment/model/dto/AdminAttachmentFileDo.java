package com.wiki.admin.sys.attachment.model.dto;

import lombok.Data;

/**
 * 附件-文件 DO，对应 {@code docs/admin/附件机制.md} 中 {@code admin_attachment_file} 表。
 * <p>
 * 保存上传文件的元数据与存储路径，以 {@code field_id} 作为文件名保存。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class AdminAttachmentFileDo {

    /** id */
    private String fieldId;

    /** 文件名称 */
    private String fieldFileName;

    /** 文件类型 */
    private String fieldFileType;

    /** 文件大小(byte) */
    private Long fieldFileSize;

    /** 文件路径 */
    private String fieldFilePath;
}
