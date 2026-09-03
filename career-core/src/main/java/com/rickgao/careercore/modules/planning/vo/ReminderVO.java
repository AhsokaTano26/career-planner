package com.rickgao.careercore.modules.planning.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内提醒 VO。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReminderVO {

    private String id;
    private String type;
    private String title;
    private String content;
    private Boolean read;
    private LocalDateTime createdAt;
}
