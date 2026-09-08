package com.wiki.admin.sys.org.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户新建/编辑请求（对应页面表单字段）。
 * <p>
 * 校验：登录名、手机号、邮箱不能重复；登录名不可为手机号（纯数字）或邮箱（含 '@'）格式。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgUserSaveRequest {

    /** 主键；新增时为空，编辑时必填 */
    private String fieldId;

    @NotBlank(message = "名称不能为空")
    private String fieldName;

    @NotBlank(message = "登录名不能为空")
    private String fieldLoginName;

    /** 手机号 */
    private String fieldPhone;

    /** 邮箱 */
    private String fieldEmail;

    /** 状态（编辑时使用） */
    private String fieldStatus;

    /** 重置密码（仅新建时使用；编辑时由专门接口修改密码） */
    private String fieldPassword;
}
