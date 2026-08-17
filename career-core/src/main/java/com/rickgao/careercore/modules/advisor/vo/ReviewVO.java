package com.rickgao.careercore.modules.advisor.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 阶段复盘。对齐 openapi Review。
 */
@Data
public class ReviewVO {

    private String id;
    private String cycle;
    /** DRAFT / SUBMITTED */
    private String status;
    private ReviewContent content;
    private String aiSummary;
    private List<String> aiSuggest;
    private Boolean advisorRequested;
    private String advisorReply;
    private LocalDateTime submittedAt;

    @Data
    public static class ReviewContent {
        private String done;
        private String undone;
        private String interest;
        private String ability;
        private String next;
    }
}
