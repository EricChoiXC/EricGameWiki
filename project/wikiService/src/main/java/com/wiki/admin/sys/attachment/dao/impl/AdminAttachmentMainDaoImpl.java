package com.wiki.admin.sys.attachment.dao.impl;

import com.wiki.admin.sys.attachment.dao.IAdminAttachmentMainDao;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentListVo;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentMainDo;
import com.wiki.admin.sys.attachment.model.mapper.AdminAttachmentMainMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 附件-信息 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Repository
public class AdminAttachmentMainDaoImpl implements IAdminAttachmentMainDao {

    private final AdminAttachmentMainMapper mapper;

    public AdminAttachmentMainDaoImpl(AdminAttachmentMainMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AdminAttachmentMainDo selectById(String fieldId) {
        return mapper.selectById(fieldId);
    }

    @Override
    public List<AdminAttachmentListVo> selectByCondition(String whereSql, String orderBySql, long offset,
                                                          int pageSize, boolean needPage, Map<String, Object> params) {
        return mapper.selectByCondition(whereSql, orderBySql, offset, pageSize, needPage, params);
    }

    @Override
    public long countByCondition(String whereSql, Map<String, Object> params) {
        return mapper.countByCondition(whereSql, params);
    }

    @Override
    public int insert(AdminAttachmentMainDo attachment) {
        return mapper.insert(attachment);
    }

    @Override
    public int updateDeleteFlag(String fieldId, Integer fieldDeleteFlag) {
        return mapper.updateDeleteFlag(fieldId, fieldDeleteFlag);
    }

    @Override
    public int updateOrder(String fieldId, Integer fieldOrder) {
        return mapper.updateOrder(fieldId, fieldOrder);
    }

    @Override
    public List<AdminAttachmentMainDo> selectByModel(String fieldModelName, String fieldModelId, String fieldKey) {
        return mapper.selectByModel(fieldModelName, fieldModelId, fieldKey);
    }
}
