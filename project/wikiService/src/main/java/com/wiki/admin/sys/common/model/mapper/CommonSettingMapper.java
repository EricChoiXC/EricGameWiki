package com.wiki.admin.sys.common.model.mapper;

import com.wiki.admin.sys.common.model.dto.CommonSettingDo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 公共服务-配置表 MyBatis Mapper，对应 {@code admin_common_setting} 表。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface CommonSettingMapper {

    /** 按配置项 code 查询单条 */
    CommonSettingDo selectByCode(@Param("fieldCode") String fieldCode);

    /** 查询全部配置项 */
    List<CommonSettingDo> selectAll();

    /** 新增配置项 */
    int insert(CommonSettingDo setting);

    /** 更新配置项元数据（不含用户已设置的 field_value） */
    int updateMeta(CommonSettingDo setting);

    /** 更新配置值 */
    int updateValue(@Param("fieldId") String fieldId, @Param("fieldValue") String fieldValue);
}
