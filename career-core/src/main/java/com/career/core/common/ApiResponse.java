package com.career.core.common;

import java.util.Collections;
import java.util.Map;

/**
 * 统一响应结构：{ code, message, data }
 * code=0 表示成功；非 0 表示业务/参数错误。
 * 说明：data 恒为对象（成功为业务数据，错误为空对象 {}），避免契约校验“data 不允许为 null”不通过。
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    /** 错误响应：data 返回空对象 {}，而非 null */
    public static ApiResponse<Map<String, Object>> error(int code, String message) {
        return new ApiResponse<>(code, message, Collections.emptyMap());
    }
}
