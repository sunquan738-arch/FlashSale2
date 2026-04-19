package com.flashsale.gateway.filter;

import com.flashsale.gateway.config.GatewaySecurityProperties;
import com.flashsale.gateway.support.GatewayException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final GatewaySecurityProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SecretKey secretKey;

    public JwtAuthGlobalFilter(GatewaySecurityProperties properties,
                               @Value("${jwt.secret:ReplaceWithAtLeast32ByteSecretKey123456}") String secret) {
        this.properties = properties;
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret length must be at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(bytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isIgnoredPath(path)) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Mono.error(new GatewayException(HttpStatus.UNAUTHORIZED, "unauthorized"));
        }

        String token = auth.substring(7);
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-Gateway-Verified", "true")
                    .header("X-Auth-UserId", claims.getSubject())
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (JwtException | IllegalArgumentException e) {
            return Mono.error(new GatewayException(HttpStatus.UNAUTHORIZED, "unauthorized"));
        }
    }

    private boolean isIgnoredPath(String path) {
        return properties.getIgnoreAuthPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
