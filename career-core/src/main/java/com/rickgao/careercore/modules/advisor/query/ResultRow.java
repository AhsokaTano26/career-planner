package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

/** 推荐结果查询行 */
@Data
public class ResultRow {

    private String runId;
    private String directionId;
    private Double score;
    private Integer rank;
    private String confidence;
    private String reasonsJson;
    private String strengthsJson;
    private String gapsJson;
    private String semesterActionsJson;
    private String feedbackJson;
}
