package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

import java.time.LocalDateTime;

/** 学生有效目标(主/备选,含方向名称)查询行 */
@Data
public class ActiveGoalRow {

    private String studentId;
    private String goalType;
    private String directionId;
    private String directionName;
    private String goalName;
    private LocalDateTime chosenAt;
    private LocalDateTime updatedAt;
    private Integer versionNo;
}
