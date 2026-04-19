package com.flashsale.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.seckill.preload")
public class SeckillPreloadProperties {

    private boolean enabled = true;

    private long fixedDelayMs = 60000;

    private int lookAheadMinutes = 120;

    private int lookBackMinutes = 30;
}

