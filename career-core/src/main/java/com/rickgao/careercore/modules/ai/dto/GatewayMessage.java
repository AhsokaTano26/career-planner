package com.rickgao.careercore.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 网关消息条目（POST /api/v1/gateway/generate 的 messages 元素）。
 * Demo 精简点 / 后续迭代替换位置:role 当前仅识别 system/user/assistant，其他值透传网关。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayMessage {

    @NotBlank(message = "role 不能为空")
    private String role;

    @NotBlank(message = "content 不能为空")
    private String content;
}
