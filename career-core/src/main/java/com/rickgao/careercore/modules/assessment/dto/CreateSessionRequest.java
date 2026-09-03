package com.rickgao.careercore.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建测评会话请求。
 */
@Data
public class CreateSessionRequest {

    @NotBlank(message = "questionnaireId 不能为空")
    private String questionnaireId;

    private String resumeSessionId;
}
