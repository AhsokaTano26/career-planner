package com.rickgao.careercore.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新访问令牌请求(POST /api/v1/auth/refresh)。
 */
@Data
public class RefreshRequest {

    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
