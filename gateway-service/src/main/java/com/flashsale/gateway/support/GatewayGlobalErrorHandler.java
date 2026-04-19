package com.flashsale.gateway.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.gateway.filter.TraceIdGlobalFilter;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(-2)
public class GatewayGlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayGlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        String message = resolveMessage(ex, status);

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status.value());
        body.put("message", message);
        body.put("data", null);
        body.put("traceId", String.valueOf(exchange.getAttribute(TraceIdGlobalFilter.TRACE_ID_ATTR)));
        body.put("path", exchange.getRequest().getURI().getPath());
        body.put("timestamp", OffsetDateTime.now().toString());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            byte[] bytes = "{\"code\":500,\"message\":\"gateway error\"}".getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        }
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof GatewayException gatewayException) {
            return gatewayException.getStatus();
        }
        if (ex instanceof ResponseStatusException statusException) {
            return HttpStatus.valueOf(statusException.getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        if (status == HttpStatus.UNAUTHORIZED) {
            return "unauthorized";
        }
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            return "too many requests";
        }
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "gateway error";
        }
        return ex.getMessage();
    }
}
