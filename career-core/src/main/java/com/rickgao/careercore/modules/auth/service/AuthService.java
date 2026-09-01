package com.rickgao.careercore.modules.auth.service;

import com.rickgao.careercore.modules.auth.dto.ConsentRequest;
import com.rickgao.careercore.modules.auth.dto.CurrentUserUpdateRequest;
import com.rickgao.careercore.modules.auth.dto.LoginRequest;
import com.rickgao.careercore.modules.auth.dto.PasswordChangeRequest;
import com.rickgao.careercore.modules.auth.dto.PasswordResetRequest;
import com.rickgao.careercore.modules.auth.dto.RefreshRequest;
import com.rickgao.careercore.modules.auth.dto.RegisterRequest;
import com.rickgao.careercore.modules.auth.vo.ConsentStatusVO;
import com.rickgao.careercore.modules.auth.vo.CurrentUserVO;
import com.rickgao.careercore.modules.auth.vo.TokenVO;

import java.time.LocalDateTime;

/**
 * 认证与账号应用服务接口。
 */
public interface AuthService {

    TokenVO register(RegisterRequest request, String ip);

    TokenVO login(LoginRequest request, String ip);

    TokenVO refresh(RefreshRequest request, String ip);

    void logout(String userId, String jti, LocalDateTime tokenExpiresAt, String ip);

    CurrentUserVO me(String userId);

    CurrentUserVO updateMe(String userId, CurrentUserUpdateRequest request, String ip);

    void changePassword(String userId, PasswordChangeRequest request, String ip);

    void resetPassword(String operatorId, PasswordResetRequest request, String ip);

    ConsentStatusVO consent(String userId, ConsentRequest request, String ip);

    ConsentStatusVO consentStatus(String userId);
}
