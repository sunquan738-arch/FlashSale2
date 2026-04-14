package com.flashsale.server.service;

import com.flashsale.server.vo.SeckillResultVO;

public interface SeckillService {

    String doSeckill(Long activityId);

    SeckillResultVO querySeckillResult(Long activityId);
}
