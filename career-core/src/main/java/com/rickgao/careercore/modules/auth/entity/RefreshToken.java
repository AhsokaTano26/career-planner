package com.rickgao.careercore.modules.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 刷新令牌实体(refresh_token)。
 */
@Data
public class RefreshToken {

    private String id;
    private String userId;
    /** 刷新令牌(随机串) */
    private String token;
    private LocalDateTime expiresAt;
    private Boolean revoked;
    private LocalDateTime createdAt;
}
