package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

import java.time.LocalDateTime;

/** 阶段复盘查询行 */
@Data
public class ReviewRow {

    private String id;
    private String studentId;
    private String cycle;
    private String status;
    private String contentJson;
    private String aiSummary;
    private String aiSuggestJson;
    private Boolean advisorRequested;
    private String advisorReply;
    private LocalDateTime submittedAt;
}
