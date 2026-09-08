package com.rickgao.careercore.config;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * JWT 配置(secret 在 application-local.yml 中,已 gitignore)。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties implements InitializingBean {

    /** 签名密钥(HS256 要求 ≥ 32 字节) */
    private String secret;

    /** 访问令牌有效期(秒),默认 2 小时 */
    private long accessTokenTtlSeconds = 7200;

    /** 刷新令牌有效期(秒),默认 7 天 */
    private long refreshTokenTtlSeconds = 604800;

    @Override
    public void afterPropertiesSet() {
        // 复审加固：短密钥直接启动失败（fail-fast），避免可暴力破解的弱签名上线
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret 长度不足 32 字节，请配置强随机密钥后重启");
        }
    }
}
