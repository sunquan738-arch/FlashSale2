package com.flashsale.server.seckill;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SeckillRollbackScript {

    private static final DefaultRedisScript<Long> ROLLBACK_LUA_SCRIPT;

    static {
        ROLLBACK_LUA_SCRIPT = new DefaultRedisScript<>();
        ROLLBACK_LUA_SCRIPT.setLocation(new ClassPathResource("lua/seckill_rollback.lua"));
        ROLLBACK_LUA_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;

    public void rollback(String stockKey, String orderUserSetKey, Long userId) {
        stringRedisTemplate.execute(
                ROLLBACK_LUA_SCRIPT,
                List.of(stockKey, orderUserSetKey),
                String.valueOf(userId)
        );
    }
}
