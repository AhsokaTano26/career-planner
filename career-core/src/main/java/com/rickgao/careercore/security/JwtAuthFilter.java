package com.rickgao.careercore.security;

import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import com.rickgao.careercore.modules.auth.mapper.TokenBlacklistMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final TokenBlacklistMapper tokenBlacklistMapper;
    private final SysUserMapper sysUserMapper;
    private final AuthUserCache authUserCache;

    public JwtAuthFilter(JwtUtil jwtUtil, TokenBlacklistMapper tokenBlacklistMapper,
                         SysUserMapper sysUserMapper,
                         AuthUserCache authUserCache) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistMapper = tokenBlacklistMapper;
        this.sysUserMapper = sysUserMapper;
        this.authUserCache = authUserCache;
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
                    // 复审 Batch4：用户行走 60s 短缓存；版本 mismatch 回源重读自愈
                    SysUser user = authUserCache.get(claims.getSubject());
                    if (user != null && !tokenVersionMatches(claims, user)) {
                        authUserCache.evict(claims.getSubject());
                        user = null;
                    }
                    if (user == null) {
                        user = sysUserMapper.findById(claims.getSubject());
                        authUserCache.put(claims.getSubject(), user);
                    }
                    if (user == null) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    // 稳定性：令牌版本不一致（改密/重置/停用后）→ 视为失效，要求重新登录
                    if (!tokenVersionMatches(claims, user)) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    LoginUser loginUser = new LoginUser(
                            claims.getSubject(),
                            claims.get("username", String.class),
                            claims.get("role", String.class),
                            jti,
                            claims.getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                    String authorityRole = loginUser.getRole();
                    // 防御：role 缺失时不组装 ROLE_null，直接匿名（避免怪异鉴权）
                    if (authorityRole == null || authorityRole.isBlank()) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            loginUser, null, List.of(new SimpleGrantedAuthority("ROLE_" + authorityRole)));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (org.springframework.dao.DataAccessException dataExc) {
                // 稳定性：DB 抖动（黑名单/用户查询）与令牌无效区分记日志；仍匿名放行由 401 统一处理
                log.warn("JWT 过滤器查库失败：{}", dataExc.getMessage());
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                // 令牌无效/过期/被列入黑名单:保持匿名,由 SecurityConfig 统一返回 401
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean tokenVersionMatches(Claims claims, SysUser user) {
        Object claimVersion = claims.get("tokenVersion");
        int tokenVersion = claimVersion instanceof Number number ? number.intValue() : 0;
        int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        return tokenVersion == currentVersion;
    }
}
