package com.flashsale.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.feature")
public class FeatureSwitchProperties {

    private boolean seckillEnabled = true;

    private boolean gatewayJwtEnabled = false;

    private boolean sentinelEnabled = true;
}
