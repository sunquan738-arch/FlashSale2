package com.flashsale.server.application.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.server.common.enums.ResultCode;
import com.flashsale.server.common.exception.BusinessException;
import com.flashsale.server.config.SentinelRuleInitializer;
import com.flashsale.server.config.properties.FeatureSwitchProperties;
import com.flashsale.server.domain.seckill.SeckillResultCacheValue;
import com.flashsale.server.domain.seckill.SeckillStatus;
import com.flashsale.server.entity.Order;
import com.flashsale.server.entity.SeckillActivity;
import com.flashsale.server.entity.SeckillOrder;
import com.flashsale.server.entity.outbox.OutboxEvent;
import com.flashsale.server.infrastructure.mq.outbox.OutboxDispatcher;
import com.flashsale.server.infrastructure.mq.outbox.OutboxEventService;
import com.flashsale.server.infrastructure.redis.RedisKeyManager;
import com.flashsale.server.infrastructure.seckill.SeckillCacheService;
import com.flashsale.server.infrastructure.seckill.SeckillResultStore;
import com.flashsale.server.mapper.OrderMapper;
import com.flashsale.server.mapper.SeckillOrderMapper;
import com.flashsale.server.seckill.SeckillMessage;
import com.flashsale.server.seckill.SeckillRollbackScript;
import com.flashsale.server.seckill.SeckillScript;
import com.flashsale.server.utils.UserContext;
import com.flashsale.server.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SeckillApplicationService {

    private final SeckillOrderMapper seckillOrderMapper;
    private final OrderMapper orderMapper;
    private final SeckillCacheService seckillCacheService;
    private final SeckillResultStore seckillResultStore;
    private final RedisKeyManager redisKeyManager;
    private final SeckillScript seckillScript;
    private final SeckillRollbackScript seckillRollbackScript;
    private final OutboxEventService outboxEventService;
    private final OutboxDispatcher outboxDispatcher;
    private final ObjectMapper objectMapper;
    private final FeatureSwitchProperties featureSwitchProperties;

    public String doSeckill(Long activityId) {
        if (!featureSwitchProperties.isSeckillEnabled()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "seckill is temporarily unavailable");
        }

        Entry entry = null;
        try {
            entry = SphU.entry(SentinelRuleInitializer.RESOURCE_SECKILL_DO, EntryType.IN, 1, activityId);
            return executeSeckill(activityId);
        } catch (BlockException e) {
            throw new BusinessException(429, "too many requests, please retry later");
        } finally {
            if (entry != null) {
                entry.exit(1, activityId);
            }
        }
    }

    private String executeSeckill(Long activityId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "please login first");
        }

        SeckillActivity activity = seckillCacheService.getActivityById(activityId);
        if (activity == null || activity.getStatus() == null || activity.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "seckill activity not found");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "activity is not in valid time");
        }

        seckillCacheService.preloadStock(activity);

        String stockKey = redisKeyManager.scriptStockKey(activityId);
        String orderedUsersKey = redisKeyManager.scriptOrderedUsersKey(activityId);

        long luaResult = seckillScript.execute(stockKey, orderedUsersKey, userId);
        if (luaResult == 1L) {
            seckillResultStore.markFailed(activityId, userId, SeckillStatus.DUPLICATE, "duplicate_order");
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "you have already purchased");
        }
        if (luaResult == 2L) {
            seckillResultStore.markFailed(activityId, userId, SeckillStatus.SOLD_OUT, "sold_out");
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "out of stock");
        }

        seckillResultStore.markQueuing(activityId, userId);

        SeckillMessage message = new SeckillMessage();
        message.setUserId(userId);
        message.setActivityId(activityId);
        message.setProductId(activity.getProductId());
        message.setSeckillPrice(activity.getSeckillPrice());

        OutboxEvent event;
        try {
            event = outboxEventService.createSeckillEvent(toPayload(message), message);
        } catch (Exception e) {
            seckillRollbackScript.rollback(stockKey, orderedUsersKey, userId);
            seckillResultStore.markFailed(activityId, userId, SeckillStatus.FAILED, "event_persist_failed");
            throw new BusinessException(ResultCode.ERROR.getCode(), "request submitted failed, please retry");
        }

        outboxDispatcher.dispatchSingle(event);
        return "抢购请求已提交，请稍候查询结果";
    }

    public SeckillResultVO querySeckillResult(Long activityId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "please login first");
        }

        SeckillResultCacheValue result = seckillResultStore.getResult(activityId, userId);
        if (result != null) {
            return toResultVO(result);
        }

        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillOrder::getUserId, userId)
                .eq(SeckillOrder::getActivityId, activityId)
                .last("limit 1");
        SeckillOrder seckillOrder = seckillOrderMapper.selectOne(wrapper);
        if (seckillOrder != null) {
            Order order = orderMapper.selectById(seckillOrder.getOrderId());
            String orderNo = order == null ? null : order.getOrderNo();
            if (StringUtils.hasText(orderNo)) {
                seckillResultStore.markSuccess(activityId, userId, orderNo);
            }
            SeckillResultCacheValue value = new SeckillResultCacheValue();
            value.setStatus(SeckillStatus.SUCCESS);
            value.setOrderNo(orderNo);
            return toResultVO(value);
        }

        SeckillResultCacheValue failed = new SeckillResultCacheValue();
        failed.setStatus(SeckillStatus.FAILED);
        return toResultVO(failed);
    }

    private SeckillResultVO toResultVO(SeckillResultCacheValue value) {
        SeckillResultVO vo = new SeckillResultVO();
        SeckillStatus status = value.getStatus();
        if (status == SeckillStatus.SUCCESS) {
            vo.setStatus("抢购成功");
            vo.setOrderNo(value.getOrderNo());
            return vo;
        }
        if (status == SeckillStatus.QUEUING) {
            vo.setStatus("排队中");
            return vo;
        }
        vo.setStatus("抢购失败");
        return vo;
    }

    private String toPayload(SeckillMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.ERROR.getCode(), "serialize message failed");
        }
    }
}
