package com.flashsale.server.infrastructure.mq.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.server.config.properties.OutboxProperties;
import com.flashsale.server.domain.seckill.SeckillStatus;
import com.flashsale.server.entity.outbox.OutboxEvent;
import com.flashsale.server.infrastructure.seckill.SeckillResultStore;
import com.flashsale.server.seckill.SeckillMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatcher {

    private final OutboxProperties outboxProperties;
    private final OutboxEventService outboxEventService;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;
    private final SeckillResultStore seckillResultStore;

    private final AtomicBoolean dispatching = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${app.outbox.dispatch-interval-ms:2000}")
    public void dispatch() {
        if (!outboxProperties.isEnabled()) {
            return;
        }
        if (!dispatching.compareAndSet(false, true)) {
            return;
        }

        try {
            List<OutboxEvent> events = outboxEventService.listDispatchableEvents(outboxProperties.getBatchSize());
            for (OutboxEvent event : events) {
                dispatchSingle(event);
            }
        } finally {
            dispatching.set(false);
        }
    }

    public void dispatchSingle(OutboxEvent event) {
        OutboxPublishResult result = outboxPublisher.publish(event);
        if (result.isSuccess()) {
            outboxEventService.markSent(event.getId());
            return;
        }

        int nextRetryCount = event.getRetryCount() + 1;
        if (nextRetryCount > event.getMaxRetry()) {
            outboxEventService.markDead(event.getId(), result.getError());
            markSeckillFailed(event);
            return;
        }

        outboxEventService.markRetry(event.getId(), nextRetryCount, result.getError());
    }

    private void markSeckillFailed(OutboxEvent event) {
        try {
            SeckillMessage message = objectMapper.readValue(event.getPayload(), SeckillMessage.class);
            seckillResultStore.markFailed(message.getActivityId(), message.getUserId(), SeckillStatus.FAILED, "message_dispatch_failed");
        } catch (Exception e) {
            log.warn("mark seckill failed from outbox dead event failed, eventId={}, err={}", event.getEventId(), e.getMessage());
        }
    }
}
