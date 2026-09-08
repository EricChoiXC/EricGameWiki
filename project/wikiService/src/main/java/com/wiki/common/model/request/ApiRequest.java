package com.wiki.common.model.request;

import lombok.Data;

import java.util.Map;

/**
 * 通用请求报文，对应 {@code docs/common/接口公共规范.md} 3.5 通用请求报文格式。
 * <p>
 * 报文结构：
 * <pre>
 * {
 *   "data": { /* VO 类对象 *&#47;},
 *   "query": { /* 分页请求信息 *&#47;},
 *   "map": { /* 其他参数 *&#47;}
 * }
 * </pre>
 * <p>
 * Controller 接收请求时使用本类作为请求体，泛型 T 为模块 VO 类型。
 * data 对象优先仅传入数据库表有的字段。
 *
 * @param <T> 请求 VO 类型
 */
@Data
public class ApiRequest<T> {

    /** 请求 VO 对象 */
    private T data;

    /** 分页与查询条件 */
    private QueryRequest query;

    /** 除 data / query 以外的其他参数 */
    private Map<String, Object> map;
}
