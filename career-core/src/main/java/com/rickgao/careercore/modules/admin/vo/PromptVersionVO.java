package com.rickgao.careercore.modules.admin.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/** 提示词版本 VO。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptVersionVO {

    private String id;
    private String scene;
    private String version;
    private String status;
    private String content;
    private LocalDateTime publishedAt;
    private String publishedBy;
}

