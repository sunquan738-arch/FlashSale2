package com.flashsale.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitCustomProperties {

    private Consumer consumer = new Consumer();

    private Publisher publisher = new Publisher();

    @Data
    public static class Consumer {
        private int concurrentConsumers = 4;
        private int maxConcurrentConsumers = 16;
        private int prefetch = 50;
        private int maxRetries = 3;
        private long retryDelayMs = 3000;
    }

    @Data
    public static class Publisher {
        private long confirmTimeoutMs = 3000;
    }
}

