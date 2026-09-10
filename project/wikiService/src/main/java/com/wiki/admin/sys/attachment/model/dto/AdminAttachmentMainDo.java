package com.wiki.admin.sys.attachment.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件-信息 DO，对应 {@code docs/admin/附件机制.md} 中 {@code admin_attachment_main} 表。
 * <p>
 * 通过 {@code field_model_name}、{@code field_model_id}、{@code field_key} 三个字段关联业务对象。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class AdminAttachmentMainDo {

    /** id */
    private String fieldId;

    /** 文件id（关联 admin_attachment_file.field_id） */
    private String fieldFileId;

    /** 文件名称 */
    private String fieldName;

    /** 创建时间 */
    private LocalDateTime fieldCreateTime;

    /** 上传者id */
    private String fieldUploaderId;

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
}
