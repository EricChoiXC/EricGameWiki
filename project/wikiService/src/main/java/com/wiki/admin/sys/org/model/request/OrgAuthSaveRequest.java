package com.wiki.admin.sys.org.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色（auth）新建/编辑请求。
 * <p>
 * 对应 {@code docs/admin/用户和权限管理.md} 角色新建/编辑页面：
 * 名称（必填）、编号（必填）、权限（占整行）、用户（占整行）。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgAuthSaveRequest {

    /** 主键；新增时为空，编辑时必填 */
    private String fieldId;

    @NotBlank(message = "名称不能为空")
    private String fieldName;

    @NotBlank(message = "编号不能为空")
    private String fieldCode;

    /** 状态 */
    private String fieldStatus;

    /** 权限ID列表（占整行） */
    private List<String> roleIds;

    /** 用户ID列表（占整行） */
    private List<String> userIds;
}
