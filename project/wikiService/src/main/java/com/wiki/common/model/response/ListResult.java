package com.wiki.common.model.response;

import lombok.Data;

import java.util.List;

/**
 * 分页列表查询结果，承载 {@code records} 与分页元信息 {@code query}。
 * <p>
 * 供 Service 返回给 Controller，由 Controller 调用 {@link ApiResponse#success(List, QueryResponse)} 统一封装。
 *
 * @param <T> 列表元素类型
 * @author Eric
 * @date 2026/9/8
 */
@Data
public class ListResult<T> {

    /** 当前页记录 */
    private List<T> records;

    /** 分页元信息 */
    private QueryResponse query;

    public ListResult(List<T> records, QueryResponse query) {
        this.records = records;
        this.query = query;
    }

    /**
     * 便捷构造：不分页时使用。
     */
    public static <T> ListResult<T> of(List<T> records) {
        QueryResponse query = QueryResponse.of(records.size(), 1, records.size());
        return new ListResult<>(records, query);
    }
}
