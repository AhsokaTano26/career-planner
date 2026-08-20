package com.rickgao.careercore.modules.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志实体(operation_audit_log)。
 */
@Data
public class OperationAuditLog {

    private String id;
    private String userId;
    /** 动作:REGISTER/LOGIN/LOGIN_FAIL/LOGOUT/PASSWORD_CHANGE/RESET_PASSWORD/PRIVACY_CONSENT/DELETION_REQUEST 等 */
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;
}
