package com.wiki.common.model.response;

import com.wiki.common.exception.ErrorCode;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 通用响应报文，对应 {@code docs/common/接口公共规范.md} 3.6 通用响应报文格式。
 * <p>
 * 报文结构：
 * <pre>
 * {
 *   "success": true,
 *   "httpCode": 200,
 *   "code": "SUCCESS",
 *   "message": "操作成功",
 *   "data": { /* VO 类对象 *&#47;},
 *   "list": [],
 *   "query": { /* 分页响应信息 *&#47;},
 *   "map": { /* 其他参数 *&#47;}
 * }
 * </pre>
 * <p>
 * Controller 返回响应时使用本类封装，泛型 T 为模块 VO 类型。
 * data 对象优先仅返回数据库表有的字段，敏感字段（密码、Token 等）必须剥离。
 *
 * @param <T> 响应 VO 类型
 */
@Data
public class ApiResponse<T> {

    /** 是否成功 */
    private Boolean success;

    /** HTTP 状态码 */
    private Integer httpCode;

    /** 信息码 */
    private String code;

    /** 信息描述 */
    private String message;

    /** 响应 VO 对象 */
    private T data;

    /** 响应列表，查询类接口使用 */
    private List<T> list;

    /** 分页响应信息 */
    private QueryResponse query;

    /** 除 data / list / query 以外的其他参数 */
    private Map<String, Object> map;

    /**
     * 构造成功响应（无数据）。
     */
    public static <T> ApiResponse<T> success() {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(Boolean.TRUE);
        response.setHttpCode(ErrorCode.SUCCESS.getHttpCode());
        response.setCode(ErrorCode.SUCCESS.getCode());
        response.setMessage(ErrorCode.SUCCESS.getMessage());
        return response;
    }

    /**
     * 构造成功响应（单个 data）。
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = success();
        response.setData(data);
        return response;
    }

    /**
     * 构造成功响应（列表）。
     */
    public static <T> ApiResponse<T> success(List<T> list) {
        ApiResponse<T> response = success();
        response.setList(list);
        return response;
    }

    /**
     * 构造成功响应（列表 + 分页信息）。
     */
    public static <T> ApiResponse<T> success(List<T> list, QueryResponse query) {
        ApiResponse<T> response = success();
        response.setList(list);
        response.setQuery(query);
        return response;
    }

    /**
     * 构造失败响应（使用错误码默认 message）。
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(Boolean.FALSE);
        response.setHttpCode(errorCode.getHttpCode());
        response.setCode(errorCode.getCode());
        response.setMessage(errorCode.getMessage());
        return response;
    }

    /**
     * 构造失败响应（自定义 message）。
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(Boolean.FALSE);
        response.setHttpCode(errorCode.getHttpCode());
        response.setCode(errorCode.getCode());
        response.setMessage(message);
        return response;
    }
}
