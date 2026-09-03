package com.rickgao.careercore.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PDF 解析结果（POST /api/v1/ai/pdf/parse 200）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPdfParseVO {

    private String jobId;
    private String status;
    private Integer itemCount;
    private Double confidence;
}
