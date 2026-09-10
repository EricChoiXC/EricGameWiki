package com.wiki.admin.wiki.controller;

import com.wiki.admin.wiki.model.dto.WikiMainDataVo;
import com.wiki.admin.wiki.model.request.WikiMainDataSaveRequest;
import com.wiki.admin.wiki.resolver.WikiResolver;
import com.wiki.admin.wiki.service.IWikiMainDataService;
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

import java.util.Map;

/**
 * wiki 数据项管理 Controller，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} 第 5 节。
 * <p>
 * 路径前缀 {@code /api/v1/admin/wiki/data}，遵循 {@code AGENTS.md} 4.2。
 * 所有接口通过 {@link WikiResolver#requireMaintain(String)} 校验
 * "ADMIN 权限 或 项目维护人员"双层鉴权，鉴权在 Controller 调用 Service 前完成。
 * 鉴权核心参数为"接口 + 文档 ID"，文档 ID 为项目 id（{@code fieldMainId}）。
 *
 * @author Eric
 * @date 2026/9/20
 */
@RestController
@RequestMapping("/api/v1/admin/wiki/data")
@RequiredArgsConstructor
public class WikiMainDataController {

    private final IWikiMainDataService wikiMainDataService;
    private final WikiResolver wikiResolver;

    /**
     * API-W101 数据项列表查询。
     * <p>
     * 文档 ID（fieldMainId）从请求 data 中获取，用于鉴权。
     */
    @PostMapping("/list")
    public ApiResponse<WikiMainDataVo> list(@RequestBody ApiRequest<WikiMainDataVo> request) {
        String fieldMainId = request.getData() == null ? null : request.getData().getFieldMainId();
        wikiResolver.requireMaintain(fieldMainId);
        ListResult<WikiMainDataVo> result = wikiMainDataService.list(request);
        return ApiResponse.success(result.getRecords(), result.getQuery());
    }

    /**
     * API-W102 数据项新建。
     * <p>
     * 副作用：创建 {@code wiki_${simpleName}_${dataName}} 动态表。
     */
    @PostMapping("/save")
    public ApiResponse<String> save(@RequestBody @Valid ApiRequest<WikiMainDataSaveRequest> request) {
        wikiResolver.requireMaintain(request.getData().getFieldMainId());
        String id = wikiMainDataService.save(request.getData());
        return ApiResponse.success(id);
    }

    /**
     * API-W103 数据项加载。
     */
    @GetMapping("/load")
    public ApiResponse<WikiMainDataVo> load(@RequestParam("fieldId") String fieldId) {
        // 加载用于获取 fieldMainId 进行鉴权
        WikiMainDataVo vo = wikiMainDataService.load(fieldId);
        wikiResolver.requireMaintain(vo.getFieldMainId());
        return ApiResponse.success(vo);
    }

    /**
     * API-W104 数据项更新。
     * <p>
     * 副作用：明细变更时执行 ALTER TABLE ADD COLUMN。
     * fieldDataName/fieldDataType 不可更新。
     */
    @PatchMapping("/update")
    public ApiResponse<Void> update(@RequestBody @Valid ApiRequest<WikiMainDataSaveRequest> request) {
        // 加载已有数据项获取 fieldMainId 进行鉴权
        WikiMainDataVo vo = wikiMainDataService.load(request.getData().getFieldId());
        wikiResolver.requireMaintain(vo.getFieldMainId());
        wikiMainDataService.update(request.getData());
        return ApiResponse.success();
    }

    /**
     * API-W105 数据项删除。
     * <p>
     * 副作用：删除 {@code wiki_${simpleName}_${dataName}} 动态表。
     * 删除前校验被引用关系，存在引用时返回 CONFLICT。
     */
    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam("fieldId") String fieldId) {
        WikiMainDataVo vo = wikiMainDataService.load(fieldId);
        wikiResolver.requireMaintain(vo.getFieldMainId());
        wikiMainDataService.delete(fieldId);
        return ApiResponse.success();
    }

    /**
     * API-W106 数据项初始化。
     * <p>
     * 返回数据项类型枚举、该项目已有图鉴类/文档类数据项列表（关联项选择源）。
     */
    @GetMapping("/init")
    public ApiResponse<Map<String, Object>> init(@RequestParam("fieldMainId") String fieldMainId) {
        wikiResolver.requireMaintain(fieldMainId);
        return ApiResponse.success(wikiMainDataService.init(fieldMainId));
    }
}
