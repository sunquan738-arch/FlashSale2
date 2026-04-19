package com.flashsale.server.infrastructure.mq.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.flashsale.server.config.properties.OutboxProperties;
import com.flashsale.server.entity.outbox.OutboxEvent;
import com.flashsale.server.mapper.OutboxEventMapper;
import com.flashsale.server.seckill.SeckillMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    public static final String EVENT_TYPE_SECKILL_ORDER_CREATE = "SECKILL_ORDER_CREATE";

    private final OutboxEventMapper outboxEventMapper;
    private final OutboxProperties outboxProperties;

    @Transactional(rollbackFor = Exception.class)
    public OutboxEvent createSeckillEvent(String payload, SeckillMessage message) {
        OutboxEvent event = new OutboxEvent();
        event.setEventId(UUID.randomUUID().toString().replace("-", ""));
        event.setEventType(EVENT_TYPE_SECKILL_ORDER_CREATE);
        event.setBizKey(buildBizKey(message));
        event.setPayload(payload);
        event.setStatus(OutboxEventStatus.NEW.getCode());
        event.setRetryCount(0);
        event.setMaxRetry(outboxProperties.getMaxRetries());
        event.setNextRetryTime(LocalDateTime.now());
        event.setLastError(null);
        outboxEventMapper.insert(event);
        return event;
    }

    public List<OutboxEvent> listDispatchableEvents(int limit) {
        LambdaQueryWrapper<OutboxEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboxEvent::getStatus, OutboxEventStatus.NEW.getCode())
                .le(OutboxEvent::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(OutboxEvent::getId)
                .last("limit " + limit);
        return outboxEventMapper.selectList(wrapper);
    }

    public void markSent(Long id) {
        LambdaUpdateWrapper<OutboxEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OutboxEvent::getId, id)
                .set(OutboxEvent::getStatus, OutboxEventStatus.SENT.getCode())
                .set(OutboxEvent::getLastError, null)
                .set(OutboxEvent::getUpdateTime, LocalDateTime.now());
        outboxEventMapper.update(null, wrapper);
    }

    public void markRetry(Long id, int retryCount, String error) {
        LambdaUpdateWrapper<OutboxEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OutboxEvent::getId, id)
                .set(OutboxEvent::getRetryCount, retryCount)
                .set(OutboxEvent::getNextRetryTime, LocalDateTime.now().plusSeconds(backoffSeconds(retryCount)))
                .set(OutboxEvent::getLastError, trim(error))
                .set(OutboxEvent::getUpdateTime, LocalDateTime.now());
        outboxEventMapper.update(null, wrapper);
    }

    public void markDead(Long id, String error) {
        LambdaUpdateWrapper<OutboxEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OutboxEvent::getId, id)
                .set(OutboxEvent::getStatus, OutboxEventStatus.DEAD.getCode())
                .set(OutboxEvent::getLastError, trim(error))
                .set(OutboxEvent::getUpdateTime, LocalDateTime.now());
        outboxEventMapper.update(null, wrapper);
    }

    private long backoffSeconds(int retryCount) {
        long delayMs = (long) (outboxProperties.getRetryBaseDelayMs() * Math.pow(2, Math.max(0, retryCount - 1)));
        return Math.max(1, delayMs / 1000);
    }

    private String trim(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > 1024 ? error.substring(0, 1024) : error;
    }

    private String buildBizKey(SeckillMessage message) {
        return "seckill:" + message.getActivityId() + ":" + message.getUserId();
    }
}
