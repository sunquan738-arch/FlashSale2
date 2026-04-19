package com.flashsale.server.config;

import com.flashsale.server.common.exception.BizException;
import com.flashsale.server.config.properties.RateLimitProperties;
import com.flashsale.server.infrastructure.redis.RedisKeyManager;
import com.flashsale.server.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements org.springframework.web.servlet.HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final RateLimitProperties rateLimitProperties;
    private final RedisKeyManager redisKeyManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        String path = request.getRequestURI();
        Long userId = UserContext.getUserId();
        String ip = resolveIp(request);

        RateLimitProperties.Rule rule = resolveRule(path);

        checkLimit(redisKeyManager.rateLimitKey(path, "endpoint", "global"), rule.getEndpointCount(), rule.getWindowSeconds(), "too many requests");
        checkLimit(redisKeyManager.rateLimitKey(path, "ip", ip), rule.getIpCount(), rule.getWindowSeconds(), "too many requests from ip");

        if (userId != null) {
            checkLimit(redisKeyManager.rateLimitKey(path, "user", String.valueOf(userId)), rule.getUserCount(), rule.getWindowSeconds(), "too many requests from user");
        }

        Long activityId = parseActivityId(path);
        if (activityId != null) {
            checkLimit(redisKeyManager.rateLimitActivityKey(path, activityId), rule.getActivityCount(), rule.getWindowSeconds(), "too many seckill requests for this activity");
        }

        return true;
    }

    private RateLimitProperties.Rule resolveRule(String path) {
        if (path.startsWith("/api/seckill/do/")) {
            return rateLimitProperties.getSeckillRule();
        }
        if (path.startsWith("/api/products") || path.startsWith("/api/seckill/activities")) {
            return rateLimitProperties.getQueryRule();
        }
        return rateLimitProperties.getDefaultRule();
    }

    private void checkLimit(String key, int limitCount, int windowSeconds, String message) {
        if (limitCount <= 0) {
            return;
        }
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        if (count != null && count > limitCount) {
            throw new BizException(429, message);
        }
    }

    private Long parseActivityId(String path) {
        if (!path.startsWith("/api/seckill/do/")) {
            return null;
        }
        String[] arr = path.split("/");
        if (arr.length < 5) {
            return null;
        }
        try {
            return Long.parseLong(arr[4]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int idx = forwarded.indexOf(',');
            return idx > 0 ? forwarded.substring(0, idx).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
