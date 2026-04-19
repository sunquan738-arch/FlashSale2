package com.flashsale.server.config;

import com.flashsale.server.config.properties.RabbitCustomProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    public static final String SECKILL_EXCHANGE = "flash.sale.exchange";
    public static final String SECKILL_QUEUE = "flash.sale.queue";
    public static final String SECKILL_ROUTING_KEY = "flash.sale.routing";

    public static final String SECKILL_RETRY_EXCHANGE = "flash.sale.retry.exchange";
    public static final String SECKILL_RETRY_QUEUE = "flash.sale.retry.queue";
    public static final String SECKILL_RETRY_ROUTING_KEY = "flash.sale.retry.routing";

    public static final String SECKILL_DLX_EXCHANGE = "flash.sale.dlx.exchange";
    public static final String SECKILL_DLX_QUEUE = "flash.sale.dlx.queue";
    public static final String SECKILL_DLX_ROUTING_KEY = "flash.sale.dlx.routing";

    private final RabbitCustomProperties rabbitCustomProperties;

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange seckillRetryExchange() {
        return new DirectExchange(SECKILL_RETRY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange seckillDlxExchange() {
        return new DirectExchange(SECKILL_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE)
                .deadLetterExchange(SECKILL_DLX_EXCHANGE)
                .deadLetterRoutingKey(SECKILL_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue seckillRetryQueue() {
        return QueueBuilder.durable(SECKILL_RETRY_QUEUE)
                .ttl((int) rabbitCustomProperties.getConsumer().getRetryDelayMs())
                .deadLetterExchange(SECKILL_EXCHANGE)
                .deadLetterRoutingKey(SECKILL_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue seckillDlxQueue() {
        return QueueBuilder.durable(SECKILL_DLX_QUEUE).build();
    }

    @Bean
    public Binding seckillBinding(@Qualifier("seckillQueue") Queue seckillQueue,
                                  @Qualifier("seckillExchange") DirectExchange seckillExchange) {
        return BindingBuilder.bind(seckillQueue).to(seckillExchange).with(SECKILL_ROUTING_KEY);
    }

    @Bean
    public Binding seckillRetryBinding(@Qualifier("seckillRetryQueue") Queue seckillRetryQueue,
                                       @Qualifier("seckillRetryExchange") DirectExchange seckillRetryExchange) {
        return BindingBuilder.bind(seckillRetryQueue).to(seckillRetryExchange).with(SECKILL_RETRY_ROUTING_KEY);
    }

    @Bean
    public Binding seckillDlxBinding(@Qualifier("seckillDlxQueue") Queue seckillDlxQueue,
                                     @Qualifier("seckillDlxExchange") DirectExchange seckillDlxExchange) {
        return BindingBuilder.bind(seckillDlxQueue).to(seckillDlxExchange).with(SECKILL_DLX_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory seckillRabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                                     MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(rabbitCustomProperties.getConsumer().getConcurrentConsumers());
        factory.setMaxConcurrentConsumers(rabbitCustomProperties.getConsumer().getMaxConcurrentConsumers());
        factory.setPrefetchCount(rabbitCustomProperties.getConsumer().getPrefetch());
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
