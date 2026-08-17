package com.rickgao.careercore.modules.advisor.vo;

import lombok.Data;

import java.util.List;

/**
 * 群体统计。对齐 openapi AdvisorStatistics。
 */
@Data
public class AdvisorStatisticsVO {

    private Integer totalStudents;
    private Integer assessedCount;
    private Integer planMadeCount;
    private Integer reviewedCount;
    private List<PathDistribution> pathDistribution;
    /** 平均任务完成率(%) */
    private Double taskCompletionRate;

    @Data
    public static class PathDistribution {
        /** graduate / employment / overseas / undecided */
        private String path;
        private Integer count;
    }
}
