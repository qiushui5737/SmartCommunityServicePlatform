package com.community.util;

import com.community.config.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * JWT Token 黑名单服务
 *
 * 解决 JWT 无状态认证的核心痛点：无法主动失效 Token
 *
 * 实现：用户登出时将 Token 写入 Redis 黑名单，TTL = Token 剩余有效期
 *       JwtAuthFilter 每次认证时校验 Token 是否在黑名单中
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /** Token 黑名单 Redis key 前缀 */
    private static final String BLACKLIST_KEY = "community:jwt:blacklist:";

    /**
     * 将 Token 加入黑名单（用户登出时调用）
     *
     * @param token JWT Token
     */
    public void addToBlacklist(String token) {
        try {
            Claims claims = jwtUtil.parseToken(token);
            long expirationMs = claims.getExpiration().getTime();
            long now = System.currentTimeMillis();
            // 剩余有效时间（毫秒），至少保留 1 秒
            long remainingMs = Math.max(expirationMs - now, 1000L);

            redisTemplate.opsForValue().set(
                    BLACKLIST_KEY + token, "1", remainingMs, TimeUnit.MILLISECONDS);
            log.info("Token 已加入黑名单, 剩余有效 {}ms", remainingMs);
        } catch (Exception e) {
            // Token 无效或已过期，无需加入黑名单
            log.debug("Token 解析失败，未加入黑名单: {}", e.getMessage());
        }
    }

    /**
     * 校验 Token 是否在黑名单中
     *
     * @param token JWT Token
     * @return true=已被加入黑名单（已登出），false=有效
     */
    public boolean isBlacklisted(String token) {
        try {
            Boolean exists = redisTemplate.hasKey(BLACKLIST_KEY + token);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Token 黑名单校验异常", e);
            // Redis 异常时放行（降级策略，保证可用性）
            return false;
        }
    }
}
