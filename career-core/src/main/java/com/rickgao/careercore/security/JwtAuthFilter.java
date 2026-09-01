package com.rickgao.careercore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import com.rickgao.careercore.modules.auth.mapper.TokenBlacklistMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.List;

/**
 * JWT 认证过滤器:解析 Authorization: Bearer &lt;token&gt;,校验签名/过期/黑名单后写入 SecurityContext。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistMapper tokenBlacklistMapper;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtUtil jwtUtil, TokenBlacklistMapper tokenBlacklistMapper,
                         SysUserMapper sysUserMapper, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistMapper = tokenBlacklistMapper;
        this.sysUserMapper = sysUserMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                String jti = claims.getId();
                if (tokenBlacklistMapper.exists(jti) == 0) {
                    SysUser user = sysUserMapper.findById(claims.getSubject());
                    if (user == null) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    if (Boolean.TRUE.equals(user.getPasswordChangeRequired()) && !isPasswordChangeAllowed(request)) {
                        writePasswordChangeRequired(response);
                        return;
                    }
                    LoginUser loginUser = new LoginUser(
                            claims.getSubject(),
                            claims.get("username", String.class),
                            claims.get("role", String.class),
                            jti,
                            claims.getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            loginUser, null, List.of(new SimpleGrantedAuthority("ROLE_" + loginUser.getRole())));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // 令牌无效/过期/被列入黑名单:保持匿名,由 SecurityConfig 统一返回 401
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPasswordChangeAllowed(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/api/v1/auth/me".equals(uri)
                || "/api/v1/auth/me/password".equals(uri)
                || "/api/v1/auth/logout".equals(uri);
    }

    private void writePasswordChangeRequired(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ResultCode.FORBIDDEN, "首次登录请先修改初始密码")));
    }
}
