package com.rickgao.careercore.modules.planning.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 站内提醒，对齐 reminder 表。 */
@Data
public class Reminder {

    private String id;
    private String studentId;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

