package com.flashsale.server.infrastructure.redis;

import com.flashsale.server.config.properties.RedisKeyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisKeyManager {

    private final RedisKeyProperties properties;

    public boolean useNewKey() {
        return properties.isUseNewKey();
    }

    public boolean readLegacyFallback() {
        return properties.isReadLegacyFallback();
    }

    public boolean doubleWriteLegacy() {
        return properties.isDoubleWriteLegacy();
    }

    public long resultTtlSeconds() {
        return properties.getCache().getResultTtlSeconds();
    }

    public long orderedSetTtlSeconds() {
        return properties.getCache().getOrderedSetTtlSeconds();
    }

    public long activityTtlSeconds() {
        return properties.getCache().getActivityTtlSeconds();
    }

    public long productTtlSeconds() {
        return properties.getCache().getProductTtlSeconds();
    }

    public long listTtlSeconds() {
        return properties.getCache().getListTtlSeconds();
    }

    public long stockBufferTtlSeconds() {
        return properties.getCache().getStockTtlBufferSeconds();
    }

    public long localCacheTtlSeconds() {
        return properties.getCache().getLocalCacheTtlSeconds();
    }

    public long localCacheMaxSize() {
        return properties.getCache().getLocalCacheMaxSize();
    }

    public String newStockKey(Long activityId) {
        return prefix() + ":seckill:{act:" + activityId + "}:stock";
    }

    public String legacyStockKey(Long activityId) {
        return "seckill:stock:" + activityId;
    }

    public String newOrderedUsersKey(Long activityId) {
        return prefix() + ":seckill:{act:" + activityId + "}:ordered_users";
    }

    public String legacyOrderedUsersKey(Long activityId) {
        return "seckill:ordered:" + activityId;
    }

    public String newResultKey(Long activityId, Long userId) {
        return prefix() + ":seckill:{act:" + activityId + "}:result:u:" + userId;
    }

    public String legacyResultKey(Long activityId, Long userId) {
        return "seckill:result:" + activityId + ":" + userId;
    }

    public String activityCacheKey(Long activityId) {
        return prefix() + ":cache:{act:" + activityId + "}:activity";
    }

    public String activityListCacheKey() {
        return prefix() + ":cache:activity:list";
    }

    public String productCacheKey(Long productId) {
        return prefix() + ":cache:{prod:" + productId + "}:product";
    }

    public String productListCacheKey() {
        return prefix() + ":cache:product:list";
    }

    public String rateLimitKey(String endpoint, String dimension, String value) {
        String cleanEndpoint = endpoint.replace('/', ':');
        return prefix() + ":limit:{ep:" + cleanEndpoint + "}:" + dimension + ":" + value;
    }

    public String rateLimitActivityKey(String endpoint, Long activityId) {
        String cleanEndpoint = endpoint.replace('/', ':');
        return prefix() + ":limit:{act:" + activityId + "}:ep:" + cleanEndpoint;
    }

    public List<String> writeResultKeys(Long activityId, Long userId) {
        List<String> keys = new ArrayList<>();
        if (useNewKey()) {
            keys.add(newResultKey(activityId, userId));
            if (doubleWriteLegacy()) {
                keys.add(legacyResultKey(activityId, userId));
            }
            return keys;
        }
        keys.add(legacyResultKey(activityId, userId));
        return keys;
    }

    public List<String> readResultKeys(Long activityId, Long userId) {
        List<String> keys = new ArrayList<>();
        if (useNewKey()) {
            keys.add(newResultKey(activityId, userId));
            if (readLegacyFallback()) {
                keys.add(legacyResultKey(activityId, userId));
            }
            return keys;
        }
        keys.add(legacyResultKey(activityId, userId));
        return keys;
    }

    public List<String> writeStockKeys(Long activityId) {
        List<String> keys = new ArrayList<>();
        if (useNewKey()) {
            keys.add(newStockKey(activityId));
            if (doubleWriteLegacy()) {
                keys.add(legacyStockKey(activityId));
            }
            return keys;
        }
        keys.add(legacyStockKey(activityId));
        return keys;
    }

    public List<String> writeOrderedUserKeys(Long activityId) {
        List<String> keys = new ArrayList<>();
        if (useNewKey()) {
            keys.add(newOrderedUsersKey(activityId));
            if (doubleWriteLegacy()) {
                keys.add(legacyOrderedUsersKey(activityId));
            }
            return keys;
        }
        keys.add(legacyOrderedUsersKey(activityId));
        return keys;
    }

    public String scriptStockKey(Long activityId) {
        return useNewKey() ? newStockKey(activityId) : legacyStockKey(activityId);
    }

    public String scriptOrderedUsersKey(Long activityId) {
        return useNewKey() ? newOrderedUsersKey(activityId) : legacyOrderedUsersKey(activityId);
    }

    private String prefix() {
        return "flashsale:" + properties.getEnvPrefix();
    }
}
