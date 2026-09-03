package com.rickgao.careercore.modules.ai.dto;

import lombok.Data;

/**
 * 月度任务条目。
 */
@Data
public class AiMonthlyTask {

    private String month;
    private String title;
    private String taskType;
    private Double estimatedHours;
}
