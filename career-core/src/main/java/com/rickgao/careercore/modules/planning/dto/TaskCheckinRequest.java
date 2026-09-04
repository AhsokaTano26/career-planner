package com.rickgao.careercore.modules.planning.dto;

import lombok.Data;

/** 任务打卡请求。 */
@Data
public class TaskCheckinRequest {

    private String doneDesc;
    private String gains;
    private String difficulties;
    private String proofUrl;
}

