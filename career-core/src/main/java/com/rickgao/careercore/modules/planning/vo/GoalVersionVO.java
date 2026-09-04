package com.rickgao.careercore.modules.planning.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/** 目标版本历史 VO。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoalVersionVO {

    private String version;
    private String primaryDirectionId;
    private String backupDirectionId;
    private String changeReason;
    private LocalDateTime changedAt;
    private String changedBy;
}

