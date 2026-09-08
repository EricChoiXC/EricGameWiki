package com.wiki.admin.sys.common.dao;

import com.wiki.admin.sys.common.model.dto.CommonSettingDo;

import java.util.List;

/**
 * 公共服务-配置表 Dao 接口，对应 {@code docs/admin/公共服务.md}。
 * <p>
 * 仅与 {@code CommonSettingMapper} 一对一对接，不承载业务逻辑。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface ICommonSettingDao {

    CommonSettingDo selectByCode(String fieldCode);

    List<CommonSettingDo> selectAll();

    int insert(CommonSettingDo setting);

    int updateMeta(CommonSettingDo setting);

    int updateValue(String fieldId, String fieldValue);
}
