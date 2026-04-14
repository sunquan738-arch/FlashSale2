package com.flashsale.server.seckill;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SeckillScript {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> SECKILL_LUA_SCRIPT;

    static {
        SECKILL_LUA_SCRIPT = new DefaultRedisScript<>();
        SECKILL_LUA_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_LUA_SCRIPT.setResultType(Long.class);
    }

    /**
     * 执行秒杀 Lua 脚本（原子操作）：
     * 0=成功，1=已抢购，2=库存不足。
     */
    public long execute(Long activityId, Long userId) {
        String stockKey = "seckill:stock:" + activityId;
        String orderUserSetKey = "seckill:ordered:" + activityId;
        Long result = stringRedisTemplate.execute(
                SECKILL_LUA_SCRIPT,
                List.of(stockKey, orderUserSetKey),
                String.valueOf(userId)
        );
        return result == null ? 2L : result;
    }
}
