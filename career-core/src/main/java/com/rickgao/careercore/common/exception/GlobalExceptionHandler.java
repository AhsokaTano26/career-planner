package com.rickgao.careercore.common.exception;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理器:将异常统一转换为 ApiResponse,并设置对应 HTTP 状态码。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException e) {
        return build(e.getResultCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "请求参数校验失败" : fieldError.getDefaultMessage();
        return build(ResultCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException e) {
        return build(ResultCode.VALIDATION_ERROR, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return build(ResultCode.VALIDATION_ERROR, "请求体格式错误或无法解析");
    }

    /** 方法级权限(@PreAuthorize)拒绝:角色不足/无权限 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return build(ResultCode.FORBIDDEN, "无权访问目标资源");
    }

    /** 方法级认证异常 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e) {
        return build(ResultCode.AUTH_REQUIRED, "未登录或令牌失效");
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        return build(ResultCode.RESOURCE_NOT_FOUND, "资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        log.error("未预期异常", e);
        return build(ResultCode.INTERNAL_ERROR, ResultCode.INTERNAL_ERROR.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> build(ResultCode resultCode, String message) {
        // 统一返回 HTTP 200，通过业务码区分错误类型（对齐 Apifox 响应校验要求）
        return ResponseEntity.ok(ApiResponse.fail(resultCode, message));
    }
}
