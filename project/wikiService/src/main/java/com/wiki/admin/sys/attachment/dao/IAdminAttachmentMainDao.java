package com.wiki.admin.sys.attachment.dao;

import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentMainDo;

import java.util.List;
import java.util.Map;

/**
 * 附件-信息 Dao 接口。仅与 {@code AdminAttachmentMainMapper} 一对一对接，不承载业务逻辑。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface IAdminAttachmentMainDao {

    AdminAttachmentMainDo selectById(String fieldId);

    List<com.wiki.admin.sys.attachment.model.dto.AdminAttachmentListVo> selectByCondition(
            String whereSql, String orderBySql, long offset, int pageSize, boolean needPage,
            Map<String, Object> params);

    long countByCondition(String whereSql, Map<String, Object> params);

    int insert(AdminAttachmentMainDo attachment);

    int updateDeleteFlag(String fieldId, Integer fieldDeleteFlag);

    int updateOrder(String fieldId, Integer fieldOrder);

    List<AdminAttachmentMainDo> selectByModel(String fieldModelName, String fieldModelId, String fieldKey);
}
