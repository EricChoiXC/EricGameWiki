package com.wiki.admin.sys.common.service.impl;

import com.wiki.admin.sys.common.dao.ICommonSettingDao;
import com.wiki.admin.sys.common.model.dto.CommonSettingDo;
import com.wiki.admin.sys.common.service.ICommonSettingService;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.setting.SettingItem;
import com.wiki.common.util.IDUtil;
import com.wiki.common.util.SettingYmlLoader;
import com.wiki.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公共服务-配置表 Service 实现，对应 {@code docs/admin/公共服务.md}。
 * <p>
 * 业务第 1 条：服务启动时读取各 setting.yml 的 SETTING 配置项，增量插入 admin_common_setting 表。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommonSettingServiceImpl implements ICommonSettingService {

    private final ICommonSettingDao commonSettingDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncSettingsFromYml() {
        List<SettingItem> items;
        try {
            items = SettingYmlLoader.loadAll();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取 setting.yml 失败", e);
        }
        for (SettingItem item : items) {
            if (StringUtil.isEmpty(item.getKey())) {
                continue;
            }
            upsertSetting(item);
        }
        log.info("公共配置项同步完成，共处理 {} 项", items.size());
    }

    private void upsertSetting(SettingItem item) {
        CommonSettingDo exists = commonSettingDao.selectByCode(item.getKey());
        if (exists == null) {
            CommonSettingDo setting = new CommonSettingDo();
            setting.setFieldId(IDUtil.initID());
            setting.setFieldName(item.getName());
            setting.setFieldCode(item.getKey());
            setting.setFieldValue(item.getDefaultValue());
            setting.setFieldDefault(item.getDefaultValue());
            setting.setFieldType(item.getType() == null ? null : item.getType().getKey());
            commonSettingDao.insert(setting);
            return;
        }
        // 已存在：仅更新元数据，保留用户已设置的 value
        boolean metaChanged = !java.util.Objects.equals(exists.getFieldName(), item.getName())
                || !java.util.Objects.equals(exists.getFieldDefault(), item.getDefaultValue())
                || !java.util.Objects.equals(exists.getFieldType(),
                item.getType() == null ? null : item.getType().getKey());
        if (metaChanged) {
            CommonSettingDo update = new CommonSettingDo();
            update.setFieldId(exists.getFieldId());
            update.setFieldName(item.getName());
            update.setFieldDefault(item.getDefaultValue());
            update.setFieldType(item.getType() == null ? null : item.getType().getKey());
            commonSettingDao.updateMeta(update);
        }
    }

    @Override
    public List<CommonSettingDo> listAll() {
        return commonSettingDao.selectAll();
    }

    @Override
    public String getValue(String code) {
        CommonSettingDo setting = commonSettingDao.selectByCode(code);
        return setting == null ? null : setting.getFieldValue();
    }

    @Override
    public String getValueOrDefault(String code, String defaultValue) {
        String value = getValue(code);
        return StringUtil.isEmpty(value) ? defaultValue : value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateValue(String fieldId, String fieldValue) {
        commonSettingDao.updateValue(fieldId, fieldValue);
    }

    /** 便捷方法：当前时间戳，供子类/审计扩展使用 */
    protected LocalDateTime now() {
        return LocalDateTime.now();
    }
}
