package com.rickgao.careercore.modules.auth.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.util.IpUtil;
import com.rickgao.careercore.modules.auth.dto.ConsentRequest;
import com.rickgao.careercore.modules.auth.dto.CurrentUserUpdateRequest;
import com.rickgao.careercore.modules.auth.dto.LoginRequest;
import com.rickgao.careercore.modules.auth.dto.PasswordChangeRequest;
import com.rickgao.careercore.modules.auth.dto.PasswordResetRequest;
import com.rickgao.careercore.modules.auth.dto.RefreshRequest;
import com.rickgao.careercore.modules.auth.dto.RegisterRequest;
import com.rickgao.careercore.modules.auth.service.AuthService;
import com.rickgao.careercore.modules.auth.vo.ConsentStatusVO;
import com.rickgao.careercore.modules.auth.vo.CurrentUserVO;
import com.rickgao.careercore.modules.auth.vo.TokenVO;
import com.rickgao.careercore.security.LoginUser;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与账号模块接口(9 个)。
 * 依据 Apifox 接口文档:/api/v1/auth/*
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 学号注册 */
    @PostMapping("/register")
    public ApiResponse<TokenVO> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.register(request, IpUtil.getClientIp(httpRequest)));
    }

    /** 登录 */
    @PostMapping("/login")
    public ApiResponse<TokenVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.login(request, IpUtil.getClientIp(httpRequest)));
    }

    /** 刷新访问令牌 */
    @PostMapping("/refresh")
    public ApiResponse<TokenVO> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.refresh(request, IpUtil.getClientIp(httpRequest)));
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        LoginUser user = SecurityUtils.currentUser();
        authService.logout(user.getId(), user.getJti(), user.getTokenExpiresAt(), IpUtil.getClientIp(httpRequest));
        return ApiResponse.ok();
    }

    /** 当前用户信息 */
    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> me() {
        return ApiResponse.ok(authService.me(SecurityUtils.currentUserId()));
    }

    /** 当前登录用户维护自己的展示姓名。 */
    @PatchMapping("/me")
    public ApiResponse<CurrentUserVO> updateMe(@Valid @RequestBody CurrentUserUpdateRequest request,
                                                HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.updateMe(SecurityUtils.currentUserId(), request, IpUtil.getClientIp(httpRequest)));
    }

    /** 修改密码 */
    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request, HttpServletRequest httpRequest) {
        authService.changePassword(SecurityUtils.currentUserId(), request, IpUtil.getClientIp(httpRequest));
        return ApiResponse.ok();
    }

    /** 管理员重置密码 */
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request, HttpServletRequest httpRequest) {
        authService.resetPassword(SecurityUtils.currentUserId(), request, IpUtil.getClientIp(httpRequest));
        return ApiResponse.ok();
    }

    /** 同意隐私授权 */
    @PostMapping("/privacy-consent")
    public ApiResponse<ConsentStatusVO> consent(@Valid @RequestBody ConsentRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.consent(SecurityUtils.currentUserId(), request, IpUtil.getClientIp(httpRequest)));
    }

    /** 查询授权状态 */
    @GetMapping("/privacy-consent/status")
    public ApiResponse<ConsentStatusVO> consentStatus() {
        return ApiResponse.ok(authService.consentStatus(SecurityUtils.currentUserId()));
    }
}
