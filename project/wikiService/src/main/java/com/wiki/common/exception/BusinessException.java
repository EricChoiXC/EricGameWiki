package com.wiki.common.exception;

import lombok.Getter;

/**
 * 基础业务异常类，遵循 {@code docs/common/业务流转公共规范.md} 4.8 异常流转约定。
 * <p>
 * 业务异常必须携带：错误码、可追踪原因、可选的权限码。
 * Dao/Mapper 抛出的数据访问异常由 IService 统一捕获并转换为本异常；
 * 跨模块调用异常由调用方 CRPService 统一捕获并封装为本异常。
 * Controller 通过 {@link com.wiki.common.advice.GlobalExceptionHandler} 统一封装为标准响应报文。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码，对应响应报文 code 字段 */
    private final ErrorCode errorCode;

    /** 可选权限码，用于鉴权失败时给出可追踪原因 */
    private final String permissionCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.permissionCode = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.permissionCode = null;
    }

    public BusinessException(ErrorCode errorCode, String message, String permissionCode) {
        super(message);
        this.errorCode = errorCode;
        this.permissionCode = permissionCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.permissionCode = null;
    }
}
