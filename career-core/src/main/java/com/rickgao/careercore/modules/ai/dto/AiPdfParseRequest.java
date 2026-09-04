package com.rickgao.careercore.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 解析培养方案 PDF 请求（POST /api/v1/ai/pdf/parse）。
 */
@Data
public class AiPdfParseRequest {

    @NotBlank(message = "jobId 不能为空")
    private String jobId;

    @NotBlank(message = "fileUrl 不能为空")
    private String fileUrl;

    @NotBlank(message = "filename 不能为空")
    private String filename;
}
