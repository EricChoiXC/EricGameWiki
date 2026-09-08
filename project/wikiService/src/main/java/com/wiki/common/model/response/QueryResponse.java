package com.wiki.common.model.response;

import lombok.Data;

/**
 * 响应报文中 {@code query} 字段的分页响应信息，对应
 * {@code docs/common/接口公共规范.md} 3.6 通用响应报文格式中的 query 节点。
 */
@Data
public class QueryResponse {

    /** 总记录数 */
    private Long total;

    /** 总页数 */
    private Integer pages;

    /** 当前页码 */
    private Integer pageNum;

    /** 每页记录数 */
    private Integer pageSize;

    /**
     * 便捷构造分页响应。
     */
    public static QueryResponse of(long total, int pageNum, int pageSize) {
        QueryResponse response = new QueryResponse();
        response.setTotal(total);
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        response.setPages(pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0);
        return response;
    }
}
