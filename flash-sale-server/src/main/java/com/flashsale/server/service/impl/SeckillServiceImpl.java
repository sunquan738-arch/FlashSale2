package com.flashsale.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.server.common.enums.ResultCode;
import com.flashsale.server.common.exception.BusinessException;
import com.flashsale.server.entity.Order;
import com.flashsale.server.entity.SeckillActivity;
import com.flashsale.server.entity.SeckillOrder;
import com.flashsale.server.mapper.OrderMapper;
import com.flashsale.server.mapper.SeckillActivityMapper;
import com.flashsale.server.mapper.SeckillOrderMapper;
import com.flashsale.server.seckill.SeckillMessage;
import com.flashsale.server.seckill.SeckillScript;
import com.flashsale.server.service.SeckillService;
import com.flashsale.server.utils.UserContext;
import com.flashsale.server.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.flashsale.server.config.RabbitMQConfig.SECKILL_EXCHANGE;
import static com.flashsale.server.config.RabbitMQConfig.SECKILL_ROUTING_KEY;

@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final OrderMapper orderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillScript seckillScript;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public String doSeckill(Long activityId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "please login first");
        }

        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "seckill activity not found");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "activity is not in valid time");
        }

        String stockKey = "seckill:stock:" + activityId;
        String resultKey = buildResultKey(activityId, userId);
        // 首次请求时把活动库存同步到 Redis，后续只读 Redis，避免高并发打 DB。
        stringRedisTemplate.opsForValue().setIfAbsent(
                stockKey,
                String.valueOf(activity.getSeckillStock()),
                1,
                TimeUnit.DAYS
        );

        long luaResult = seckillScript.execute(activityId, userId);
        if (luaResult == 1L) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "you have already purchased");
        }
        if (luaResult == 2L) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "out of stock");
        }

        // 先写入“排队中”结果，前端可立即轮询结果接口。
        stringRedisTemplate.opsForValue().set(resultKey, "0", 30, TimeUnit.MINUTES);

        SeckillMessage message = new SeckillMessage();
        message.setUserId(userId);
        message.setActivityId(activityId);
        message.setProductId(activity.getProductId());
        message.setSeckillPrice(activity.getSeckillPrice());
        rabbitTemplate.convertAndSend(SECKILL_EXCHANGE, SECKILL_ROUTING_KEY, message);

        return "抢购请求已提交，请稍候查询结果";
    }

    @Override
    public SeckillResultVO querySeckillResult(Long activityId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "please login first");
        }

        String resultCache = stringRedisTemplate.opsForValue().get(buildResultKey(activityId, userId));
        SeckillResultVO vo = new SeckillResultVO();

        if ("0".equals(resultCache)) {
            vo.setStatus("排队中");
            return vo;
        }
        if ("-1".equals(resultCache)) {
            vo.setStatus("抢购失败");
            return vo;
        }
        if (StringUtils.hasText(resultCache)) {
            vo.setStatus("抢购成功");
            vo.setOrderNo(resultCache);
            return vo;
        }

        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillOrder::getUserId, userId)
                .eq(SeckillOrder::getActivityId, activityId)
                .last("limit 1");
        SeckillOrder seckillOrder = seckillOrderMapper.selectOne(wrapper);
        if (seckillOrder != null) {
            Order order = orderMapper.selectById(seckillOrder.getOrderId());
            vo.setStatus("抢购成功");
            vo.setOrderNo(order == null ? null : order.getOrderNo());
            return vo;
        }

        vo.setStatus("抢购失败");
        return vo;
    }

    private String buildResultKey(Long activityId, Long userId) {
        return "seckill:result:" + activityId + ":" + userId;
    }
}
