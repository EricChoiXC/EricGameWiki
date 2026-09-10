package com.wiki.admin.wiki.controller;

import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.admin.wiki.model.dto.WikiMainVo;
import com.wiki.admin.wiki.model.request.WikiMainSaveRequest;
import com.wiki.admin.wiki.resolver.WikiResolver;
import com.wiki.admin.wiki.service.IWikiMainService;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ApiResponse;
import com.wiki.common.model.response.ListResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * wiki 项目管理 Controller，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} 第 4 节。
 * <p>
 * 路径前缀 {@code /api/v1/admin/wiki}，遵循 {@code AGENTS.md} 4.2。
 * 所有接口通过 {@link WikiResolver#requireAdmin()} 校验 {@code admin-wiki::ADMIN} 权限，
 * 鉴权在 Controller 调用 Service 前完成。
 *
 * @author Eric
 * @date 2026/9/20
 */
@RestController
@RequestMapping("/api/v1/admin/wiki")
@RequiredArgsConstructor
public class WikiMainController {

    private final IWikiMainService wikiMainService;
    private final WikiResolver wikiResolver;

    /**
     * API-W001 项目列表查询。
     */
    @PostMapping("/list")
    public ApiResponse<WikiMainVo> list(@RequestBody ApiRequest<WikiMainDo> request) {
        wikiResolver.requireAdmin();
        ListResult<WikiMainVo> result = wikiMainService.list(request);
        return ApiResponse.success(result.getRecords(), result.getQuery());
    }

    /**
     * API-W002 项目新建。
     */
    @PostMapping("/save")
    public ApiResponse<String> save(@RequestBody @Valid ApiRequest<WikiMainSaveRequest> request) {
        wikiResolver.requireAdmin();
        String id = wikiMainService.save(request.getData());
        return ApiResponse.success(id);
    }

    /**
     * API-W003 项目加载。
     */
    @GetMapping("/load")
    public ApiResponse<WikiMainVo> load(@RequestParam("fieldId") String fieldId) {
        wikiResolver.requireAdmin();
        return ApiResponse.success(wikiMainService.load(fieldId));
    }

    /**
     * API-W004 / API-W006 项目更新（含启用/停用）。
     */
    @PatchMapping("/update")
    public ApiResponse<Void> update(@RequestBody @Valid ApiRequest<WikiMainSaveRequest> request) {
        wikiResolver.requireAdmin();
        wikiMainService.update(request.getData());
        return ApiResponse.success();
    }

    /**
     * API-W007 页面初始化。
     */
    @GetMapping("/init")
    public ApiResponse<Map<String, Object>> init() {
        wikiResolver.requireAdmin();
        return ApiResponse.success(wikiMainService.init());
    }
}
