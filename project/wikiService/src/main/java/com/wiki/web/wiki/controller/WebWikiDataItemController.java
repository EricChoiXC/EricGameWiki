package com.wiki.web.wiki.controller;

import com.wiki.common.model.response.ApiResponse;
import com.wiki.web.wiki.model.dto.WebWikiRecordDetailVo;
import com.wiki.web.wiki.model.dto.WebWikiRecordListVo;
import com.wiki.web.wiki.service.IWebWikiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * wiki 前台数据明细 Controller，对应 {@code docs/wiki/图鉴类数据项页面.md} 与
 * {@code docs/wiki/文档类页面.md} 的列表 / 详情页面。
 * <p>
 * 路径前缀 {@code /api/v1/wiki/data-item}，遵循 {@code AGENTS.md} 4.2。
 * 前台无登录校验、只读（{@code docs/wiki/wiki访问.md}），仅提供 GET 查询。
 *
 * @author Eric
 * @date 2026/9/20
 */
@RestController
@RequestMapping("/api/v1/wiki/data-item")
@RequiredArgsConstructor
public class WebWikiDataItemController {

    private final IWebWikiService webWikiService;

    /**
     * 数据明细记录列表：图鉴类关键字匹配名称/编号，文档类关键字匹配标题，不分页。
     */
    @GetMapping("/list")
    public ApiResponse<WebWikiRecordListVo> list(@RequestParam("fieldSimpleName") String fieldSimpleName,
                                                 @RequestParam("fieldDataName") String fieldDataName,
                                                 @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(webWikiService.listRecords(fieldSimpleName, fieldDataName, keyword));
    }

    /**
     * 数据明细记录详情：返回记录数据、wiki 页面配置与引用本记录的关联项记录。
     */
    @GetMapping("/load")
    public ApiResponse<WebWikiRecordDetailVo> load(@RequestParam("fieldSimpleName") String fieldSimpleName,
                                                   @RequestParam("fieldDataName") String fieldDataName,
                                                   @RequestParam("fieldId") String fieldId) {
        return ApiResponse.success(webWikiService.loadRecord(fieldSimpleName, fieldDataName, fieldId));
    }
}
