package com.wiki.web.wiki.controller;

import com.wiki.common.model.response.ApiResponse;
import com.wiki.web.wiki.model.dto.WebWikiProjectVo;
import com.wiki.web.wiki.service.IWebWikiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * wiki 前台项目 Controller，对应 {@code docs/wiki/首页.md} 与 {@code docs/wiki/项目首页.md}。
 * <p>
 * 路径前缀 {@code /api/v1/wiki/main}，遵循 {@code AGENTS.md} 4.2。
 * 前台无登录校验、只读（{@code docs/wiki/wiki访问.md}），仅提供 GET 查询。
 *
 * @author Eric
 * @date 2026/9/20
 */
@RestController
@RequestMapping("/api/v1/wiki/main")
@RequiredArgsConstructor
public class WebWikiMainController {

    private final IWebWikiService webWikiService;

    /**
     * 首页项目列表：关键字模糊匹配中文名称、英文名称、日文名称、简称，仅返回开启项目，不分页。
     */
    @GetMapping("/list")
    public ApiResponse<WebWikiProjectVo> list(@RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(webWikiService.homeProjects(keyword));
    }

    /**
     * 项目信息加载：按项目简称加载（项目首页 / 顶部公共布局）。
     */
    @GetMapping("/load")
    public ApiResponse<WebWikiProjectVo> load(@RequestParam("fieldSimpleName") String fieldSimpleName) {
        return ApiResponse.success(webWikiService.loadProject(fieldSimpleName));
    }
}
