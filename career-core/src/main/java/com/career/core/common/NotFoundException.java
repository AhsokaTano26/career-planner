package com.career.core.common;

/**
 * 业务异常：资源不存在（404），由全局异常处理器转为 HTTP 404。
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
