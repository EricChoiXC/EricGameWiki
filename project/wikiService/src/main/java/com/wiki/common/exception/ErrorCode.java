package com.wiki.common.exception;

import lombok.Getter;

/**
 * 通用错误码枚举，与 {@code docs/common/接口公共规范.md} 3.7 错误码约定一致。
 * 各模块如需扩展模块特有错误码，应在模块包内单独定义枚举，公共错误码统一引用本类。
 */
@Getter
public enum ErrorCode {

    SUCCESS("SUCCESS", 200, "操作成功"),
    BAD_REQUEST("BAD_REQUEST", 400, "参数校验失败"),
    UNAUTHORIZED("UNAUTHORIZED", 401, "未登录或Token过期"),
    FORBIDDEN("FORBIDDEN", 403, "无操作权限"),
    NOT_FOUND("NOT_FOUND", 404, "资源不存在"),
    CONFLICT("CONFLICT", 409, "数据冲突"),
    INTERNAL_ERROR("INTERNAL_ERROR", 500, "服务器内部错误");

    /**
     * 信息码，出现在响应报文 code 字段。
     */
    private final String code;

    /**
     * HTTP 状态码。
     */
    private final int httpCode;

    /**
     * 默认信息描述。
     */
    private final String message;

    ErrorCode(String code, int httpCode, String message) {
        this.code = code;
        this.httpCode = httpCode;
        this.message = message;
    }
}
