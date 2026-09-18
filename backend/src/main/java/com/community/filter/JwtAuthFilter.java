package com.community.filter;

import com.community.config.JwtUtil;
import com.community.util.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * JWT 认证过滤器
 *
 * 认证链路：
 * 1. 白名单放行（登录/注册/静态资源）
 * 2. 解析 Bearer Token → 校验签名 + 过期时间
 * 3. Redis 黑名单校验（用户已登出则拒绝）
 * 4. 注入 userId + role 到 RequestAttribute
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // 白名单：登录/注册/登出/静态资源放行
        if (requestURI.contains("/auth/login") || requestURI.contains("/auth/register")
                || requestURI.startsWith("/uploads") || requestURI.equals("/health")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 1. 校验 Token 签名 + 过期时间
                var claims = jwtUtil.parseToken(token);

                // 2. 校验 Token 是否在 Redis 黑名单中（已登出）
                if (tokenBlacklistService.isBlacklisted(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"msg\":\"Token已失效，请重新登录\"}");
                    return;
                }

                // 3. 注入用户信息到请求上下文
                request.setAttribute("userId", Long.parseLong(claims.getSubject()));
                request.setAttribute("role", claims.get("role", String.class));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"Token无效或已过期\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
