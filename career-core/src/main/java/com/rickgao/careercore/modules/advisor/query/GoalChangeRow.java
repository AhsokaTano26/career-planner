package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

/** 目标版本变更次数查询行 */
@Data
public class GoalChangeRow {

    private String studentId;
    private Long changeCount;
}
