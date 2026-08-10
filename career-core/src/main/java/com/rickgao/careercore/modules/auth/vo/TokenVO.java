package com.rickgao.careercore.modules.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 令牌响应 VO(TokenResponse)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    private String accessToken;
    private String refreshToken;
    /** 访问令牌有效秒数 */
    private long expiresIn;
    private String tokenType;
    /** 是否首次登录(需完成隐私授权) */
    private Boolean firstLogin;
    private CurrentUserVO user;
}
