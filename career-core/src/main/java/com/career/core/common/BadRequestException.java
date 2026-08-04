package com.career.core.common;

/**
 * 业务异常：用于“必填字段缺失”等可控错误，由全局异常处理器转为 HTTP 400。
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
