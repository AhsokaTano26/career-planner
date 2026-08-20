package com.rickgao.careercore.common.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 统一业务码与 HTTP 状态映射。
 * 依据 Apifox 接口文档 ApiResponse 定义与《开发设计说明书》12.3 错误码。
 */
@Getter
public enum ResultCode {
    OK("OK", HttpStatus.OK, "success"),
    AUTH_REQUIRED("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED, "未登录或令牌失效"),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "无权访问目标资源"),
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "请求字段或业务参数错误"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "资源不存在"),
    STATE_CONFLICT("STATE_CONFLICT", HttpStatus.CONFLICT, "当前状态不允许操作"),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "未预期系统错误");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ResultCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
