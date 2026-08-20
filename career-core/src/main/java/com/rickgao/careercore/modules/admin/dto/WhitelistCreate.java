package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增白名单请求体。对齐 openapi WhitelistCreate。
 */
@Data
public class WhitelistCreate {

    @NotBlank(message = "学号不能为空")
    @Size(max = 32, message = "学号不能超过 32 位")
    private String studentNo;

    @Size(max = 50, message = "班级不能超过 50 字")
    private String className;

    @Size(max = 32, message = "校验码不能超过 32 位")
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "校验码只能包含字母和数字")
    private String verifyCode;
}
