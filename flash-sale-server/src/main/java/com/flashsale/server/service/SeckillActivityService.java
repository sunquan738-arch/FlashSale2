package com.flashsale.server.service;

import com.flashsale.server.vo.SeckillActivityDetailVO;
import com.flashsale.server.vo.SeckillActivityVO;

import java.util.List;

public interface SeckillActivityService {

    List<SeckillActivityVO> listActivities();

    SeckillActivityDetailVO getActivityDetail(Long id);
}
