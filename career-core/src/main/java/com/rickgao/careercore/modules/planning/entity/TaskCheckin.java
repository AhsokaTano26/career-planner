package com.rickgao.careercore.modules.planning.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 任务打卡，对齐 task_checkin 表。 */
@Data
public class TaskCheckin {

    private String id;
    private String taskId;
    private String doneDesc;
    private String gains;
    private String difficulties;
    private String proofUrl;
    private LocalDateTime checkedInAt;
    private LocalDateTime createdAt;
}

