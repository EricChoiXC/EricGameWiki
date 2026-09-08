package com.wiki.common.model.setting;

import lombok.Data;

/**
 * setting.yml 中 SETTING 配置项的单条模型，对应
 * {@code docs/common/yml配置文件内容规范.md} 配置项内容配置标准。
 * <p>
 * 报文样例：
 * <pre>
 * SETTING:
 *   - KEY: "sys-org::change-password-expire-days"
 *     NAME: "密码有效期"
 *     DESC: "密码有效期，为 0 时表示不过期..."
 *     DEFAULT: "0"
 *     TYPE: "integer"
 * </pre>
 * <p>
 * 服务启动时由 SettingYmlLoader 扫描各模块 setting.yml，
 * 读取 SETTING 列表为本对象集合，供 {@code admin_common_setting} 表增量写入使用。
 * 各模块独立维护和使用自己的配置项信息。
 */
@Data
public class SettingItem {

    /** 配置项标识，如 "sys-org::change-password-expire-days" */
    private String key;

    /** 中文名，对应 admin_common_setting.field_name */
    private String name;

    /** 描述说明 */
    private String desc;

    /** 默认值（字符串管理），对应 admin_common_setting.field_default */
    private String defaultValue;

    /** 配置项类型，对应 admin_common_setting.field_type */
    private SettingType type;
}
