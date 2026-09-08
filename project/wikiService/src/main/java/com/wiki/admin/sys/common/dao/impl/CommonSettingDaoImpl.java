package com.wiki.admin.sys.common.dao.impl;

import com.wiki.admin.sys.common.dao.ICommonSettingDao;
import com.wiki.admin.sys.common.model.dto.CommonSettingDo;
import com.wiki.admin.sys.common.model.mapper.CommonSettingMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 公共服务-配置表 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Repository
public class CommonSettingDaoImpl implements ICommonSettingDao {

    private final CommonSettingMapper mapper;

    public CommonSettingDaoImpl(CommonSettingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CommonSettingDo selectByCode(String fieldCode) {
        return mapper.selectByCode(fieldCode);
    }

    @Override
    public List<CommonSettingDo> selectAll() {
        return mapper.selectAll();
    }

    @Override
    public int insert(CommonSettingDo setting) {
        return mapper.insert(setting);
    }

    @Override
    public int updateMeta(CommonSettingDo setting) {
        return mapper.updateMeta(setting);
    }

    @Override
    public int updateValue(String fieldId, String fieldValue) {
        return mapper.updateValue(fieldId, fieldValue);
    }
}
