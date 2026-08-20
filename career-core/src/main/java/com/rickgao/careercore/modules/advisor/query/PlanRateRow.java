package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

/** 计划任务完成率查询行 */
@Data
public class PlanRateRow {

    private String studentId;
    private Long totalTasks;
    private Long doneTasks;
}
