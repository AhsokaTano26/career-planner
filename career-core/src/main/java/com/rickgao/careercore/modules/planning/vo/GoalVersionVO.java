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
    /** 主方向中文名。 */
    private String primaryDirectionName;
    private String backupDirectionId;
    /** 备选方向中文名。 */
    private String backupDirectionName;
    private String changeReason;
    private LocalDateTime changedAt;
    private String changedBy;
}

