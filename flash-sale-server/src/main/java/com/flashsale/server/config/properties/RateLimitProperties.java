package com.flashsale.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private Rule defaultRule = new Rule();

    private Rule queryRule = new Rule();

    private Rule seckillRule = new Rule();

    @Data
    public static class Rule {
        private int windowSeconds = 10;
        private int endpointCount = 300;
        private int ipCount = 200;
        private int userCount = 120;
        private int activityCount = 0;
    }
}

