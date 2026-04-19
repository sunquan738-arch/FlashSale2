package com.flashsale.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.sentinel")
public class SentinelRuleProperties {

    private int seckillHotActivityQps = 80;

    private int seckillHotActivityWindowSec = 1;

    private int orderWriteFlowQps = 120;

    private int orderWriteDegradeRtMs = 200;

    private int orderWriteDegradeWindowSec = 10;

    private int orderWriteMinRequestAmount = 20;
}
