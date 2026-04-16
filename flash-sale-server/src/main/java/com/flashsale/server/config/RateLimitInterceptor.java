package com.flashsale.server.config;

import com.flashsale.server.common.exception.BizException;
import com.flashsale.server.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${app.rate-limit.count:5}")
    private int limitCount;

    @Value("${app.rate-limit.window-seconds:10}")
    private int windowSeconds;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = UserContext.getUserId();
        String userMark = userId == null ? request.getRemoteAddr() : String.valueOf(userId);
        String path = request.getRequestURI();
        String key = "rate:api:" + path + ":" + userMark;

        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        if (count != null && count > limitCount) {
            throw new BizException(429, "too many requests, please retry later");
        }
        return true;
    }
}
