package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** AI 调用日志。对齐 openapi AiCallLog。 */
@Data
public class AiCallLogVO {

    private String id;
    private LocalDateTime time;
    private String userRef;
    private String scene;
    private String modelName;
    private String promptVersion;
    private Integer durationMs;
    private String status;
    private Integer tokenEstimate;
    private String requestHash;
}
