package com.rickgao.careercore.modules.advisor.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生目标(主/备选)。对齐 openapi Goal。
 */
@Data
public class GoalVO {

    private GoalItem primary;
    private GoalItem backup;
    private String version;
    private LocalDateTime updatedAt;

    @Data
    public static class GoalItem {
        private String directionId;
        private String name;
        private LocalDateTime chosenAt;
    }
}
