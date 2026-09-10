package com.wiki.admin.sys.attachment.model.request;

import lombok.Data;

/**
 * 附件上传请求参数（对应 {@code docs/common/前端公共组件.md} 附件组件绑定参数）。
 * <p>
 * 通过 {@code field_model_name}、{@code field_model_id}、{@code field_key} 三个字段关联业务对象。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class AdminAttachmentSaveRequest {

    /** 传递给接口的 fieldModelId 字段值 */
    private String fieldModelId;

    /** 传递给接口的 fieldModelName 字段值，必填 */
    private String fieldModelName;

    /** 传递给接口的 fieldKey 字段值 */
    private String fieldKey;
}
