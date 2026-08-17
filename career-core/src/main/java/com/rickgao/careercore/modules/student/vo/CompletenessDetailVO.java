package com.rickgao.careercore.modules.student.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资料完整度明细 VO(CompletenessDetail)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletenessDetailVO {

    /** 综合完整度(0-100) */
    private Integer score;
    /** 必填字段总数 */
    private Integer total;
    /** 已填字段数 */
    private Integer filled;
    /** 缺失字段清单 */
    private List<MissingField> missing;
    /** 按维度拆分 */
    private List<Dimension> dimensions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingField {
        private String key;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dimension {
        /** interest/values/ability/academic/tendency/practice */
        private String key;
        private String name;
        /** 该维度是否已填 */
        private Boolean filled;
        /** 是否必填 */
        private Boolean required;
    }
}
