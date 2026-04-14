package com.flashsale.server.service.impl;

import com.flashsale.server.common.enums.ResultCode;
import com.flashsale.server.common.exception.BusinessException;
import com.flashsale.server.entity.Product;
import com.flashsale.server.entity.SeckillActivity;
import com.flashsale.server.mapper.ProductMapper;
import com.flashsale.server.mapper.SeckillActivityMapper;
import com.flashsale.server.service.SeckillActivityService;
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
public class SeckillActivityServiceImpl implements SeckillActivityService {

    private final SeckillActivityMapper seckillActivityMapper;
    private final ProductMapper productMapper;

    @Override
    public List<SeckillActivityVO> listActivities() {
        List<SeckillActivity> activities = seckillActivityMapper.selectList(null);
        if (activities.isEmpty()) {
            return List.of();
        }

        Set<Long> productIds = activities.stream()
                .map(SeckillActivity::getProductId)
                .collect(Collectors.toSet());

        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
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

    @Override
    public SeckillActivityDetailVO getActivityDetail(Long id) {
        SeckillActivity activity = seckillActivityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "seckill activity not found");
        }

        Product product = productMapper.selectById(activity.getProductId());
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
            return "\u672A\u5F00\u59CB";
        }
        if (now.isAfter(endTime)) {
            return "\u5DF2\u7ED3\u675F";
        }
        return "\u8FDB\u884C\u4E2D";
    }
}
