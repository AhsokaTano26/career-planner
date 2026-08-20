package com.rickgao.careercore.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 同意隐私授权请求(POST /api/v1/auth/privacy-consent)。
 */
@Data
public class ConsentRequest {

    @NotBlank(message = "授权版本号不能为空")
    @Size(max = 20, message = "版本号长度不能超过 20")
    private String version;
}
