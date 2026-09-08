package com.wiki.admin.sys.org.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求。
 * <p>
 * 对应 {@code docs/admin/用户和权限管理.md} 业务第 5 条：
 * 顶部栏右侧"修改密码"按钮，弹窗二次输入密码对当前用户密码进行修改，保存后自动登出。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class OrgChangePasswordRequest {

    /** 旧密码 */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
