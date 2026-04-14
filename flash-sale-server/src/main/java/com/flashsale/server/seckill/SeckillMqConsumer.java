package com.flashsale.server.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.flashsale.server.common.enums.ResultCode;
import com.flashsale.server.common.exception.BusinessException;
import com.flashsale.server.entity.Order;
import com.flashsale.server.entity.Product;
import com.flashsale.server.entity.SeckillActivity;
import com.flashsale.server.entity.SeckillOrder;
import com.flashsale.server.mapper.OrderMapper;
import com.flashsale.server.mapper.ProductMapper;
import com.flashsale.server.mapper.SeckillActivityMapper;
import com.flashsale.server.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.flashsale.server.config.RabbitMQConfig.SECKILL_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillMqConsumer {

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillActivityMapper seckillActivityMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = SECKILL_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void consume(SeckillMessage message) {
        Long userId = message.getUserId();
        Long activityId = message.getActivityId();
        Long productId = message.getProductId();
        String resultKey = buildResultKey(activityId, userId);

        try {
            // 1) 幂等校验：同一用户同一活动只允许一笔秒杀订单
            LambdaQueryWrapper<SeckillOrder> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SeckillOrder::getUserId, userId)
                    .eq(SeckillOrder::getProductId, productId)
                    .eq(SeckillOrder::getActivityId, activityId)
                    .last("limit 1");
            SeckillOrder exist = seckillOrderMapper.selectOne(queryWrapper);
            if (exist != null) {
                Order order = orderMapper.selectById(exist.getOrderId());
                stringRedisTemplate.opsForValue().set(
                        resultKey,
                        order == null ? "" : order.getOrderNo(),
                        30,
                        TimeUnit.MINUTES
                );
                return;
            }

            // 2) 扣减活动库存（DB层兜底，防止 Redis 与 DB 长期不一致）
            LambdaUpdateWrapper<SeckillActivity> stockWrapper = new LambdaUpdateWrapper<>();
            stockWrapper.eq(SeckillActivity::getId, activityId)
                    .gt(SeckillActivity::getSeckillStock, 0)
                    .setSql("seckill_stock = seckill_stock - 1");
            int activityRows = seckillActivityMapper.update(null, stockWrapper);
            if (activityRows == 0) {
                markFail(activityId, userId);
                return;
            }

            // 3) 乐观锁扣减商品库存：依赖 product.version 字段
            Product product = productMapper.selectById(productId);
            if (product == null || product.getStock() == null || product.getStock() <= 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "product stock not enough");
            }
            Product toUpdate = new Product();
            toUpdate.setId(productId);
            toUpdate.setStock(product.getStock() - 1);
            toUpdate.setVersion(product.getVersion());
            int productRows = productMapper.updateById(toUpdate);
            if (productRows == 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "optimistic lock update failed");
            }

            // 4) 创建普通订单
            Order order = new Order();
            order.setOrderNo(generateOrderNo(userId));
            order.setUserId(userId);
            order.setProductId(productId);
            order.setQuantity(1);
            order.setTotalAmount(message.getSeckillPrice());
            order.setOrderStatus(0);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.insert(order);

            // 5) 创建秒杀订单
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setUserId(userId);
            seckillOrder.setProductId(productId);
            seckillOrder.setActivityId(activityId);
            seckillOrder.setOrderId(order.getId());
            seckillOrder.setSeckillPrice(message.getSeckillPrice());
            seckillOrder.setOrderStatus(0);
            seckillOrder.setCreateTime(LocalDateTime.now());
            seckillOrder.setUpdateTime(LocalDateTime.now());
            seckillOrderMapper.insert(seckillOrder);

            // 6) 把抢购成功结果回写到 Redis，供查询接口快速读取
            stringRedisTemplate.opsForValue().set(resultKey, order.getOrderNo(), 30, TimeUnit.MINUTES);
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突，视为幂等成功分支
            log.warn("duplicate seckill order, userId={}, activityId={}", userId, activityId);
        } catch (Exception e) {
            markFail(activityId, userId);
            // 抛出异常让 MQ 进入重试/死信流程
            throw e;
        }
    }

    private void markFail(Long activityId, Long userId) {
        stringRedisTemplate.opsForValue().set(buildResultKey(activityId, userId), "-1", 30, TimeUnit.MINUTES);
    }

    private String buildResultKey(Long activityId, Long userId) {
        return "seckill:result:" + activityId + ":" + userId;
    }

    private String generateOrderNo(Long userId) {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int randomPart = ThreadLocalRandom.current().nextInt(100, 999);
        return "SO" + timePart + userId + randomPart;
    }
}
