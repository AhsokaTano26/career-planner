package com.rickgao.careercore.modules.planning.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 更新任务（标题 / 月份 / 时长 / 状态）。 */
@Data
public class TaskStatusUpdate {

    private String month;
    private String title;
    private BigDecimal estHours;
    private String status;
    private String reason;
    private String note;
}

