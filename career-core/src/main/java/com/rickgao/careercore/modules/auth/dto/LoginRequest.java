package com.rickgao.careercore.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求(POST /api/v1/auth/login)。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度为 6-128 位")
    private String password;

    /** 角色(可选,通常由后端根据账号判断) */
    private String role;
}
