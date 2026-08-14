package com.career.core.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * 全局异常处理：统一返回 { code, message, data }。
 * Demo 阶段只处理“参数缺失(400)”、“接口不存在(404)”与“未知异常(500)”三类。
 * 错误响应的 data 恒为空对象 {}（符合“data 不允许为 null”的契约）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, Object>> handleBadRequest(BadRequestException e) {
        return ApiResponse.error(40001, e.getMessage());
    }

    /** 资源不存在 → 404（契约定义：画像/推荐详情/反馈等） */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Map<String, Object>> handleNotFound(NotFoundException e) {
        return ApiResponse.error(40400, e.getMessage());
    }

    /** 未匹配到任何 Controller 的路径 → 404（否则会被兜底 500 误伤） */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Map<String, Object>> handleNotFound(NoResourceFoundException e) {
        return ApiResponse.error(40400, "接口不存在");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Map<String, Object>> handleException(Exception e) {
        log.error("未处理异常", e);
        return ApiResponse.error(50000, "服务器内部错误");
    }
}
