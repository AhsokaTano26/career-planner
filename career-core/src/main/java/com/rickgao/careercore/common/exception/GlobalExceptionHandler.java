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
        // 复审加固：原始 message 可能拼接内部属性路径/值，统一对外文案，详情记日志
        log.warn("参数校验失败：{}", e.getMessage());
        return build(ResultCode.VALIDATION_ERROR, "请求参数校验失败");
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

    /**
     * 复审加固：唯一冲突（并发注册/重复复盘/重复收藏）统一 409，前端可按 STATE_CONFLICT
     * 提示“已存在/重复提交”而非 500。注意：message 取约束名，可能是英文键，仅用于定位。
     */
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(org.springframework.dao.DuplicateKeyException e) {
        log.warn("唯一约束冲突：{}", e.getMessage());
        return build(ResultCode.STATE_CONFLICT, "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException e) {
        log.warn("数据完整性冲突：{}", e.getMessage());
        return build(ResultCode.STATE_CONFLICT, "数据冲突，操作未执行");
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
        return ResponseEntity.status(resultCode.getHttpStatus()).body(ApiResponse.fail(resultCode, message));
    }
}
