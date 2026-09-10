package com.wiki.admin.sys.attachment.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件列表查询 VO，联表 {@code admin_attachment_main} + {@code admin_attachment_file} 的展示对象。
 * <p>
 * 对应 {@code docs/admin/附件机制.md} 附件列表页面展示字段。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class AdminAttachmentListVo {

    /** 附件信息id（admin_attachment_main.field_id） */
    private String fieldId;

    /** 文件id */
    private String fieldFileId;

    /** 文件名称 */
    private String fieldName;

    /** 创建时间 */
    private LocalDateTime fieldCreateTime;

    /** 上传者id */
    private String fieldUploaderId;

    /** 上传者名称（联表 admin_org_user，仅列表展示用） */
    private String fieldUploaderName;

    /** 删除标识（0 未删除 / 1 已删除） */
    private Integer fieldDeleteFlag;

    /** 排序 */
    private Integer fieldOrder;

    /** 模型名称 */
    private String fieldModelName;

    /** 模型id */
    private String fieldModelId;

    /** key */
    private String fieldKey;

    /** 文件类型 */
    private String fieldFileType;

    /** 文件大小(byte) */
    private Long fieldFileSize;

    /** 文件路径 */
    private String fieldFilePath;
}
