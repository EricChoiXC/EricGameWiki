package com.wiki.admin.sys.common.controller;

import com.wiki.common.model.response.ApiResponse;
import com.wiki.common.util.IDUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共服务-ID Controller，提供业务主键预生成能力。
 * <p>
 * 用于新建场景下需在落库前先取得主键的场景（如：先上传附件再保存主单据）。
 * 返回的 id 由 {@link IDUtil#initID()} 生成，与各模块 Service 落库时调用的
 * {@code IDUtil.initID(id)} 配合：若前端回传相同 id，后端会原样保留，避免重复生成。
 * <p>
 * 路径前缀显式包含完整 {@code /api/v1/admin/sys/id}，遵循 {@code AGENTS.md} 4.2。
 *
 * @author Eric
 * @date 2026/9/8
 */
@RestController
@RequestMapping("/api/v1/admin/sys/id")
@RequiredArgsConstructor
public class SysIdController {

    /**
     * 预生成业务主键。
     *
     * @return 32 位业务主键
     */
    @GetMapping("/init")
    public ApiResponse<String> init() {
        return ApiResponse.success(IDUtil.initID());
    }
}
