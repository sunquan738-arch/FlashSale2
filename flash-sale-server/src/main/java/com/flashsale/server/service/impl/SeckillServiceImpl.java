package com.flashsale.server.service.impl;

import com.flashsale.server.application.seckill.SeckillApplicationService;
import com.flashsale.server.service.SeckillService;
import com.flashsale.server.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillApplicationService seckillApplicationService;

    @Override
    public String doSeckill(Long activityId) {
        return seckillApplicationService.doSeckill(activityId);
    }

    @Override
    public SeckillResultVO querySeckillResult(Long activityId) {
        return seckillApplicationService.querySeckillResult(activityId);
    }
}
