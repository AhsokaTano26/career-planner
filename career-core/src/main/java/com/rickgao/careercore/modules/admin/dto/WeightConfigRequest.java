package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 更新推荐权重请求体。对齐 openapi WeightConfig(创建 DRAFT,status 由后端控制)。 */
@Data
public class WeightConfigRequest {

    @NotBlank(message = "版本号不能为空")
    private String version;

    private Weights weights;
    private Double minConfidence;
    private Integer topN;

    @Data
    public static class Weights {
        private Double interest;
        private Double values;
        private Double ability;
        private Double academic;
        private Double tendency;
        private Double practice;
    }
}
