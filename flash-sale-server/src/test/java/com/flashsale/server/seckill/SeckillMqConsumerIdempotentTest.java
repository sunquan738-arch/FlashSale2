package com.flashsale.server.seckill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.server.config.properties.RabbitCustomProperties;
import com.flashsale.server.entity.Order;
import com.flashsale.server.entity.SeckillOrder;
import com.flashsale.server.infrastructure.seckill.SeckillResultStore;
import com.flashsale.server.mapper.OrderMapper;
import com.flashsale.server.mapper.ProductMapper;
import com.flashsale.server.mapper.SeckillActivityMapper;
import com.flashsale.server.mapper.SeckillOrderMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

class SeckillMqConsumerIdempotentTest {

    @SuppressWarnings("unchecked")
    @Test
    void consumeShouldBeIdempotentWhenOrderAlreadyExists() throws Exception {
        SeckillOrderMapper seckillOrderMapper = Mockito.mock(SeckillOrderMapper.class);
        SeckillActivityMapper activityMapper = Mockito.mock(SeckillActivityMapper.class);
        ProductMapper productMapper = Mockito.mock(ProductMapper.class);
        OrderMapper orderMapper = Mockito.mock(OrderMapper.class);
        SeckillResultStore seckillResultStore = Mockito.mock(SeckillResultStore.class);
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        TransactionTemplate transactionTemplate = Mockito.mock(TransactionTemplate.class);

        RabbitCustomProperties properties = new RabbitCustomProperties();
        properties.getConsumer().setMaxRetries(3);

        Mockito.doAnswer(invocation -> {
            Consumer<Object> consumer = invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        SeckillOrder exist = new SeckillOrder();
        exist.setOrderId(99L);
        Mockito.when(seckillOrderMapper.selectOne(any())).thenReturn(exist);

        Order order = new Order();
        order.setOrderNo("SO_EXISTING");
        Mockito.when(orderMapper.selectById(99L)).thenReturn(order);

        SeckillMqConsumer consumer = new SeckillMqConsumer(
                seckillOrderMapper,
                activityMapper,
                productMapper,
                orderMapper,
                seckillResultStore,
                rabbitTemplate,
                properties,
                new ObjectMapper(),
                transactionTemplate
        );

        SeckillMessage payload = new SeckillMessage();
        payload.setUserId(1L);
        payload.setActivityId(2L);
        payload.setProductId(3L);
        payload.setSeckillPrice(BigDecimal.TEN);

        byte[] body = new ObjectMapper().writeValueAsBytes(payload);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(1L);
        Message message = new Message(body, messageProperties);

        Channel channel = Mockito.mock(Channel.class);
        consumer.consume(message, channel);

        Mockito.verify(channel).basicAck(1L, false);
        Mockito.verify(orderMapper, never()).insert(Mockito.any(Order.class));
        Mockito.verify(seckillResultStore).markSuccess(2L, 1L, "SO_EXISTING");
    }
}
