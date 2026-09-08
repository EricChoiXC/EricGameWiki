package com.wiki.common.model.request;

import com.wiki.common.constant.CommonConstants;
import com.wiki.common.model.query.QueryCondition;
import lombok.Data;

/**
 * 请求报文中 {@code query} 字段的分页与查询条件封装，对应
 * {@code docs/common/接口公共规范.md} 3.5 通用请求报文格式中的 query 节点。
 * <p>
 * 字段说明：
 * <ul>
 *   <li>{@code pageNum}：页码，从 1 开始</li>
 *   <li>{@code pageSize}：每页记录数</li>
 *   <li>{@code needPage}：为 false 时查询不分页</li>
 *   <li>{@code data}：查询条件树，遵循 {@code docs/common/查询标准.md}</li>
 * </ul>
 */
@Data
public class QueryRequest {

    /** 页码，从 1 开始 */
    private Integer pageNum = CommonConstants.DEFAULT_PAGE_NUM;

    /** 每页记录数 */
    private Integer pageSize = CommonConstants.DEFAULT_PAGE_SIZE;

    /** 是否分页，false 时查询不分页 */
    private Boolean needPage = Boolean.TRUE;

    /** 查询条件树 */
    private QueryCondition data;
}
