package com.wiki.admin.sys.attachment.model.mapper;

import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentListVo;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentMainDo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 附件-信息 MyBatis Mapper，对应 {@code admin_attachment_main} 表。
 * <p>
 * 列表查询联表 {@code admin_attachment_file} 与 {@code admin_org_user}，返回 {@link AdminAttachmentListVo}。
 *
 * @author Eric
 * @date 2026/9/8
 */
public interface AdminAttachmentMainMapper {

    /** 按主键查询附件信息 */
    AdminAttachmentMainDo selectById(@Param("fieldId") String fieldId);

    /** 通用查询（含动态条件 + 分页 + 排序），whereSql / orderBySql 由 Service 通过 Map 注入 */
    List<AdminAttachmentListVo> selectByCondition(@Param("whereSql") String whereSql,
                                                   @Param("orderBySql") String orderBySql,
                                                   @Param("offset") long offset,
                                                   @Param("pageSize") int pageSize,
                                                   @Param("needPage") boolean needPage,
                                                   @Param("params") Map<String, Object> params);

    /** 通用查询对应的总数 */
    long countByCondition(@Param("whereSql") String whereSql, @Param("params") Map<String, Object> params);

    /** 新增附件信息 */
    int insert(AdminAttachmentMainDo attachment);

    /** 逻辑删除附件（设置 field_delete_flag = 1） */
    int updateDeleteFlag(@Param("fieldId") String fieldId, @Param("fieldDeleteFlag") Integer fieldDeleteFlag);

    /** 更新排序 */
    int updateOrder(@Param("fieldId") String fieldId, @Param("fieldOrder") Integer fieldOrder);

    /** 按模型名称 + 模型id + key 查询附件列表（未删除） */
    List<AdminAttachmentMainDo> selectByModel(@Param("fieldModelName") String fieldModelName,
                                               @Param("fieldModelId") String fieldModelId,
                                               @Param("fieldKey") String fieldKey);
}
