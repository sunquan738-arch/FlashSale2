package com.flashsale.server.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.flashsale.server.config.properties.FeatureSwitchProperties;
import com.flashsale.server.config.properties.SentinelRuleProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SentinelRuleInitializer {

    public static final String RESOURCE_SECKILL_DO = "flashsale.seckill.do";
    public static final String RESOURCE_ORDER_WRITE_CHAIN = "flashsale.order.write.chain";

    private final SentinelRuleProperties sentinelRuleProperties;
    private final FeatureSwitchProperties featureSwitchProperties;

    @PostConstruct
    public void init() {
        reloadRules();
    }

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh() {
        reloadRules();
    }

    public void reloadRules() {
        if (!featureSwitchProperties.isSentinelEnabled()) {
            FlowRuleManager.loadRules(Collections.emptyList());
            DegradeRuleManager.loadRules(Collections.emptyList());
            ParamFlowRuleManager.loadRules(Collections.emptyList());
            log.info("sentinel rules disabled by feature switch");
            return;
        }

        ParamFlowRule hotRule = new ParamFlowRule(RESOURCE_SECKILL_DO)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setParamIdx(0)
                .setCount(sentinelRuleProperties.getSeckillHotActivityQps())
                .setDurationInSec(sentinelRuleProperties.getSeckillHotActivityWindowSec());

        FlowRule orderWriteFlowRule = new FlowRule();
        orderWriteFlowRule.setResource(RESOURCE_ORDER_WRITE_CHAIN);
        orderWriteFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        orderWriteFlowRule.setCount(sentinelRuleProperties.getOrderWriteFlowQps());

        DegradeRule orderWriteDegrade = new DegradeRule();
        orderWriteDegrade.setResource(RESOURCE_ORDER_WRITE_CHAIN);
        orderWriteDegrade.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        orderWriteDegrade.setCount(sentinelRuleProperties.getOrderWriteDegradeRtMs());
        orderWriteDegrade.setTimeWindow(sentinelRuleProperties.getOrderWriteDegradeWindowSec());
        orderWriteDegrade.setMinRequestAmount(sentinelRuleProperties.getOrderWriteMinRequestAmount());
        orderWriteDegrade.setStatIntervalMs(10000);

        ParamFlowRuleManager.loadRules(List.of(hotRule));
        FlowRuleManager.loadRules(List.of(orderWriteFlowRule));
        DegradeRuleManager.loadRules(List.of(orderWriteDegrade));

        log.info("sentinel rules loaded: seckillHotQps={}, orderWriteQps={}, orderWriteDegradeRtMs={}",
                sentinelRuleProperties.getSeckillHotActivityQps(),
                sentinelRuleProperties.getOrderWriteFlowQps(),
                sentinelRuleProperties.getOrderWriteDegradeRtMs());
    }
}
