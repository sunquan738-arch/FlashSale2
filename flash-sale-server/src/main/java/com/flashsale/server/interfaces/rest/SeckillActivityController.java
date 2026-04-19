package com.flashsale.server.interfaces.rest;

import com.flashsale.server.common.result.Result;
import com.flashsale.server.service.SeckillActivityService;
import com.flashsale.server.vo.SeckillActivityDetailVO;
import com.flashsale.server.vo.SeckillActivityVO;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/seckill/activities")
@RequiredArgsConstructor
public class SeckillActivityController {

    private final SeckillActivityService seckillActivityService;

    @GetMapping
    public Result<List<SeckillActivityVO>> listActivities() {
        return Result.success(seckillActivityService.listActivities());
    }

    @GetMapping("/{id}")
    public Result<SeckillActivityDetailVO> getActivityDetail(@PathVariable @Positive(message = "activity id must be greater than 0") Long id) {
        return Result.success(seckillActivityService.getActivityDetail(id));
    }
}

