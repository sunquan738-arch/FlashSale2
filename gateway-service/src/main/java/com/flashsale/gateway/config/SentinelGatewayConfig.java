package com.flashsale.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.flashsale.gateway.filter.TraceIdGlobalFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class SentinelGatewayConfig {

    private final GatewaySentinelRuleProperties ruleProperties;
    public SentinelGatewayConfig(GatewaySentinelRuleProperties ruleProperties) {
        this.ruleProperties = ruleProperties;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler(ObjectProvider<ViewResolver> viewResolvers,
                                                                                     ServerCodecConfigurer serverCodecConfigurer) {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers.orderedStream().collect(Collectors.toList()), serverCodecConfigurer);
    }

    @Bean
    @Order(-1)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    @PostConstruct
    public void initRules() {
        Set<ApiDefinition> apiDefinitions = new HashSet<>();
        apiDefinitions.add(new ApiDefinition("apiAuth").setPredicateItems(Set.of(
                new ApiPathPredicateItem().setPattern("/api/auth/**").setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
        )));
        GatewayApiDefinitionManager.loadApiDefinitions(apiDefinitions);

        Set<GatewayFlowRule> rules = new HashSet<>();
        for (GatewaySentinelRuleProperties.RouteRule routeRule : ruleProperties.getRouteRules()) {
            GatewayFlowRule rule = new GatewayFlowRule(routeRule.getRouteId())
                    .setCount(routeRule.getCount() == null ? 100 : routeRule.getCount())
                    .setIntervalSec(routeRule.getIntervalSec() == null ? 1 : routeRule.getIntervalSec())
                    .setBurst(routeRule.getBurst() == null ? 0 : routeRule.getBurst());
            rules.add(rule);
        }

        // activityId 热点限流（query param 或 path variable 通过网关参数解析）
        GatewayFlowRule seckillHotRule = new GatewayFlowRule("route-seckill")
                .setCount(80)
                .setIntervalSec(1)
                .setParamItem(new GatewayParamFlowItem()
                        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER)
                        .setFieldName("X-Activity-Id")
                );
        rules.add(seckillHotRule);

        GatewayRuleManager.loadRules(rules);

        BlockRequestHandler blockRequestHandler = (exchange, throwable) -> {
            Map<String, Object> body = new HashMap<>();
            body.put("code", 429);
            body.put("message", "too many requests");
            body.put("data", null);
            body.put("traceId", String.valueOf(exchange.getAttribute(TraceIdGlobalFilter.TRACE_ID_ATTR)));
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body));
        };
        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }
}
