package com.flashsale.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.gateway.sentinel")
public class GatewaySentinelRuleProperties {

    private List<RouteRule> routeRules = new ArrayList<>();

    public List<RouteRule> getRouteRules() {
        return routeRules;
    }

    public void setRouteRules(List<RouteRule> routeRules) {
        this.routeRules = routeRules;
    }

    public static class RouteRule {
        private String routeId;
        private Integer count;
        private Integer intervalSec = 1;
        private Integer burst = 0;

        public String getRouteId() {
            return routeId;
        }

        public void setRouteId(String routeId) {
            this.routeId = routeId;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Integer getIntervalSec() {
            return intervalSec;
        }

        public void setIntervalSec(Integer intervalSec) {
            this.intervalSec = intervalSec;
        }

        public Integer getBurst() {
            return burst;
        }

        public void setBurst(Integer burst) {
            this.burst = burst;
        }
    }
}
