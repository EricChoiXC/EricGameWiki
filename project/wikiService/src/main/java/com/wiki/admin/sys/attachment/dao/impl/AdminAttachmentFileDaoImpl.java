package com.wiki.admin.sys.attachment.dao.impl;

import com.wiki.admin.sys.attachment.dao.IAdminAttachmentFileDao;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentFileDo;
import com.wiki.admin.sys.attachment.model.mapper.AdminAttachmentFileMapper;
import org.springframework.stereotype.Repository;

/**
 * 附件-文件 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Repository
public class AdminAttachmentFileDaoImpl implements IAdminAttachmentFileDao {

    private final AdminAttachmentFileMapper mapper;

    public AdminAttachmentFileDaoImpl(AdminAttachmentFileMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AdminAttachmentFileDo selectById(String fieldId) {
        return mapper.selectById(fieldId);
    }

    @Override
    public int insert(AdminAttachmentFileDo file) {
        return mapper.insert(file);
    }

    @Override
    public int deleteById(String fieldId) {
        return mapper.deleteById(fieldId);
    }
}
