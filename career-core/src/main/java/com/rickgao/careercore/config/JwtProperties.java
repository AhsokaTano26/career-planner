package com.rickgao.careercore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置(secret 在 application-local.yml 中,已 gitignore)。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥(HS256 要求 ≥ 32 字节) */
    private String secret;

    /** 访问令牌有效期(秒),默认 2 小时 */
    private long accessTokenTtlSeconds = 7200;

    /** 刷新令牌有效期(秒),默认 7 天 */
    private long refreshTokenTtlSeconds = 604800;
}
