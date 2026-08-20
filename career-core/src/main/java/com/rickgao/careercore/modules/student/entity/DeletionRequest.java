package com.rickgao.careercore.modules.student.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 删除本人信息申请实体(deletion_request)。
 */
@Data
public class DeletionRequest {

    private String id;
    private String userId;
    private String reason;
    /** 状态:PENDING/PROCESSED/REJECTED */
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String processedBy;
}
