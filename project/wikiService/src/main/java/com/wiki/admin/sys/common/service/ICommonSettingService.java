package com.wiki.admin.sys.common.service;

import com.wiki.admin.sys.common.model.dto.CommonSettingDo;

import java.util.List;

/**
 * 公共服务-配置表 Service 接口。
 * <p>
 * 承载配置项的业务逻辑：启动同步、读取、更新。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface ICommonSettingService {

    /**
     * 启动同步：读取各模块 setting.yml 的 SETTING 配置项，增量写入 admin_common_setting 表。
     * 已存在的配置项仅更新元数据（name/default/type），保留用户已设置的 value。
     */
    void syncSettingsFromYml();

    /**
     * 查询全部配置项。
     */
    List<CommonSettingDo> listAll();

    /**
     * 按配置项 code 读取配置值；不存在返回 null。
     */
    String getValue(String code);

    /**
     * 按配置项 code 读取配置值；不存在或为空时返回默认值。
     */
    String getValueOrDefault(String code, String defaultValue);

    /**
     * 更新配置值。
     *
     * @param fieldId    配置项 id
     * @param fieldValue 配置值
     */
    void updateValue(String fieldId, String fieldValue);
}
