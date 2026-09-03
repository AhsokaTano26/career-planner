package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 管理端提示词版本创建请求。 */
@Data
public class PromptVersionRequest {

    @NotBlank(message = "scene 不能为空")
    private String scene;

    @NotBlank(message = "version 不能为空")
    private String version;

    @NotBlank(message = "content 不能为空")
    private String content;
}
