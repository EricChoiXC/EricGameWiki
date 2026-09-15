package com.wiki.admin.wiki.controller;

import com.wiki.admin.wiki.model.dto.WikiMainDataVo;
import com.wiki.admin.wiki.model.request.WikiPageRequest;
import com.wiki.admin.wiki.model.response.WikiPageVo;
import com.wiki.admin.wiki.resolver.WikiResolver;
import com.wiki.admin.wiki.service.IWikiMainDataService;
import com.wiki.admin.wiki.service.IWikiPageService;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * wiki 页面维护 Controller，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} 第 7 节。
 * <p>
 * 路径前缀 {@code /api/v1/admin/wiki/page}，遵循 {@code AGENTS.md} 4.2。
 * 所有接口通过 {@link WikiResolver#requireMaintain(String)} 校验
 * "ADMIN 权限 或 项目维护人员"双层鉴权，鉴权在 Controller 调用 Service 前完成。
 * 文档 ID 为数据项 id（{@code fieldDataId}），先加载数据项解析出项目 id（{@code fieldMainId}）再进行鉴权。
 *
 * @author Eric
 * @date 2026/9/20
 */
@RestController
@RequestMapping("/api/v1/admin/wiki/page")
@RequiredArgsConstructor
public class WikiPageController {

    private final IWikiPageService wikiPageService;
    private final IWikiMainDataService wikiMainDataService;
    private final WikiResolver wikiResolver;

    /**
     * API-W301 wiki 页面配置加载。
     */
    @GetMapping("/load")
    public ApiResponse<WikiPageVo> load(@RequestParam("fieldDataId") String fieldDataId) {
        requireMaintain(fieldDataId);
        return ApiResponse.success(wikiPageService.load(fieldDataId));
    }

    /**
     * API-W302 wiki 页面配置保存。
     * <p>
     * 一个数据项对应一份配置（唯一约束 fieldDataId），已存在则更新，不存在则新建。
     */
    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestBody @Valid ApiRequest<WikiPageRequest> request) {
        requireMaintain(request.getData());
        wikiPageService.save(request.getData());
        return ApiResponse.success();
    }

    /**
     * API-W303 wiki 页面配置初始化。
     * <p>
     * 返回该数据项的数据明细列表（显示信息可选源）和所有包含该数据项的关联类数据项列表（显示字段可选源）。
     */
    @GetMapping("/init")
    public ApiResponse<Map<String, Object>> init(@RequestParam("fieldDataId") String fieldDataId) {
        requireMaintain(fieldDataId);
        return ApiResponse.success(wikiPageService.init(fieldDataId));
    }

    /**
     * 数据项 id → 项目 id 解析后执行双层鉴权（请求体来源）。
     */
    private void requireMaintain(WikiPageRequest data) {
        if (data == null || data.getFieldDataId() == null || data.getFieldDataId().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据项ID不能为空");
        }
        requireMaintain(data.getFieldDataId());
    }

    /**
     * 数据项 id → 项目 id 解析后执行双层鉴权（请求参数来源）。
     */
    private void requireMaintain(String fieldDataId) {
        WikiMainDataVo vo = wikiMainDataService.load(fieldDataId);
        wikiResolver.requireMaintain(vo.getFieldMainId());
    }
}
