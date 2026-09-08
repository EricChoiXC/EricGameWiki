package com.wiki.admin.sys.org.util;

import com.wiki.common.util.YmlResourceUtil;
import lombok.Data;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * roles.yml 加载器，对应 {@code docs/common/yml配置文件内容规范.md} 权限配置标准
 * 与 {@code docs/admin/用户和权限管理.md} 业务第 2 条：
 * <p>
 * "admin 用户检查后，读取所有模块的 roles.yml 文件，读取权限信息增量保存到 admin_org_role 表中"。
 * <p>
 * 纯函数工具，不承载 DB 写入。
 *
 * @author Eric
 * @date 2026/9/8
 */
public final class OrgRolesYmlLoader {

    /** classpath 下各模块 roles.yml 通配扫描模式 */
    private static final String ROLES_YML_PATTERN = "classpath*:*/roles.yml";

    /** roles.yml 顶层 key */
    private static final String ROLES_KEY = "ROLES";

    private OrgRolesYmlLoader() {
    }

    /**
     * 扫描 classpath 下所有模块的 roles.yml，解析权限项列表。
     */
    public static List<RoleItem> loadAll() throws IOException {
        Resource[] resources = YmlResourceUtil.getResources(ROLES_YML_PATTERN);
        List<RoleItem> items = new ArrayList<>();
        for (Resource resource : resources) {
            items.addAll(load(resource));
        }
        return items;
    }

    /**
     * 解析单个 roles.yml 资源为权限项列表。
     */
    @SuppressWarnings("unchecked")
    public static List<RoleItem> load(Resource resource) throws IOException {
        Map<String, Object> root = YmlResourceUtil.loadYml(resource);
        Object node = root.get(ROLES_KEY);
        if (!(node instanceof List)) {
            return Collections.emptyList();
        }
        List<RoleItem> items = new ArrayList<>();
        for (Object entry : (List<?>) node) {
            if (!(entry instanceof Map)) {
                continue;
            }
            Map<String, Object> map = (Map<String, Object>) entry;
            RoleItem item = new RoleItem();
            item.setKey(asString(map.get("KEY")));
            item.setName(asString(map.get("NAME")));
            item.setDesc(asString(map.get("DESC")));
            if (item.getKey() != null) {
                items.add(item);
            }
        }
        return items;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 权限项模型 */
    @Data
    public static class RoleItem {
        /** 权限标识，如 admin-org::USER */
        private String key;
        /** 中文名 */
        private String name;
        /** 描述 */
        private String desc;
    }
}
