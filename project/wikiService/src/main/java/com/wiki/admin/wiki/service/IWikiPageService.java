package com.wiki.admin.wiki.service;

import com.wiki.admin.wiki.model.request.WikiPageRequest;
import com.wiki.admin.wiki.model.response.WikiPageVo;

import java.util.Map;

/**
 * wiki 页面维护 Service 接口，承载业务逻辑，对应 {@code docs/admin/wiki/wiki业务逻辑.md} 6.4（FLOW-W004）。
 * <p>
 * 页面配置以数据项 id（{@code fieldDataId}）为入口，存储于 {@code wiki_main_data_wiki_page.field_wiki_page}
 * （blob json），一个数据项对应一份配置（唯一约束 {@code fieldDataId}）。
 * <ul>
 *   <li>{@code load}：加载已保存的页面配置</li>
 *   <li>{@code save}：保存配置，已存在则更新，不存在则新建（upsert）</li>
 *   <li>{@code init}：返回显示信息可选源（本数据项数据明细 + 包含该数据项的关联类数据项）</li>
 * </ul>
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface IWikiPageService {

    /**
     * 加载 wiki 页面配置（API-W301）。
     * <p>
     * 以 {@code fieldDataId} 查询唯一配置，未配置时抛 NOT_FOUND；返回结构化配置。
     */
    WikiPageVo load(String fieldDataId);

    /**
     * 保存 wiki 页面配置（API-W302）。
     * <p>
     * 一个数据项对应一份配置，已存在则更新（复用原配置 id），不存在则新建；
     * 配置序列化为 json 写入 {@code field_wiki_page} blob 列。
     */
    void save(WikiPageRequest request);

    /**
     * wiki 页面维护初始化（API-W303）。
     * <p>
     * 返回该数据项的数据明细列表（显示信息可选源 - self），以及所有包含该数据项的
     * 关联类数据项列表（显示字段可选源 - join），每项附数据明细项。
     */
    Map<String, Object> init(String fieldDataId);
}
