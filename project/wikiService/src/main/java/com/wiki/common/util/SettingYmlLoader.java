package com.wiki.common.util;

import com.wiki.common.model.setting.SettingItem;
import com.wiki.common.model.setting.SettingType;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * setting.yml 配置项加载工具，对应 {@code docs/common/yml配置文件内容规范.md} 配置项内容配置标准
 * 与 {@code docs/admin/公共服务.md} 业务第 1 条：
 * <p>
 * "服务启动时，读取各个 setting.yml 文档的 SETTING 配置项，增量插入 admin_common_setting 表"。
 * <p>
 * 本类仅负责将 classpath 下各模块的 setting.yml 解析为 {@link SettingItem} 列表，
 * 不承载 DB 写入逻辑；DB 持久化由各模块 SettingService 在启动流程中调用本类后完成。
 * <p>
 * 约束遵循 {@code docs/common/业务流转公共规范.md} 2.3 分层职责：
 * 本类为纯函数工具，不调用业务层 / DB。
 *
 * @author Eric
 * @date 2026/9/8
 */
public final class SettingYmlLoader {

    /** setting.yml 通配扫描模式，覆盖各模块 resources/${模块}/setting.yml */
    private static final String SETTING_YML_PATTERN = "classpath*:*/setting.yml";

    /** setting.yml 中 SETTING 顶层 key */
    private static final String SETTING_KEY = "SETTING";

    private SettingYmlLoader() {
    }

    /**
     * 扫描 classpath 下所有模块的 setting.yml，解析 SETTING 配置项列表。
     *
     * @return 全部模块的配置项集合（无配置时返回空列表）
     * @throws IOException 资源扫描或读取失败
     */
    public static List<SettingItem> loadAll() throws IOException {
        Resource[] resources = YmlResourceUtil.getResources(SETTING_YML_PATTERN);
        List<SettingItem> items = new ArrayList<>();
        for (Resource resource : resources) {
            items.addAll(load(resource));
        }
        return items;
    }

    /**
     * 解析单个 setting.yml 资源为配置项列表。
     *
     * @param resource setting.yml 资源
     * @return 配置项列表（无 SETTING 节点或为空时返回空列表）
     * @throws IOException 资源读取失败
     */
    @SuppressWarnings("unchecked")
    public static List<SettingItem> load(Resource resource) throws IOException {
        Map<String, Object> root = YmlResourceUtil.loadYml(resource);
        Object settingNode = root.get(SETTING_KEY);
        if (!(settingNode instanceof List)) {
            return Collections.emptyList();
        }
        List<SettingItem> items = new ArrayList<>();
        for (Object entry : (List<?>) settingNode) {
            if (!(entry instanceof Map)) {
                continue;
            }
            SettingItem item = toSettingItem((Map<String, Object>) entry);
            if (item != null && Objects.nonNull(item.getKey())) {
                items.add(item);
            }
        }
        return items;
    }

    private static SettingItem toSettingItem(Map<String, Object> map) {
        SettingItem item = new SettingItem();
        item.setKey(asString(map.get("KEY")));
        item.setName(asString(map.get("NAME")));
        item.setDesc(asString(map.get("DESC")));
        item.setDefaultValue(asString(map.get("DEFAULT")));
        String typeKey = asString(map.get("TYPE"));
        item.setType(SettingType.fromKey(typeKey));
        return item;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
