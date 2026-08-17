package com.rickgao.careercore.modules.advisor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 填写指导意见 / 提出建议任务 / 建议重新测评 请求体。
 * 对齐 openapi GuidanceCommentRequest。
 */
@Data
public class GuidanceCommentRequest {

    @NotBlank(message = "指导意见正文不能为空")
    @Size(max = 2000, message = "指导意见正文不能超过 2000 字")
    private String content;

    @NotNull(message = "adviceType 不能为空")
    private String adviceType;

    @Size(max = 500, message = "建议任务不能超过 500 字")
    private String suggestedTask;

    @Size(max = 500, message = "建议重新测评原因不能超过 500 字")
    private String retestReason;
}
