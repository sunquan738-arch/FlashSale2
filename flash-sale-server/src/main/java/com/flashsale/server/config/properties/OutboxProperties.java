package com.flashsale.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {

    private boolean enabled = true;

    private int batchSize = 100;

    private long dispatchIntervalMs = 2000;

    private int maxRetries = 6;

    private long retryBaseDelayMs = 2000;
}

