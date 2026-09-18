package com.community.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 缓存工具类
 *
 * 防护策略：
 * 1. 缓存穿透：查询结果为空时缓存空值（短TTL），防止恶意请求反复打DB
 * 2. 缓存雪崩：TTL 加随机抖动（baseTTL ± jitter），避免大批 key 同时过期
 * 3. 缓存击穿：热点 key 过期时，使用 SETNX 互斥锁重建缓存，只放行一个线程查DB
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 空值标记前缀，区分"缓存了空结果"和"缓存不存在" */
    private static final String NULL_TAG = "__NULL__";

    /** 击穿重建锁的等待超时 */
    private static final long REBUILD_WAIT_SECONDS = 3;

    // ==================== 基础缓存操作 ====================

    /**
     * 写入缓存（带雪崩防护：TTL 随机抖动）
     */
    public void set(String key, Object value, long baseTTL, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            // 随机抖动：baseTTL 的 80% ~ 120%
            long jitter = (long) (baseTTL * (0.8 + ThreadLocalRandom.current().nextDouble() * 0.4));
            redisTemplate.opsForValue().set(key, json, jitter, unit);
        } catch (Exception e) {
            log.error("写入缓存失败 key={}", key, e);
        }
    }

    /**
     * 读取缓存（自动反序列化为指定类型）
     */
    public <T> T get(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            if (NULL_TAG.equals(json)) {
                log.debug("命中空值缓存 key={}", key);
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("读取缓存失败 key={}", key, e);
            return null;
        }
    }

    /**
     * 读取缓存（反序列化为 List 类型）
     */
    public <T> List<T> getList(String key, Class<T> elementType) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            if (NULL_TAG.equals(json)) return null;
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            log.error("读取缓存List失败 key={}", key, e);
            return null;
        }
    }

    /**
     * 删除缓存
     */
    public void evict(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除（按前缀）
     */
    public void evictByPrefix(String prefix) {
        var keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("批量清除缓存 prefix={} count={}", prefix, keys.size());
        }
    }

    // ==================== 高级缓存策略 ====================

    /**
     * 查询缓存（三重防护：穿透 + 雪崩 + 击穿）
     *
     * @param key       缓存 key
     * @param type      返回值类型
     * @param baseTTL   基础过期时间（会自动加随机抖动防雪崩）
     * @param unit      时间单位
     * @param dbLoader  DB 回源函数
     * @return 查询结果
     */
    public <T> T queryWithProtection(String key, Class<T> type,
                                     long baseTTL, TimeUnit unit,
                                     Supplier<T> dbLoader) {
        // 1. 先查缓存
        T cached = get(key, type);
        if (cached != null) return cached;

        // 检查是否缓存了空值
        String raw = redisTemplate.opsForValue().get(key);
        if (NULL_TAG.equals(raw)) {
            // 命中空值缓存，直接返回null（防穿透）
            return null;
        }

        // 2. 缓存未命中 → 互斥锁重建（防击穿）
        String lockKey = key + ":lock";
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", REBUILD_WAIT_SECONDS, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                // 双重检查：拿到锁后再查一次缓存（可能其他线程已重建）
                cached = get(key, type);
                if (cached != null) return cached;

                raw = redisTemplate.opsForValue().get(key);
                if (NULL_TAG.equals(raw)) return null;

                // 3. 回源 DB
                T data = dbLoader.get();

                if (data == null) {
                    // 缓存空值，短TTL（防穿透）
                    redisTemplate.opsForValue().set(key, NULL_TAG,
                            Math.max(baseTTL / 10, 60), TimeUnit.SECONDS);
                    log.info("缓存空值 key={} 防穿透", key);
                    return null;
                }

                // 写入缓存（带雪崩防护）
                set(key, data, baseTTL, unit);
                log.info("缓存重建成功 key={}", key);
                return data;

            } catch (Exception e) {
                log.error("缓存重建异常 key={}", key, e);
                // 降级：直接回源
                return dbLoader.get();
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // 未拿到锁 → 短暂等待后重试读取
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            cached = get(key, type);
            if (cached != null) return cached;
            // 等待后仍未拿到，直接回源（兜底）
            return dbLoader.get();
        }
    }

    /**
     * List 版本的查询缓存（三重防护）
     */
    public <T> List<T> queryListWithProtection(String key, Class<T> elementType,
                                               long baseTTL, TimeUnit unit,
                                               Supplier<List<T>> dbLoader) {
        // 1. 先查缓存
        List<T> cached = getList(key, elementType);
        if (cached != null) return cached;

        String raw = redisTemplate.opsForValue().get(key);
        if (NULL_TAG.equals(raw)) return null;

        // 2. 互斥锁重建
        String lockKey = key + ":lock";
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", REBUILD_WAIT_SECONDS, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                cached = getList(key, elementType);
                if (cached != null) return cached;

                raw = redisTemplate.opsForValue().get(key);
                if (NULL_TAG.equals(raw)) return null;

                List<T> data = dbLoader.get();

                if (data == null || data.isEmpty()) {
                    redisTemplate.opsForValue().set(key, NULL_TAG,
                            Math.max(baseTTL / 10, 60), TimeUnit.SECONDS);
                    return data;
                }

                set(key, data, baseTTL, unit);
                log.info("缓存List重建成功 key={}", key);
                return data;
            } catch (Exception e) {
                log.error("缓存List重建异常 key={}", key, e);
                return dbLoader.get();
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try { Thread.sleep(100); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            cached = getList(key, elementType);
            return cached != null ? cached : dbLoader.get();
        }
    }
}
