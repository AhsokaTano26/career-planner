package com.career.core.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求路径归一化过滤器（Demo 兼容点 / 后续迭代替换位置）。
 * <p>
 * Apifox 契约测试在路径变量为空时会请求带连续斜杠的 URL（如 /api/v1/profile-snapshots//feedback），
 * Spring PathPattern 不匹配空路径段 → NoResourceFoundException（404「接口不存在」）。
 * 此过滤器将连续斜杠折叠为单斜杠，使其命中 Controller 的兜底路由；对 queryString 无影响。
 */
@Component
public class PathNormalizeFilter extends OncePerRequestFilter {

    private static final String DOUBLE_SLASH = "//";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && uri.contains(DOUBLE_SLASH)) {
            chain.doFilter(new SlashNormalizedRequest(request), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /** 折叠连续斜杠的请求包装器（getRequestURI/getServletPath/getPathInfo 三处归一，兼容不同匹配方式） */
    private static final class SlashNormalizedRequest extends HttpServletRequestWrapper {
        private final String normalizedUri;

        SlashNormalizedRequest(HttpServletRequest request) {
            super(request);
            this.normalizedUri = request.getRequestURI().replaceAll("/{2,}", "/");
        }

        @Override
        public String getRequestURI() {
            return normalizedUri;
        }

        @Override
        public String getServletPath() {
            String sp = super.getServletPath();
            return sp == null ? null : sp.replaceAll("/{2,}", "/");
        }

        @Override
        public String getPathInfo() {
            String pi = super.getPathInfo();
            return pi == null ? null : pi.replaceAll("/{2,}", "/");
        }
    }
}
