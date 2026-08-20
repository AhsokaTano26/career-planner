package com.rickgao.careercore.common.response;

import com.rickgao.careercore.common.util.TraceIdUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 统一响应体 {code, message, data, traceId, timestamp}。
 * 遵循 Apifox 接口文档 ApiResponse 定义(设计说明书 12.2)。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private String code;
    private String message;
    private T data;
    private String traceId;
    private String timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ResultCode.OK.getCode(), "success", data, TraceIdUtil.generate(), now());
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> fail(ResultCode resultCode, String message) {
        return new ApiResponse<>(resultCode.getCode(), message, null, TraceIdUtil.generate(), now());
    }

    public static <T> ApiResponse<T> fail(ResultCode resultCode) {
        return fail(resultCode, resultCode.getMessage());
    }

    private static String now() {
        return OffsetDateTime.now(ZoneOffset.ofHours(8)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
