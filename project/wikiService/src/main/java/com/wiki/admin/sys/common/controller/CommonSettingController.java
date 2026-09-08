package com.wiki.admin.sys.common.controller;

import com.wiki.admin.sys.common.model.dto.CommonSettingDo;
import com.wiki.admin.sys.common.resolver.CommonSettingResolver;
import com.wiki.admin.sys.common.service.ICommonSettingService;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.request.QueryRequest;
import com.wiki.common.model.response.ApiResponse;
import com.wiki.common.model.response.QueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公共服务-配置表 Controller，对应 {@code docs/admin/公共服务.md} 与
 * {@code docs/admin/用户和权限管理.md} 系统配置页面。
 * <p>
 * 路径前缀显式包含完整 {@code /api/v1/admin/sys/setting}，遵循 {@code AGENTS.md} 4.2。
 *
 * @author Eric
 * @date 2026/9/8
 */
@RestController
@RequestMapping("/api/v1/admin/sys/setting")
@RequiredArgsConstructor
public class CommonSettingController {

    private final ICommonSettingService commonSettingService;
    private final CommonSettingResolver commonSettingResolver;

    /**
     * 页面初始化：加载全部配置项。
     */
    @GetMapping("/init")
    public ApiResponse<CommonSettingDo> init() {
        return list(new ApiRequest<>());
    }

    /**
     * 配置项列表查询。
     */
    @PostMapping("/list")
    public ApiResponse<CommonSettingDo> list(@RequestBody ApiRequest<CommonSettingDo> request) {
        QueryRequest query = request.getQuery();
        boolean needPage = query == null || query.getNeedPage() == null || query.getNeedPage();
        List<CommonSettingDo> all = commonSettingService.listAll();
        List<CommonSettingDo> records = applyFilter(all, request.getData());
        if (!needPage) {
            return ApiResponse.success(records, QueryResponse.of(records.size(), 1, records.size()));
        }
        int pageNum = query == null || query.getPageNum() == null ? 1 : query.getPageNum();
        int pageSize = query == null || query.getPageSize() == null ? 15 : query.getPageSize();
        int total = records.size();
        int from = Math.min((pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<CommonSettingDo> page = records.subList(from, to);
        return ApiResponse.success(page, QueryResponse.of(total, pageNum, pageSize));
    }

    /**
     * 更新配置值。
     */
    @PatchMapping("/update")
    public ApiResponse<Void> update(@RequestParam("fieldId") String fieldId,
                                   @RequestBody ApiRequest<CommonSettingDo> request) {
        commonSettingResolver.requireManage();
        CommonSettingDo data = request.getData();
        if (data == null || data.getFieldValue() == null) {
            return ApiResponse.success();
        }
        commonSettingService.updateValue(fieldId, data.getFieldValue());
        return ApiResponse.success();
    }

    private List<CommonSettingDo> applyFilter(List<CommonSettingDo> all, CommonSettingDo filter) {
        if (filter == null) {
            return all;
        }
        String name = filter.getFieldName();
        String code = filter.getFieldCode();
        return all.stream()
                .filter(s -> matchLike(s.getFieldName(), name))
                .filter(s -> matchLike(s.getFieldCode(), code))
                .toList();
    }

    private boolean matchLike(String value, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return value != null && value.contains(keyword);
    }
}
