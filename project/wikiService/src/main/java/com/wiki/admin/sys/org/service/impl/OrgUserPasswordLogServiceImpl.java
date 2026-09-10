package com.wiki.admin.sys.org.service.impl;

import com.wiki.admin.sys.org.dao.IOrgUserPasswordLogDao;
import com.wiki.admin.sys.org.model.dto.OrgUserPasswordLogDo;
import com.wiki.admin.sys.org.service.IOrgUserPasswordLogService;
import com.wiki.common.util.IDUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户与权限-用户密码变更记录 Service 实现。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Service
@RequiredArgsConstructor
public class OrgUserPasswordLogServiceImpl implements IOrgUserPasswordLogService {

    private static final int DEFAULT_SCAN_LIMIT = 10;

    private final IOrgUserPasswordLogDao passwordLogDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(String userId, String newPassword) {
        OrgUserPasswordLogDo log = new OrgUserPasswordLogDo();
        log.setFieldId(IDUtil.initID());
        log.setFieldUserId(userId);
        log.setFieldChangeTime(LocalDateTime.now());
        log.setFieldNewPassword(newPassword);
        passwordLogDao.insert(log);
    }

    @Override
    public List<OrgUserPasswordLogDo> loadRecent(String userId, int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_SCAN_LIMIT : limit;
        return passwordLogDao.selectByUserId(userId, safeLimit);
    }

    @Override
    public java.time.LocalDateTime loadLatestChangeTime(String userId) {
        return passwordLogDao.selectLatestChangeTime(userId);
    }
}
