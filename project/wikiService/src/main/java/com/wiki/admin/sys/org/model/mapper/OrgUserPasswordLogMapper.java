package com.wiki.admin.sys.org.model.mapper;

import com.wiki.admin.sys.org.model.dto.OrgUserPasswordLogDo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户与权限-用户密码变更记录 MyBatis Mapper，对应 {@code admin_org_user_password_log} 表。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface OrgUserPasswordLogMapper {

    /** 按用户ID查询历史密码（按变更时间降序，limit 条数控制扫描量） */
    List<OrgUserPasswordLogDo> selectByUserId(@Param("fieldUserId") String fieldUserId, @Param("limit") int limit);

    /** 查询用户最近一次密码变更时间（用于密码过期校验） */
    java.time.LocalDateTime selectLatestChangeTime(@Param("fieldUserId") String fieldUserId);

    int insert(OrgUserPasswordLogDo log);
}
