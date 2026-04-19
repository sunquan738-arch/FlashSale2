package com.flashsale.gateway.filter;

import com.flashsale.gateway.config.GatewaySecurityProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_ATTR = "traceId";

    private final GatewaySecurityProperties properties;

    public TraceIdGlobalFilter(GatewaySecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceHeader = properties.getTraceHeader();
        String sourceTraceId = exchange.getRequest().getHeaders().getFirst(traceHeader);
        if (sourceTraceId == null || sourceTraceId.isBlank()) {
            sourceTraceId = UUID.randomUUID().toString().replace("-", "");
        }
        final String traceId = sourceTraceId;

        exchange.getAttributes().put(TRACE_ID_ATTR, traceId);

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header(traceHeader, traceId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> mutatedExchange.getResponse().getHeaders().set(traceHeader, traceId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
