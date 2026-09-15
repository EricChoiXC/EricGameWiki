package com.wiki.admin.wiki.controller;

import com.wiki.admin.wiki.model.dto.WikiMainDataVo;
import com.wiki.admin.wiki.model.request.WikiDataRequest;
import com.wiki.admin.wiki.model.response.WikiDataVo;
import com.wiki.admin.wiki.resolver.WikiResolver;
import com.wiki.admin.wiki.service.IWikiDataService;
import com.wiki.admin.wiki.service.IWikiMainDataService;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ApiResponse;
import com.wiki.common.model.response.ListResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * wiki 数据明细维护 Controller，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} 第 6 节。
 * <p>
 * 路径前缀 {@code /api/v1/admin/wiki/data-item}，遵循 {@code AGENTS.md} 4.2。
 * 数据明细接口以 {@code fieldDataId}（数据项 id）为入口，后端加载元数据后按 {@code fieldDataType} 分发。
 * <p>
 * 鉴权经 {@link WikiResolver#requireMaintain(String)} 校验"ADMIN 权限 或 项目维护人员"双层鉴权，
 * 鉴权在 Controller 调用 Service 前完成。文档 ID 为数据项 id（{@code fieldDataId}），
 * 先加载数据项解析出项目 id（{@code fieldMainId}）再进行鉴权。
 *
 * @author Eric
 * @date 2026/9/20
 */
@RestController
@RequestMapping("/api/v1/admin/wiki/data-item")
@RequiredArgsConstructor
public class WikiDataController {

    private final IWikiDataService wikiDataService;
    private final IWikiMainDataService wikiMainDataService;
    private final WikiResolver wikiResolver;

    /**
     * API-W201 数据明细列表查询。
     * <p>
     * 图鉴类/文档类固定 fieldCode 降序；关联项固定 fieldId 降序、支持关联数据模糊筛选。
     */
    @PostMapping("/list")
    public ApiResponse<WikiDataVo> list(@RequestBody ApiRequest<WikiDataRequest> request) {
        requireMaintain(request.getData());
        ListResult<WikiDataVo> result = wikiDataService.list(request);
        return ApiResponse.success(result.getRecords(), result.getQuery());
    }

    /**
     * API-W202 数据明细新建。
     * <p>
     * 返回新建记录 id（动态表 field_id）。
     */
    @PostMapping("/save")
    public ApiResponse<String> save(@RequestBody @Valid ApiRequest<WikiDataRequest> request) {
        requireMaintain(request.getData());
        String id = wikiDataService.save(request.getData());
        return ApiResponse.success(id);
    }

    /**
     * API-W203 数据明细加载。
     */
    @GetMapping("/load")
    public ApiResponse<WikiDataVo> load(@RequestParam("fieldId") String fieldId,
                                        @RequestParam("fieldDataId") String fieldDataId) {
        requireMaintain(fieldDataId);
        return ApiResponse.success(wikiDataService.load(fieldId, fieldDataId));
    }

    /**
     * API-W204 数据明细更新。
     */
    @PatchMapping("/update")
    public ApiResponse<Void> update(@RequestBody @Valid ApiRequest<WikiDataRequest> request) {
        requireMaintain(request.getData());
        wikiDataService.update(request.getData());
        return ApiResponse.success();
    }

    /**
     * API-W205 数据明细删除。
     */
    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam("fieldId") String fieldId,
                                    @RequestParam("fieldDataId") String fieldDataId) {
        requireMaintain(fieldDataId);
        wikiDataService.delete(fieldId, fieldDataId);
        return ApiResponse.success();
    }

    /**
     * API-W206 数据明细批量删除（仅关联项支持）。
     */
    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody ApiRequest<WikiDataRequest> request) {
        requireMaintain(request.getData());
        wikiDataService.batchDelete(request.getData().getFieldDataId(), request.getData().getFieldIds());
        return ApiResponse.success();
    }

    /**
     * 数据项 id → 项目 id 解析后执行双层鉴权（请求体来源）。
     */
    private void requireMaintain(WikiDataRequest data) {
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
