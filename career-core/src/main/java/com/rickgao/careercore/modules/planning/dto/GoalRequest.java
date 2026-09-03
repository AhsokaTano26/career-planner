package com.rickgao.careercore.modules.planning.dto;

import lombok.Data;

/** 设置 / 变更目标请求。 */
@Data
public class GoalRequest {

    private String primaryDirectionId;
    private String backupDirectionId;
    private String changeReason;
}
