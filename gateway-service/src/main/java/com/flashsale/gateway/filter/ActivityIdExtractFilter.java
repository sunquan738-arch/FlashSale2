package com.flashsale.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ActivityIdExtractFilter implements GlobalFilter, Ordered {

    private static final Pattern SECKILL_DO_PATTERN = Pattern.compile("^/api/seckill/do/(\\d+)$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        Matcher matcher = SECKILL_DO_PATTERN.matcher(path);
        if (!matcher.matches()) {
            return chain.filter(exchange);
        }

        String activityId = matcher.group(1);
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-Activity-Id", activityId)
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
