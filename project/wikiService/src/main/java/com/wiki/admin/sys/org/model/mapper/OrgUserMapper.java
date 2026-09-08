package com.wiki.admin.sys.org.model.mapper;

import com.wiki.admin.sys.org.model.dto.OrgUserDo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户与权限-用户 MyBatis Mapper，对应 {@code admin_org_user} 表。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface OrgUserMapper {

    /** 按主键查询（密码字段照常返回，由 Service 在出参时剥离） */
    OrgUserDo selectById(@Param("fieldId") String fieldId);

    /** 按登录名查询 */
    OrgUserDo selectByLoginName(@Param("fieldLoginName") String fieldLoginName);

    /** 按手机号查询 */
    OrgUserDo selectByPhone(@Param("fieldPhone") String fieldPhone);

    /** 按邮箱查询 */
    OrgUserDo selectByEmail(@Param("fieldEmail") String fieldEmail);

    /** 通用查询（含动态条件 + 分页 + 排序），whereSql / orderBySql 由 Service 通过 Map 注入 */
    List<OrgUserDo> selectByCondition(@Param("whereSql") String whereSql,
                                      @Param("orderBySql") String orderBySql,
                                      @Param("offset") long offset,
                                      @Param("pageSize") int pageSize,
                                      @Param("needPage") boolean needPage,
                                      @Param("params") java.util.Map<String, Object> params);

    /** 通用查询对应的总数 */
    long countByCondition(@Param("whereSql") String whereSql, @Param("params") java.util.Map<String, Object> params);

    /** 新增用户 */
    int insert(OrgUserDo user);

    /** 更新用户基本信息（不含密码） */
    int update(OrgUserDo user);

    /** 更新密码 */
    int updatePassword(@Param("fieldId") String fieldId, @Param("fieldPassword") String fieldPassword);

    /** 更新最后登录信息 */
    int updateLastLogin(@Param("fieldId") String fieldId,
                       @Param("fieldLoginTime") LocalDateTime fieldLoginTime,
                       @Param("fieldLoginIp") String fieldLoginIp);

    /** 更新状态 */
    int updateStatus(@Param("fieldId") String fieldId, @Param("fieldStatus") String fieldStatus);

    /** 更新锁定标识与解锁时间 */
    int updateLock(@Param("fieldId") String fieldId,
                  @Param("fieldLockFlag") Integer fieldLockFlag,
                  @Param("fieldUnlockTime") LocalDateTime fieldUnlockTime);

    /** 解锁已到期的用户（unlock_time <= now） */
    int unlockExpired(@Param("now") LocalDateTime now);
}
