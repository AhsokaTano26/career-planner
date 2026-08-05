package com.career.core.common;

/** 资源不存在业务异常，由全局异常处理器转换为 HTTP 404。 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
