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
    /**
     * 刷新令牌 SHA-256 哈希（2026-09 复审：防拖库冒用；双写兼容一轮——新行同时写
     * token 明文 + tokenHash，读取优先哈希、回退明文并回填；下轮切读只走哈希、
     * 写停明文。后续迭代替换位置：token 列置空 + 删 findByToken）。
     */
    private String tokenHash;
    private LocalDateTime expiresAt;
    private Boolean revoked;
    private LocalDateTime createdAt;
}
