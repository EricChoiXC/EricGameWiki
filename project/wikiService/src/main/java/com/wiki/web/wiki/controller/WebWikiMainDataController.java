package com.wiki.web.wiki.controller;

import com.wiki.common.model.response.ApiResponse;
import com.wiki.web.wiki.model.dto.WebWikiDataItemVo;
import com.wiki.web.wiki.service.IWebWikiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * wiki 前台数据项 Controller，对应 {@code docs/wiki/项目首页.md} 数据项列表与顶部下拉。
 * <p>
 * 路径前缀 {@code /api/v1/wiki/data}，遵循 {@code AGENTS.md} 4.2。
 * 前台无登录校验、只读（{@code docs/wiki/wiki访问.md}），仅提供 GET 查询。
 *
 * @author Eric
 * @date 2026/9/20
 */
@RestController
@RequestMapping("/api/v1/wiki/data")
@RequiredArgsConstructor
public class WebWikiMainDataController {

    private final IWebWikiService webWikiService;

    /**
     * 项目数据项列表：关键字模糊匹配数据项名称、简称，可按数据项类型过滤，不分页。
     */
    @GetMapping("/list")
    public ApiResponse<WebWikiDataItemVo> list(@RequestParam("fieldSimpleName") String fieldSimpleName,
                                               @RequestParam(value = "keyword", required = false) String keyword,
                                               @RequestParam(value = "fieldDataType", required = false)
                                               String fieldDataType) {
        return ApiResponse.success(webWikiService.listDataItems(fieldSimpleName, keyword, fieldDataType));
    }
}
