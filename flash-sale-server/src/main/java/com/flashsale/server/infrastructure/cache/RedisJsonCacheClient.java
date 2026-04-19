package com.flashsale.server.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.server.infrastructure.redis.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisJsonCacheClient {

    private static final class LocalValue {
        private final String json;
        private final long expireAtMillis;

        private LocalValue(String json, long expireAtMillis) {
            this.json = json;
            this.expireAtMillis = expireAtMillis;
        }
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisKeyManager redisKeyManager;

    private final Map<String, LocalValue> localCache = new ConcurrentHashMap<>();

    public <T> T get(String key, Class<T> clazz) {
        String json = getRaw(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("failed to deserialize cache key={}, err={}", key, e.getMessage());
            return null;
        }
    }

    public <T> List<T> getList(String key, Class<T> clazz) {
        String json = getRaw(key);
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.warn("failed to deserialize list cache key={}, err={}", key, e.getMessage());
            return List.of();
        }
    }

    public void set(String key, Object value, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
            putLocal(key, json);
        } catch (Exception e) {
            log.warn("failed to set cache key={}, err={}", key, e.getMessage());
        }
    }

    public void setRaw(String key, String value, long ttlSeconds) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            putLocal(key, value);
        } catch (Exception e) {
            log.warn("failed to set raw cache key={}, err={}", key, e.getMessage());
        }
    }

    public String getRaw(String key) {
        try {
            String redisValue = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(redisValue)) {
                putLocal(key, redisValue);
                return redisValue;
            }
        } catch (Exception e) {
            log.warn("redis read failed, fallback local cache key={}, err={}", key, e.getMessage());
        }

        LocalValue localValue = localCache.get(key);
        if (localValue == null) {
            return null;
        }
        if (System.currentTimeMillis() > localValue.expireAtMillis) {
            localCache.remove(key);
            return null;
        }
        return localValue.json;
    }

    private void putLocal(String key, String value) {
        if (localCache.size() >= redisKeyManager.localCacheMaxSize()) {
            String firstKey = localCache.keySet().stream().findFirst().orElse(null);
            if (firstKey != null) {
                localCache.remove(firstKey);
            }
        }
        long expireAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(redisKeyManager.localCacheTtlSeconds());
        localCache.put(key, new LocalValue(value, expireAt));
    }
}
