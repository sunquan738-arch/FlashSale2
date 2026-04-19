package com.flashsale.server.service.impl;

import com.flashsale.server.application.seckill.SeckillActivityApplicationService;
import com.flashsale.server.service.SeckillActivityService;
import com.flashsale.server.vo.SeckillActivityDetailVO;
import com.flashsale.server.vo.SeckillActivityVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeckillActivityServiceImpl implements SeckillActivityService {

    private final SeckillActivityApplicationService seckillActivityApplicationService;

    @Override
    public List<SeckillActivityVO> listActivities() {
        return seckillActivityApplicationService.listActivities();
    }

    @Override
    public SeckillActivityDetailVO getActivityDetail(Long id) {
        return seckillActivityApplicationService.getActivityDetail(id);
    }
}
