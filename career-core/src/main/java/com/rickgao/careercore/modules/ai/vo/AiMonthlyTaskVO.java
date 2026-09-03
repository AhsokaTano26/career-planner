package com.rickgao.careercore.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 月度任务。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMonthlyTaskVO {

    private String month;
    private String title;
    private String taskType;
    private Double estimatedHours;
}
