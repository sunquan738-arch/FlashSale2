package com.flashsale.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.gateway")
public class GatewaySecurityProperties {

    private List<String> ignoreAuthPaths = new ArrayList<>(List.of(
            "/api/auth/login",
            "/api/products",
            "/api/products/**",
            "/api/seckill/activities",
            "/api/seckill/activities/**",
            "/actuator/**"
    ));

    private String traceHeader = "X-Trace-Id";

    public List<String> getIgnoreAuthPaths() {
        return ignoreAuthPaths;
    }

    public void setIgnoreAuthPaths(List<String> ignoreAuthPaths) {
        this.ignoreAuthPaths = ignoreAuthPaths;
    }

    public String getTraceHeader() {
        return traceHeader;
    }

    public void setTraceHeader(String traceHeader) {
        this.traceHeader = traceHeader;
    }
}
