package com.wiki.web.wiki.service;

import com.wiki.web.wiki.model.dto.WebWikiDataItemVo;
import com.wiki.web.wiki.model.dto.WebWikiProjectVo;
import com.wiki.web.wiki.model.dto.WebWikiRecordDetailVo;
import com.wiki.web.wiki.model.dto.WebWikiRecordListVo;

import java.util.List;

/**
 * wiki 前台只读服务接口，承载 wikiWeb 页面的读取逻辑。
 * <p>
 * 遵循 {@code docs/wiki/wiki访问.md}：前台无登录校验，所有接口只允许读取操作。
 * 本服务在 {@code com.wiki.web.wiki} 模块内组合复用 {@code com.wiki.admin.wiki}
 * 模块的领域查询逻辑（元数据查询、动态表查询、页面配置解析），仅做参数归一化、
 * 对外字段裁剪与跨层编排，不复制业务逻辑。
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface IWebWikiService {

    /**
     * 首页项目列表（{@code /home}）。
     * <p>
     * 仅返回开启状态的项目；关键字模糊匹配中文名称、英文名称、日文名称、简称；
     * 不分页。
     */
    List<WebWikiProjectVo> homeProjects(String keyword);

    /**
     * 项目信息加载（项目首页 / 顶部公共布局）。
     * <p>
     * 按简称加载项目，项目不存在或已停用时抛 NOT_FOUND。
     */
    WebWikiProjectVo loadProject(String fieldSimpleName);

    /**
     * 项目数据项列表（项目首页 + 顶部下拉）。
     * <p>
     * 关键字模糊匹配数据项名称、数据项简称；可按数据项类型过滤；不分页。
     */
    List<WebWikiDataItemVo> listDataItems(String fieldSimpleName, String keyword, String fieldDataType);

    /**
     * 数据明细记录列表（数据项列表页）。
     * <p>
     * 图鉴类关键字模糊匹配名称、编号；文档类关键字模糊匹配标题；关联项不按关键字筛选。
     * 不分页，同时返回数据项元数据与记录列表。
     */
    WebWikiRecordListVo listRecords(String fieldSimpleName, String fieldDataName, String keyword);

    /**
     * 数据明细记录详情（数据项详情页）。
     * <p>
     * 返回记录数据、wiki 页面配置（未配置时为 null）以及引用本记录的关联项记录
     * （按关联类数据项 id 分组）。
     */
    WebWikiRecordDetailVo loadRecord(String fieldSimpleName, String fieldDataName, String fieldId);
}
