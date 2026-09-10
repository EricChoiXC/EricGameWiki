package com.wiki.admin.wiki.service;

import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.admin.wiki.model.dto.WikiMainVo;
import com.wiki.admin.wiki.model.request.WikiMainSaveRequest;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ListResult;

import java.util.List;
import java.util.Map;

/**
 * wiki 项目 Service 接口，承载业务逻辑，对应 {@code wiki_main} 表。
 * <p>
 * 业务逻辑遵循 {@code docs/admin/wiki/wiki业务逻辑.md} 6.1。
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface IWikiMainService {

    /**
     * 项目列表查询。
     * <p>
     * 筛选区支持名称模糊查询（同时匹配 field_name、field_en_name、field_jp_name），
     * 查询条件由 query.data 标准查询树传入，经 WikiFieldMaps 白名单校验。
     */
    ListResult<WikiMainVo> list(ApiRequest<WikiMainDo> request);

    /**
     * 新建项目。
     * <p>
     * 校验简称全局唯一（小写英文和数字格式）→ 序列化 fieldExtend/fieldManagers JSON → 保存。
     * 返回新建项目 id。
     */
    String save(WikiMainSaveRequest request);

    /**
     * 加载项目详情（含 fieldExtend、fieldManagers）。
     */
    WikiMainVo load(String fieldId);

    /**
     * 更新项目（含启用/停用 fieldStatus）。
     * <p>
     * fieldSimpleName 不可更新；更新时校验维护人员用户有效性。
     */
    void update(WikiMainSaveRequest request);

    /**
     * 页面初始化：返回维护人员候选用户列表等。
     */
    Map<String, Object> init();
}
