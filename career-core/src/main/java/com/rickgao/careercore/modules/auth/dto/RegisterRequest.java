package com.rickgao.careercore.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 学号注册请求(POST /api/v1/auth/register)。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "学号不能为空")
    @Size(max = 32, message = "学号长度不能超过 32")
    private String studentNo;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过 50")
    private String name;

    @Size(max = 50, message = "班级长度不能超过 50")
    private String className;

    @NotBlank(message = "初始密码不能为空")
    @Size(max = 32, message = "初始密码长度不能超过 32")
    private String initialPassword;
}
