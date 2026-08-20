package com.rickgao.careercore.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置密码请求(POST /api/v1/auth/password/reset)。
 */
@Data
public class PasswordResetRequest {

    @NotBlank(message = "学号不能为空")
    @Size(max = 32, message = "学号长度不能超过 32")
    private String studentNo;

    @NotBlank(message = "新初始密码不能为空")
    @Size(min = 6, max = 128, message = "新密码长度为 6-128 位")
    private String newPassword;

    @Size(max = 200, message = "重置原因长度不能超过 200")
    private String reason;
}
