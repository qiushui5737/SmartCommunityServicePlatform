package com.community.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁工具类
 *
 * 实现：基于 SET key value NX PX expire（原子加锁） + Lua 脚本 CAS 释放
 * 特性：
 *   1. 原子性加锁（SETNX + 过期时间一条命令）
 *   2. 防误删（value 存唯一标识，释放时 Lua CAS 校验）
 *   3. 防死锁（过期时间兜底，持有者崩溃后锁自动释放）
 *   4. 可重试（tryLock 支持等待超时）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private final StringRedisTemplate redisTemplate;

    /** 释放锁的 Lua 脚本：校验 value 一致后再删除（CAS） */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private static final DefaultRedisScript<Long> UNLOCK_REDIS_SCRIPT =
            new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);

    /** 锁默认过期时间（毫秒） */
    private static final long DEFAULT_EXPIRE_MS = 30000;

    /** 默认等待超时（毫秒） */
    private static final long DEFAULT_WAIT_MS = 3000;

    /** 重试间隔（毫秒） */
    private static final long RETRY_INTERVAL_MS = 50;

    /**
     * 尝试获取分布式锁（等待超时 + 自动过期）
     *
     * @param lockKey   锁 key
     * @param waitMs    等待获取锁的最长时间（毫秒）
     * @param expireMs  锁的过期时间（毫秒），防死锁
     * @return 锁标识（value），用于后续释放；null 表示获取失败
     */
    public String tryLock(String lockKey, long waitMs, long expireMs) {
        String lockValue = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + waitMs;

        while (System.currentTimeMillis() < deadline) {
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expireMs, TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(locked)) {
                log.debug("获取锁成功 key={} value={}", lockKey, lockValue);
                return lockValue;
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        log.warn("获取锁超时 key={} waitMs={}", lockKey, waitMs);
        return null;
    }

    /**
     * 尝试获取锁（默认参数）
     */
    public String tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_WAIT_MS, DEFAULT_EXPIRE_MS);
    }

    /**
     * 尝试获取锁，获取失败抛出业务异常
     *
     * @param lockKey 锁 key
     * @return 锁标识
     * @throws IllegalStateException 获取锁失败时抛出
     */
    public String tryLockOrThrow(String lockKey, long waitMs, long expireMs, String failMsg) {
        String lockValue = tryLock(lockKey, waitMs, expireMs);
        if (lockValue == null) {
            throw new IllegalStateException(failMsg != null ? failMsg : "操作过于频繁，请稍后重试");
        }
        return lockValue;
    }

    /**
     * 释放分布式锁（Lua CAS 校验，防止误删他人持有的锁）
     *
     * @param lockKey   锁 key
     * @param lockValue 加锁时返回的唯一标识
     * @return true=释放成功
     */
    public boolean unlock(String lockKey, String lockValue) {
        try {
            Long result = redisTemplate.execute(
                    UNLOCK_REDIS_SCRIPT,
                    Collections.singletonList(lockKey),
                    lockValue);
            boolean success = result != null && result > 0;
            if (success) {
                log.debug("释放锁成功 key={}", lockKey);
            } else {
                log.warn("释放锁失败（锁已过期或被他人持有） key={}", lockKey);
            }
            return success;
        } catch (Exception e) {
            log.error("释放锁异常 key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 执行带分布式锁的操作（自动释放）
     *
     * @param lockKey    锁 key
     * @param waitMs     等待超时
     * @param expireMs   锁过期时间
     * @param failMsg    获取锁失败提示
     * @param action     获取锁后执行的操作
     * @return 操作的返回值
     */
    public <T> T executeWithLock(String lockKey, long waitMs, long expireMs,
                                  String failMsg, java.util.function.Supplier<T> action) {
        String lockValue = tryLockOrThrow(lockKey, waitMs, expireMs, failMsg);
        try {
            return action.get();
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 执行带分布式锁的无返回值操作（自动释放）
     */
    public void executeWithLock(String lockKey, long waitMs, long expireMs,
                                String failMsg, Runnable action) {
        String lockValue = tryLockOrThrow(lockKey, waitMs, expireMs, failMsg);
        try {
            action.run();
        } finally {
            unlock(lockKey, lockValue);
        }
    }
}
