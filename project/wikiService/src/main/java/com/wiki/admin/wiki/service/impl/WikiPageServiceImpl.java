package com.wiki.admin.wiki.service.impl;

import com.wiki.admin.wiki.dao.IWikiMainDataDao;
import com.wiki.admin.wiki.dao.IWikiMainDao;
import com.wiki.admin.wiki.dao.IWikiPageDao;
import com.wiki.admin.wiki.model.dto.WikiDataDetailDo;
import com.wiki.admin.wiki.model.dto.WikiMainDataDo;
import com.wiki.admin.wiki.model.dto.WikiMainDo;
import com.wiki.admin.wiki.model.dto.WikiPageConfigDo;
import com.wiki.admin.wiki.model.dto.WikiPageDo;
import com.wiki.admin.wiki.model.request.WikiPageRequest;
import com.wiki.admin.wiki.model.response.WikiPageVo;
import com.wiki.admin.wiki.service.IWikiPageService;
import com.wiki.admin.wiki.util.DynamicTableSqlBuilder;
import com.wiki.admin.wiki.util.WikiConstants;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.util.IDUtil;
import com.wiki.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * wiki 页面维护 Service 实现，对应 {@code docs/admin/wiki/wiki业务逻辑.md} 6.4（FLOW-W004）。
 * <p>
 * 页面配置以数据项 id（{@code fieldDataId}）为入口：
 * <ul>
 *   <li>save：配置序列化为 json 写入 {@code field_wiki_page} blob 列，唯一约束 {@code fieldDataId}，
 *       已存在则更新、不存在则新建</li>
 *   <li>init：返回显示信息可选源（本数据项数据明细 + 所有包含该数据项的关联类数据项），
 *       关联类数据项通过解析同项目 join 数据项的 {@code field_data_json} 中 {@code join} 目标判断</li>
 * </ul>
 * 仅图鉴类（data）、文档类（doc）数据项支持 wiki 页面维护（业务逻辑 4.1）。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiPageServiceImpl implements IWikiPageService {

    private final IWikiPageDao wikiPageDao;
    private final IWikiMainDataDao wikiMainDataDao;
    private final IWikiMainDao wikiMainDao;

    @Override
    public WikiPageVo load(String fieldDataId) {
        WikiMainDataDo meta = loadMeta(fieldDataId);
        loadMain(meta.getFieldMainId());
        WikiPageDo page = wikiPageDao.selectByDataId(fieldDataId);
        if (page == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 页面配置不存在");
        }
        WikiPageVo vo = new WikiPageVo();
        vo.setFieldId(page.getFieldId());
        vo.setFieldDataId(page.getFieldDataId());
        vo.setFieldWikiPage(parseConfig(page.getFieldWikiPage()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(WikiPageRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求数据不能为空");
        }
        if (request.getFieldDataId() == null || request.getFieldDataId().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据项ID不能为空");
        }
        WikiMainDataDo meta = loadMeta(request.getFieldDataId());
        WikiMainDo wikiMain = loadMain(meta.getFieldMainId());
        validatePageSupported(meta);
        WikiPageConfigDo config = request.getFieldWikiPage();
        if (config == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "页面配置不能为空");
        }
        validateConfig(config);

        byte[] blob = JsonUtil.toJson(config).getBytes(StandardCharsets.UTF_8);
        WikiPageDo existing = wikiPageDao.selectByDataId(request.getFieldDataId());
        if (existing == null) {
            WikiPageDo page = new WikiPageDo();
            page.setFieldId(IDUtil.initID(null));
            page.setFieldMainId(meta.getFieldMainId());
            page.setFieldDataId(request.getFieldDataId());
            page.setFieldWikiPage(blob);
            wikiPageDao.insert(page);
            log.info("[wiki] 页面配置新建 fieldDataId={}", request.getFieldDataId());
        } else {
            existing.setFieldMainId(wikiMain.getFieldId());
            existing.setFieldWikiPage(blob);
            wikiPageDao.update(existing);
            log.info("[wiki] 页面配置更新 fieldDataId={}", request.getFieldDataId());
        }
    }

    @Override
    public Map<String, Object> init(String fieldDataId) {
        WikiMainDataDo meta = loadMeta(fieldDataId);
        loadMain(meta.getFieldMainId());
        validatePageSupported(meta);

        Map<String, Object> dataItem = new LinkedHashMap<>();
        dataItem.put("fieldId", meta.getFieldId());
        dataItem.put("fieldName", meta.getFieldName());
        dataItem.put("fieldDataName", meta.getFieldDataName());
        dataItem.put("fieldDataType", meta.getFieldDataType());
        dataItem.put("details", buildSelfDetails(meta));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataItem", dataItem);
        result.put("joinItems", buildJoinItems(meta));
        return result;
    }

    // ===== 内部：可选源构建 =====

    /**
     * 本数据项的数据明细（显示信息可选源 - self）。
     * <p>
     * 图鉴类解析 {@code field_data_json}（含后端自动补充的 name/code 固定列）；
     * 文档类无明细定义，按数据库设计 4.3 固定列合成 name（标题）/code（编号）/context（内容）。
     */
    private List<Map<String, Object>> buildSelfDetails(WikiMainDataDo meta) {
        if (WikiConstants.DATA_TYPE_DOC.equals(meta.getFieldDataType())) {
            List<Map<String, Object>> details = new ArrayList<>();
            details.add(buildDetailEntry("name", "标题", "text"));
            details.add(buildDetailEntry("code", "编号", "text"));
            details.add(buildDetailEntry("context", "内容", "blob"));
            return details;
        }
        List<Map<String, Object>> details = new ArrayList<>();
        for (WikiDataDetailDo detail : DynamicTableSqlBuilder.parseDetails(meta.getFieldDataJson())) {
            details.add(buildDetailEntry(detail));
        }
        return details;
    }

    /**
     * 所有包含该数据项的关联类数据项（显示字段可选源 - join）。
     * <p>
     * 遍历同项目 join 类数据项，其 {@code field_data_json} 中任一 {@code type=join} 明细的
     * {@code join} 目标等于本数据项 id 即为包含。
     */
    private List<Map<String, Object>> buildJoinItems(WikiMainDataDo meta) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (WikiMainDataDo sibling : wikiMainDataDao.selectByMainId(meta.getFieldMainId())) {
            if (sibling.getFieldId().equals(meta.getFieldId())) {
                continue;
            }
            if (!WikiConstants.DATA_TYPE_JOIN.equals(sibling.getFieldDataType())) {
                continue;
            }
            if (!containsDataItem(sibling, meta.getFieldId())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fieldId", sibling.getFieldId());
            item.put("fieldName", sibling.getFieldName());
            item.put("fieldDataName", sibling.getFieldDataName());
            item.put("fieldDataType", sibling.getFieldDataType());
            List<Map<String, Object>> details = new ArrayList<>();
            for (WikiDataDetailDo detail : DynamicTableSqlBuilder.parseDetails(sibling.getFieldDataJson())) {
                details.add(buildDetailEntry(detail));
            }
            item.put("details", details);
            items.add(item);
        }
        return items;
    }

    private boolean containsDataItem(WikiMainDataDo joinItem, String fieldDataId) {
        for (WikiDataDetailDo detail : DynamicTableSqlBuilder.parseDetails(joinItem.getFieldDataJson())) {
            if (WikiConstants.DATA_TYPE_JOIN.equals(detail.getType())
                    && fieldDataId.equals(detail.getJoin())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 明细行 → 可选源条目：{@code dataName}（简称）、{@code name}（显示名）、
     * {@code type}（类型）、{@code fieldKey}（页面配置 fields 中使用的属性键）。
     */
    private Map<String, Object> buildDetailEntry(WikiDataDetailDo detail) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("dataName", detail.getDataName());
        entry.put("name", detail.getName());
        entry.put("type", detail.getType());
        entry.put("fieldKey", toFieldKey(detail.getDataName(), detail.getType()));
        return entry;
    }

    private Map<String, Object> buildDetailEntry(String dataName, String name, String type) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("dataName", dataName);
        entry.put("name", name);
        entry.put("type", type);
        entry.put("fieldKey", toFieldKey(dataName, type));
        return entry;
    }

    /**
     * 明细行 → 页面配置字段属性键：type=join 生成 {@code field${DataName}Id}，其余 {@code field${DataName}}。
     */
    private String toFieldKey(String dataName, String type) {
        String prefix = "field" + upperFirst(dataName);
        return WikiConstants.DATA_TYPE_JOIN.equals(type) ? prefix + "Id" : prefix;
    }

    // ===== 内部：配置校验 =====

    /**
     * 页面配置结构校验：displayInfos 的 type 仅支持 self/join 且字段列表非空；
     * displayFields 数据项 id 与字段列表非空。
     */
    private void validateConfig(WikiPageConfigDo config) {
        List<WikiPageConfigDo.DisplayInfo> infos = config.getDisplayInfos();
        if (infos != null) {
            for (WikiPageConfigDo.DisplayInfo info : infos) {
                if (info == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "显示信息项不能为空");
                }
                String type = info.getType();
                if (!WikiConstants.PAGE_SOURCE_SELF.equals(type)
                        && !WikiConstants.DATA_TYPE_JOIN.equals(type)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "不支持的显示信息来源类型：" + type);
                }
                requireValue(info.getFieldDataId(), "显示信息数据项ID");
                if (info.getFields() == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "显示信息字段列表不能为空");
                }
            }
        }
        List<WikiPageConfigDo.DisplayField> fields = config.getDisplayFields();
        if (fields != null) {
            for (WikiPageConfigDo.DisplayField field : fields) {
                if (field == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "显示字段项不能为空");
                }
                requireValue(field.getFieldDataId(), "显示字段数据项ID");
                if (field.getFields() == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "显示字段列表不能为空");
                }
            }
        }
    }

    private void validatePageSupported(WikiMainDataDo meta) {
        if (WikiConstants.DATA_TYPE_JOIN.equals(meta.getFieldDataType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "仅图鉴类、文档类数据项支持 wiki 页面维护");
        }
    }

    // ===== 内部：公共 =====

    private WikiPageConfigDo parseConfig(byte[] blob) {
        if (blob == null) {
            return null;
        }
        return JsonUtil.fromJson(new String(blob, StandardCharsets.UTF_8), WikiPageConfigDo.class);
    }

    private WikiMainDataDo loadMeta(String fieldDataId) {
        if (fieldDataId == null || fieldDataId.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据项ID不能为空");
        }
        WikiMainDataDo meta = wikiMainDataDao.selectById(fieldDataId);
        if (meta == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 数据项不存在");
        }
        return meta;
    }

    private WikiMainDo loadMain(String fieldMainId) {
        WikiMainDo wikiMain = wikiMainDao.selectById(fieldMainId);
        if (wikiMain == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "wiki 项目不存在");
        }
        return wikiMain;
    }

    private void requireValue(String value, String label) {
        if (value == null || value.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不能为空");
        }
    }

    private String upperFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
