package com.flashsale.server.infrastructure.mq.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.server.config.properties.RabbitCustomProperties;
import com.flashsale.server.entity.outbox.OutboxEvent;
import com.flashsale.server.seckill.SeckillMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static com.flashsale.server.config.RabbitMQConfig.SECKILL_EXCHANGE;
import static com.flashsale.server.config.RabbitMQConfig.SECKILL_ROUTING_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitCustomProperties rabbitCustomProperties;

    public OutboxPublishResult publish(OutboxEvent event) {
        try {
            SeckillMessage message = objectMapper.readValue(event.getPayload(), SeckillMessage.class);
            CorrelationData correlationData = new CorrelationData(event.getEventId());

            rabbitTemplate.convertAndSend(SECKILL_EXCHANGE, SECKILL_ROUTING_KEY, message, msg -> {
                msg.getMessageProperties().setHeader("x-event-id", event.getEventId());
                msg.getMessageProperties().setHeader("x-biz-key", event.getBizKey());
                msg.getMessageProperties().setHeader("x-retry-count", event.getRetryCount());
                return msg;
            }, correlationData);

            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(rabbitCustomProperties.getPublisher().getConfirmTimeoutMs(), TimeUnit.MILLISECONDS);
            if (confirm != null && confirm.isAck()) {
                return new OutboxPublishResult(true, null);
            }

            String error = confirm == null ? "confirm timeout" : confirm.getReason();
            return new OutboxPublishResult(false, error == null ? "publish not acknowledged" : error);
        } catch (Exception e) {
            log.warn("outbox publish failed, eventId={}, err={}", event.getEventId(), e.getMessage());
            return new OutboxPublishResult(false, e.getMessage());
        }
    }
}
