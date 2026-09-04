package com.rickgao.careercore.modules.ai.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关高阶文本生成响应（POST /api/v1/gateway/generate 200）。
 * 字段对齐 OpenAPI GatewayGenerateResponse schema（text/model/requestId/durationMs 必填）。
 *
 * <p>Demo 精简点 / 后续迭代替换位置:totalTokens 由网关 usage 字段提取，
 * 网关未返回时填 null（前端按字段缺省处理）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayGenerateResponse {

    private String text;

    private String model;

    private String requestId;

    private Long durationMs;

    private Integer totalTokens;

    private String disclaimer;
}
