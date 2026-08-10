package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

import java.time.LocalDateTime;

/** 任务打卡查询行 */
@Data
public class CheckinRow {

    private String id;
    private String taskId;
    private String doneDesc;
    private String gains;
    private String difficulties;
    private String proofUrl;
    private LocalDateTime checkedInAt;
}
