package com.rickgao.careercore.security;

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
import java.time.ZoneId;
import java.util.List;

/**
 * JWT 认证过滤器:解析 Authorization: Bearer &lt;token&gt;,校验签名/过期/黑名单后写入 SecurityContext。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistMapper tokenBlacklistMapper;

    public JwtAuthFilter(JwtUtil jwtUtil, TokenBlacklistMapper tokenBlacklistMapper) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistMapper = tokenBlacklistMapper;
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
}
