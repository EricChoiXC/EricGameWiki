package com.wiki.admin.sys.org.dao.impl;

import com.wiki.admin.sys.org.dao.IOrgUserPasswordLogDao;
import com.wiki.admin.sys.org.model.dto.OrgUserPasswordLogDo;
import com.wiki.admin.sys.org.model.mapper.OrgUserPasswordLogMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户与权限-用户密码变更记录 Dao 默认实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Repository
public class OrgUserPasswordLogDaoImpl implements IOrgUserPasswordLogDao {

    private final OrgUserPasswordLogMapper mapper;

    public OrgUserPasswordLogDaoImpl(OrgUserPasswordLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<OrgUserPasswordLogDo> selectByUserId(String fieldUserId, int limit) {
        return mapper.selectByUserId(fieldUserId, limit);
    }

    @Override
    public java.time.LocalDateTime selectLatestChangeTime(String fieldUserId) {
        return mapper.selectLatestChangeTime(fieldUserId);
    }

    @Override
    public int insert(OrgUserPasswordLogDo log) {
        return mapper.insert(log);
    }
}
