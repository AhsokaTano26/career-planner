package com.rickgao.careercore.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 网关高阶文本生成请求（POST /api/v1/gateway/generate）。
 * 无需 token（OpenAPI security: []，由 SecurityConfig 放行）。
 *
 * <p>Demo 精简点 / 后续迭代替换位置:
 *  - scene/modelGroup/userRef/promptVersion 仅做透传，不在 career-core 做归因或持久化；
 *  - temperature 默认 0.7、maxTokens 默认 500，遵循 OpenAPI schema 默认值。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayGenerateRequest {

    @NotEmpty(message = "messages 不能为空")
    @Valid
    private List<GatewayMessage> messages;

    private String scene;

    private String modelGroup;

    private Double temperature;

    private Integer maxTokens;

    private String userRef;

    private String promptVersion;
}
