package com.rickgao.careercore.common.idempotency;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 写接口幂等记录(idempotency_record)。
 */
@Data
public class IdempotencyRecord {

    private String id;
    private String userId;
    private String endpoint;
    private String requestKey;
    private String requestHash;
    private String status;
    private String responseCode;
    private String responseBody;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
