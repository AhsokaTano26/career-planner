package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** AI 调用日志(ai_call_log,由 AI 组写入)。 */
@Data
public class AiCallLog {

    private String id;
    private String requestId;
    private String userRef;
    private String scene;
    private String modelName;
    private String promptVersion;
    private Integer durationMs;
    private String status;
    private Integer tokenEstimate;
    private String requestHash;
    private String inputHash;
    private LocalDateTime createdAt;
}
