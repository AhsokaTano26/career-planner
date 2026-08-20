package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

import java.time.LocalDateTime;

/** 最近复盘时间查询行 */
@Data
public class LastReviewRow {

    private String studentId;
    private LocalDateTime lastReviewAt;
}
