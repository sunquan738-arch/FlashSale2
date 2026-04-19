package com.flashsale.server.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.server.config.SentinelRuleInitializer;
import com.flashsale.server.config.properties.RabbitCustomProperties;
import com.flashsale.server.domain.seckill.SeckillStatus;
import com.flashsale.server.entity.Order;
import com.flashsale.server.entity.Product;
import com.flashsale.server.entity.SeckillActivity;
import com.flashsale.server.entity.SeckillOrder;
import com.flashsale.server.infrastructure.seckill.SeckillResultStore;
import com.flashsale.server.mapper.OrderMapper;
import com.flashsale.server.mapper.ProductMapper;
import com.flashsale.server.mapper.SeckillActivityMapper;
import com.flashsale.server.mapper.SeckillOrderMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.flashsale.server.config.RabbitMQConfig.SECKILL_DLX_QUEUE;
import static com.flashsale.server.config.RabbitMQConfig.SECKILL_QUEUE;
import static com.flashsale.server.config.RabbitMQConfig.SECKILL_RETRY_EXCHANGE;
import static com.flashsale.server.config.RabbitMQConfig.SECKILL_RETRY_ROUTING_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillMqConsumer {

    private static final String RETRY_COUNT_HEADER = "x-retry-count";

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillActivityMapper seckillActivityMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final SeckillResultStore seckillResultStore;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitCustomProperties rabbitCustomProperties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @RabbitListener(queues = SECKILL_QUEUE, containerFactory = "seckillRabbitListenerContainerFactory")
    public void consume(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        int retryCount = resolveRetryCount(message.getMessageProperties().getHeaders());

        SeckillMessage payload;
        try {
            payload = objectMapper.readValue(message.getBody(), SeckillMessage.class);
        } catch (Exception e) {
            log.error("invalid mq payload, send to dead-letter. err={}", e.getMessage());
            channel.basicReject(tag, false);
            return;
        }

        try {
            transactionTemplate.executeWithoutResult(status -> processMessage(payload));
            channel.basicAck(tag, false);
        } catch (RetryableSeckillException e) {
            handleRetryOrDead(channel, message, tag, retryCount, payload, e.getMessage());
        } catch (Exception e) {
            handleRetryOrDead(channel, message, tag, retryCount, payload, e.getMessage());
        }
    }

    @RabbitListener(queues = SECKILL_DLX_QUEUE, containerFactory = "seckillRabbitListenerContainerFactory")
    public void consumeDeadLetter(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            SeckillMessage payload = objectMapper.readValue(message.getBody(), SeckillMessage.class);
            seckillResultStore.markFailed(payload.getActivityId(), payload.getUserId(), SeckillStatus.FAILED, "mq_dead_letter");
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("dead-letter consume failed", e);
            channel.basicAck(tag, false);
        }
    }

    private void processMessage(SeckillMessage message) {
        Long userId = message.getUserId();
        Long activityId = message.getActivityId();
        Long productId = message.getProductId();

        try {
            LambdaQueryWrapper<SeckillOrder> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SeckillOrder::getUserId, userId)
                    .eq(SeckillOrder::getProductId, productId)
                    .eq(SeckillOrder::getActivityId, activityId)
                    .last("limit 1");
            SeckillOrder exist = seckillOrderMapper.selectOne(queryWrapper);
            if (exist != null) {
                Order order = orderMapper.selectById(exist.getOrderId());
                if (order != null) {
                    seckillResultStore.markSuccess(activityId, userId, order.getOrderNo());
                }
                return;
            }

            Entry sentinelEntry = null;
            try {
                sentinelEntry = SphU.entry(SentinelRuleInitializer.RESOURCE_ORDER_WRITE_CHAIN);

                LambdaUpdateWrapper<SeckillActivity> stockWrapper = new LambdaUpdateWrapper<>();
                stockWrapper.eq(SeckillActivity::getId, activityId)
                        .gt(SeckillActivity::getSeckillStock, 0)
                        .setSql("seckill_stock = seckill_stock - 1");
                int activityRows = seckillActivityMapper.update(null, stockWrapper);
                if (activityRows == 0) {
                    seckillResultStore.markFailed(activityId, userId, SeckillStatus.SOLD_OUT, "activity_stock_empty");
                    return;
                }

                Product product = productMapper.selectById(productId);
                if (product == null || product.getStock() == null || product.getStock() <= 0) {
                    throw new RetryableSeckillException("product stock not enough");
                }

                Product toUpdate = new Product();
                toUpdate.setId(productId);
                toUpdate.setStock(product.getStock() - 1);
                toUpdate.setVersion(product.getVersion());
                int productRows = productMapper.updateById(toUpdate);
                if (productRows == 0) {
                    throw new RetryableSeckillException("optimistic lock update failed");
                }

                Order order = new Order();
                order.setOrderNo(generateOrderNo(userId));
                order.setUserId(userId);
                order.setProductId(productId);
                order.setQuantity(1);
                order.setTotalAmount(message.getSeckillPrice());
                order.setOrderStatus(0);
                order.setCreateTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.insert(order);

                SeckillOrder seckillOrder = new SeckillOrder();
                seckillOrder.setUserId(userId);
                seckillOrder.setProductId(productId);
                seckillOrder.setActivityId(activityId);
                seckillOrder.setOrderId(order.getId());
                seckillOrder.setSeckillPrice(message.getSeckillPrice());
                seckillOrder.setOrderStatus(0);
                seckillOrder.setCreateTime(LocalDateTime.now());
                seckillOrder.setUpdateTime(LocalDateTime.now());
                seckillOrderMapper.insert(seckillOrder);

                seckillResultStore.markSuccess(activityId, userId, order.getOrderNo());
            } catch (BlockException e) {
                seckillResultStore.markFailed(activityId, userId, SeckillStatus.FAILED, "order_write_degraded");
                return;
            } finally {
                if (sentinelEntry != null) {
                    sentinelEntry.exit();
                }
            }
        } catch (DuplicateKeyException e) {
            log.warn("duplicate seckill order, userId={}, activityId={}", userId, activityId);
            LambdaQueryWrapper<SeckillOrder> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SeckillOrder::getUserId, userId)
                    .eq(SeckillOrder::getProductId, productId)
                    .eq(SeckillOrder::getActivityId, activityId)
                    .last("limit 1");
            SeckillOrder exist = seckillOrderMapper.selectOne(queryWrapper);
            if (exist != null) {
                Order order = orderMapper.selectById(exist.getOrderId());
                if (order != null) {
                    seckillResultStore.markSuccess(activityId, userId, order.getOrderNo());
                }
            }
        }
    }

    private int resolveRetryCount(Map<String, Object> headers) {
        Object value = headers.get(RETRY_COUNT_HEADER);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void handleRetryOrDead(Channel channel,
                                   Message message,
                                   long tag,
                                   int retryCount,
                                   SeckillMessage payload,
                                   String error) throws IOException {
        if (retryCount >= rabbitCustomProperties.getConsumer().getMaxRetries()) {
            log.error("seckill consume retries exceeded, send dead-letter, activityId={}, userId={}, err={}",
                    payload.getActivityId(), payload.getUserId(), error);
            channel.basicReject(tag, false);
            return;
        }

        try {
            message.getMessageProperties().setHeader(RETRY_COUNT_HEADER, retryCount + 1);
            rabbitTemplate.send(SECKILL_RETRY_EXCHANGE, SECKILL_RETRY_ROUTING_KEY, message);
            channel.basicAck(tag, false);
        } catch (Exception sendEx) {
            log.error("failed to publish retry message, send dead-letter, activityId={}, userId={}, err={}",
                    payload.getActivityId(), payload.getUserId(), sendEx.getMessage());
            channel.basicReject(tag, false);
        }
    }

    private String generateOrderNo(Long userId) {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int randomPart = ThreadLocalRandom.current().nextInt(100, 999);
        return "SO" + timePart + userId + randomPart;
    }
}
