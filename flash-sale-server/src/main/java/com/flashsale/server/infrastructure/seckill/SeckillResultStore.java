package com.flashsale.server.infrastructure.seckill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.server.domain.seckill.SeckillResultCacheValue;
import com.flashsale.server.domain.seckill.SeckillStatus;
import com.flashsale.server.infrastructure.redis.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillResultStore {

    private static final String LEGACY_QUEUING = "0";
    private static final String LEGACY_FAILED = "-1";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisKeyManager redisKeyManager;
    private final ObjectMapper objectMapper;

    public void markQueuing(Long activityId, Long userId) {
        SeckillResultCacheValue value = new SeckillResultCacheValue();
        value.setStatus(SeckillStatus.QUEUING);
        value.setUpdateTime(LocalDateTime.now());
        writeNewAndLegacy(activityId, userId, value, LEGACY_QUEUING);
    }

    public void markSuccess(Long activityId, Long userId, String orderNo) {
        SeckillResultCacheValue value = new SeckillResultCacheValue();
        value.setStatus(SeckillStatus.SUCCESS);
        value.setOrderNo(orderNo);
        value.setUpdateTime(LocalDateTime.now());
        writeNewAndLegacy(activityId, userId, value, orderNo);
    }

    public void markFailed(Long activityId, Long userId, SeckillStatus status, String reason) {
        SeckillResultCacheValue value = new SeckillResultCacheValue();
        value.setStatus(status);
        value.setReason(reason);
        value.setUpdateTime(LocalDateTime.now());
        writeNewAndLegacy(activityId, userId, value, LEGACY_FAILED);
    }

    public SeckillResultCacheValue getResult(Long activityId, Long userId) {
        List<String> readKeys = redisKeyManager.readResultKeys(activityId, userId);
        for (String key : readKeys) {
            String raw = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            SeckillResultCacheValue parsed = parse(raw);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private SeckillResultCacheValue parse(String raw) {
        if (LEGACY_QUEUING.equals(raw)) {
            SeckillResultCacheValue value = new SeckillResultCacheValue();
            value.setStatus(SeckillStatus.QUEUING);
            return value;
        }
        if (LEGACY_FAILED.equals(raw)) {
            SeckillResultCacheValue value = new SeckillResultCacheValue();
            value.setStatus(SeckillStatus.FAILED);
            return value;
        }

        if (!raw.startsWith("{")) {
            SeckillResultCacheValue value = new SeckillResultCacheValue();
            value.setStatus(SeckillStatus.SUCCESS);
            value.setOrderNo(raw);
            return value;
        }

        try {
            return objectMapper.readValue(raw, SeckillResultCacheValue.class);
        } catch (JsonProcessingException e) {
            log.warn("failed to parse seckill result payload, err={}", e.getMessage());
            return null;
        }
    }

    private void writeNewAndLegacy(Long activityId, Long userId, SeckillResultCacheValue value, String legacyValue) {
        long ttlSeconds = redisKeyManager.resultTtlSeconds();

        if (redisKeyManager.useNewKey()) {
            try {
                String json = objectMapper.writeValueAsString(value);
                stringRedisTemplate.opsForValue().set(redisKeyManager.newResultKey(activityId, userId), json, ttlSeconds, TimeUnit.SECONDS);
            } catch (JsonProcessingException e) {
                log.warn("failed to serialize seckill result cache, err={}", e.getMessage());
            }

            if (redisKeyManager.doubleWriteLegacy()) {
                stringRedisTemplate.opsForValue().set(redisKeyManager.legacyResultKey(activityId, userId), legacyValue, ttlSeconds, TimeUnit.SECONDS);
            }
            return;
        }

        stringRedisTemplate.opsForValue().set(redisKeyManager.legacyResultKey(activityId, userId), legacyValue, ttlSeconds, TimeUnit.SECONDS);
    }
}
