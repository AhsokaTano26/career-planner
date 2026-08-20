package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 推荐权重配置。对齐 openapi WeightConfig。 */
@Data
public class WeightConfigVO {

    private String version;
    private Weights weights;
    private Double minConfidence;
    private Integer topN;
    private String status;
    private LocalDateTime publishedAt;

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
