package com.rickgao.careercore.modules.advisor.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 画像快照。对齐 openapi ProfileSnapshot。
 */
@Data
public class ProfileSnapshotVO {

    private String id;
    private Integer version;
    private LocalDateTime generatedAt;
    private String sourceVersion;
    private Integer completeness;
    private List<DimensionValue> dimensions;
    private String summary;
    private List<String> strengths;
    private List<String> explore;
    private ProfileFeedback feedback;

    @Data
    public static class DimensionValue {
        private String key;
        private String name;
        private Double score;
    }

    @Data
    public static class ProfileFeedback {
        private String feedbackType;
        private String comment;
    }
}
