package com.flashsale.server.infrastructure.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.server.config.properties.SeckillPreloadProperties;
import com.flashsale.server.entity.Product;
import com.flashsale.server.entity.SeckillActivity;
import com.flashsale.server.infrastructure.cache.RedisJsonCacheClient;
import com.flashsale.server.infrastructure.redis.RedisKeyManager;
import com.flashsale.server.mapper.ProductMapper;
import com.flashsale.server.mapper.SeckillActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillCacheService {

    private final SeckillActivityMapper seckillActivityMapper;
    private final ProductMapper productMapper;
    private final RedisJsonCacheClient redisJsonCacheClient;
    private final RedisKeyManager redisKeyManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillPreloadProperties preloadProperties;

    public SeckillActivity getActivityById(Long activityId) {
        String cacheKey = redisKeyManager.activityCacheKey(activityId);
        SeckillActivity cached = redisJsonCacheClient.get(cacheKey, SeckillActivity.class);
        if (cached != null) {
            return cached;
        }

        SeckillActivity db = seckillActivityMapper.selectById(activityId);
        if (db != null) {
            redisJsonCacheClient.set(cacheKey, db, redisKeyManager.activityTtlSeconds());
            preloadStock(db);
        }
        return db;
    }

    public Product getProductById(Long productId) {
        String cacheKey = redisKeyManager.productCacheKey(productId);
        Product cached = redisJsonCacheClient.get(cacheKey, Product.class);
        if (cached != null) {
            return cached;
        }

        Product db = productMapper.selectById(productId);
        if (db != null) {
            redisJsonCacheClient.set(cacheKey, db, redisKeyManager.productTtlSeconds());
        }
        return db;
    }

    public List<SeckillActivity> listActivities() {
        String key = redisKeyManager.activityListCacheKey();
        List<SeckillActivity> cached = redisJsonCacheClient.getList(key, SeckillActivity.class);
        if (!CollectionUtils.isEmpty(cached)) {
            return cached;
        }
        List<SeckillActivity> db = seckillActivityMapper.selectList(null);
        redisJsonCacheClient.set(key, db, redisKeyManager.listTtlSeconds());
        db.forEach(this::preloadStock);
        return db;
    }

    public List<Product> listProducts() {
        String key = redisKeyManager.productListCacheKey();
        List<Product> cached = redisJsonCacheClient.getList(key, Product.class);
        if (!CollectionUtils.isEmpty(cached)) {
            return cached;
        }
        List<Product> db = productMapper.selectList(null);
        redisJsonCacheClient.set(key, db, redisKeyManager.listTtlSeconds());
        return db;
    }

    @Scheduled(initialDelay = 5000L, fixedDelayString = "${app.seckill.preload.fixed-delay-ms:60000}")
    public void preloadHotData() {
        if (!preloadProperties.isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusMinutes(preloadProperties.getLookBackMinutes());
        LocalDateTime end = now.plusMinutes(preloadProperties.getLookAheadMinutes());

        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillActivity::getStatus, 1)
                .ge(SeckillActivity::getEndTime, start)
                .le(SeckillActivity::getStartTime, end);
        List<SeckillActivity> activities = seckillActivityMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(activities)) {
            return;
        }

        Set<Long> productIds = activities.stream().map(SeckillActivity::getProductId).collect(Collectors.toSet());
        List<Product> products = productMapper.selectBatchIds(productIds);

        activities.forEach(activity -> {
            redisJsonCacheClient.set(redisKeyManager.activityCacheKey(activity.getId()), activity, redisKeyManager.activityTtlSeconds());
            preloadStock(activity);
        });

        products.forEach(product -> redisJsonCacheClient.set(
                redisKeyManager.productCacheKey(product.getId()),
                product,
                redisKeyManager.productTtlSeconds()
        ));

        redisJsonCacheClient.set(redisKeyManager.activityListCacheKey(), activities, redisKeyManager.listTtlSeconds());
        redisJsonCacheClient.set(redisKeyManager.productListCacheKey(), products, redisKeyManager.listTtlSeconds());
    }

    public void preloadStock(SeckillActivity activity) {
        if (activity == null || activity.getSeckillStock() == null) {
            return;
        }

        long ttlSeconds = computeStockTtl(activity.getEndTime());
        for (String stockKey : redisKeyManager.writeStockKeys(activity.getId())) {
            try {
                Boolean absent = stringRedisTemplate.opsForValue().setIfAbsent(
                        stockKey,
                        String.valueOf(activity.getSeckillStock()),
                        ttlSeconds,
                        TimeUnit.SECONDS
                );
                if (Boolean.FALSE.equals(absent)) {
                    stringRedisTemplate.expire(stockKey, ttlSeconds, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                log.warn("preload stock failed, key={}, err={}", stockKey, e.getMessage());
            }
        }

        for (String orderedKey : redisKeyManager.writeOrderedUserKeys(activity.getId())) {
            try {
                stringRedisTemplate.expire(orderedKey, redisKeyManager.orderedSetTtlSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("set ordered key ttl failed, key={}, err={}", orderedKey, e.getMessage());
            }
        }
    }

    private long computeStockTtl(LocalDateTime endTime) {
        if (endTime == null) {
            return redisKeyManager.orderedSetTtlSeconds();
        }
        long seconds = Duration.between(LocalDateTime.now(), endTime).getSeconds() + redisKeyManager.stockBufferTtlSeconds();
        return Math.max(seconds, 3600);
    }
}
