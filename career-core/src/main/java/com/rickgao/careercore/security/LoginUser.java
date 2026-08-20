package com.rickgao.careercore.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户(存于 SecurityContext,由 JwtAuthFilter 解析)。
 */
@Data
@AllArgsConstructor
public class LoginUser {

    private String id;
    private String username;
    private String role;
    private String jti;
    private LocalDateTime tokenExpiresAt;
}
