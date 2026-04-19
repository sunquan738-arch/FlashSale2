package com.flashsale.server.application.seckill;

import com.flashsale.server.common.enums.ResultCode;
import com.flashsale.server.common.exception.BusinessException;
import com.flashsale.server.entity.Product;
import com.flashsale.server.entity.SeckillActivity;
import com.flashsale.server.infrastructure.seckill.SeckillCacheService;
import com.flashsale.server.vo.SeckillActivityDetailVO;
import com.flashsale.server.vo.SeckillActivityVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeckillActivityApplicationService {

    private final SeckillCacheService seckillCacheService;

    public List<SeckillActivityVO> listActivities() {
        List<SeckillActivity> activities = seckillCacheService.listActivities();
        if (activities.isEmpty()) {
            return List.of();
        }

        Set<Long> productIds = activities.stream()
                .map(SeckillActivity::getProductId)
                .collect(Collectors.toSet());

        Map<Long, Product> productMap = seckillCacheService.listProducts().stream()
                .filter(p -> productIds.contains(p.getId()))
                .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));

        return activities.stream().map(activity -> {
            SeckillActivityVO vo = new SeckillActivityVO();
            vo.setId(activity.getId());
            vo.setProductId(activity.getProductId());
            Product product = productMap.get(activity.getProductId());
            vo.setProductName(product == null ? null : product.getName());
            vo.setSeckillPrice(activity.getSeckillPrice());
            vo.setSeckillStock(activity.getSeckillStock());
            vo.setStartTime(activity.getStartTime());
            vo.setEndTime(activity.getEndTime());
            vo.setActivityStatus(resolveActivityStatus(activity.getStartTime(), activity.getEndTime()));
            return vo;
        }).toList();
    }

    public SeckillActivityDetailVO getActivityDetail(Long id) {
        SeckillActivity activity = seckillCacheService.getActivityById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "seckill activity not found");
        }

        Product product = seckillCacheService.getProductById(activity.getProductId());
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "linked product not found");
        }

        SeckillActivityDetailVO vo = new SeckillActivityDetailVO();
        vo.setId(activity.getId());
        vo.setProductId(activity.getProductId());
        vo.setProductName(product.getName());
        vo.setProductOriginalPrice(product.getOriginalPrice());
        vo.setSeckillPrice(activity.getSeckillPrice());
        vo.setSeckillStock(activity.getSeckillStock());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setActivityStatus(resolveActivityStatus(activity.getStartTime(), activity.getEndTime()));
        return vo;
    }

    private String resolveActivityStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return "未开始";
        }
        if (now.isAfter(endTime)) {
            return "已结束";
        }
        return "进行中";
    }
}
