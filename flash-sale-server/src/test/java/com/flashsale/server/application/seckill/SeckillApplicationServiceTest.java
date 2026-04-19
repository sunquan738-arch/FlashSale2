package com.flashsale.server.application.seckill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.server.common.exception.BusinessException;
import com.flashsale.server.config.properties.FeatureSwitchProperties;
import com.flashsale.server.domain.seckill.SeckillResultCacheValue;
import com.flashsale.server.domain.seckill.SeckillStatus;
import com.flashsale.server.entity.SeckillActivity;
import com.flashsale.server.infrastructure.mq.outbox.OutboxDispatcher;
import com.flashsale.server.infrastructure.mq.outbox.OutboxEventService;
import com.flashsale.server.infrastructure.redis.RedisKeyManager;
import com.flashsale.server.infrastructure.seckill.SeckillCacheService;
import com.flashsale.server.infrastructure.seckill.SeckillResultStore;
import com.flashsale.server.mapper.OrderMapper;
import com.flashsale.server.mapper.SeckillOrderMapper;
import com.flashsale.server.seckill.SeckillRollbackScript;
import com.flashsale.server.seckill.SeckillScript;
import com.flashsale.server.utils.UserContext;
import com.flashsale.server.vo.SeckillResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;

class SeckillApplicationServiceTest {

    private final SeckillOrderMapper seckillOrderMapper = Mockito.mock(SeckillOrderMapper.class);
    private final OrderMapper orderMapper = Mockito.mock(OrderMapper.class);
    private final SeckillCacheService seckillCacheService = Mockito.mock(SeckillCacheService.class);
    private final SeckillResultStore seckillResultStore = Mockito.mock(SeckillResultStore.class);
    private final RedisKeyManager redisKeyManager = Mockito.mock(RedisKeyManager.class);
    private final SeckillScript seckillScript = Mockito.mock(SeckillScript.class);
    private final SeckillRollbackScript seckillRollbackScript = Mockito.mock(SeckillRollbackScript.class);
    private final OutboxEventService outboxEventService = Mockito.mock(OutboxEventService.class);
    private final OutboxDispatcher outboxDispatcher = Mockito.mock(OutboxDispatcher.class);
    private final FeatureSwitchProperties featureSwitchProperties = Mockito.mock(FeatureSwitchProperties.class);

    private final SeckillApplicationService service = new SeckillApplicationService(
            seckillOrderMapper,
            orderMapper,
            seckillCacheService,
            seckillResultStore,
            redisKeyManager,
            seckillScript,
            seckillRollbackScript,
            outboxEventService,
            outboxDispatcher,
            new ObjectMapper(),
            featureSwitchProperties
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void doSeckillShouldRejectDuplicateRequest() {
        UserContext.setUserId(1L);
        Mockito.when(featureSwitchProperties.isSeckillEnabled()).thenReturn(true);

        SeckillActivity activity = new SeckillActivity();
        activity.setId(2L);
        activity.setProductId(11L);
        activity.setStatus(1);
        activity.setStartTime(LocalDateTime.now().minusMinutes(10));
        activity.setEndTime(LocalDateTime.now().plusMinutes(10));

        Mockito.when(seckillCacheService.getActivityById(2L)).thenReturn(activity);
        Mockito.when(redisKeyManager.scriptStockKey(2L)).thenReturn("stockKey");
        Mockito.when(redisKeyManager.scriptOrderedUsersKey(2L)).thenReturn("orderedKey");
        Mockito.when(seckillScript.execute("stockKey", "orderedKey", 1L)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.doSeckill(2L));
        Mockito.verify(outboxEventService, never()).createSeckillEvent(Mockito.anyString(), Mockito.any());
    }

    @Test
    void queryResultShouldMapNewStateToCompatibleText() {
        UserContext.setUserId(1L);
        Mockito.when(featureSwitchProperties.isSeckillEnabled()).thenReturn(true);

        SeckillResultCacheValue value = new SeckillResultCacheValue();
        value.setStatus(SeckillStatus.SUCCESS);
        value.setOrderNo("SO123");

        Mockito.when(seckillResultStore.getResult(2L, 1L)).thenReturn(value);

        SeckillResultVO vo = service.querySeckillResult(2L);
        assertEquals("抢购成功", vo.getStatus());
        assertEquals("SO123", vo.getOrderNo());
    }
}
