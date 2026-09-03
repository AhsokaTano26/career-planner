package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 提示词版本，对齐 prompt_version 表。 */
@Data
public class PromptVersion {

    private String id;
    private String scene;
    private String version;
    private String status;
    private String content;
    private LocalDateTime publishedAt;
    private String publishedBy;
    private LocalDateTime createdAt;
}
