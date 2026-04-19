package com.flashsale.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.redis")
public class RedisKeyProperties {

    private String envPrefix = "dev";

    private boolean useNewKey = true;

    private boolean doubleWriteLegacy = true;

    private boolean readLegacyFallback = true;

    private Cache cache = new Cache();

    @Data
    public static class Cache {
        private long activityTtlSeconds = 300;
        private long productTtlSeconds = 600;
        private long listTtlSeconds = 120;
        private long resultTtlSeconds = 1800;
        private long orderedSetTtlSeconds = 172800;
        private long stockTtlBufferSeconds = 86400;
        private long localCacheTtlSeconds = 30;
        private long localCacheMaxSize = 10_000;
    }
}

