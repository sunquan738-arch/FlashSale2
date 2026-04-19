package com.flashsale.server.interfaces.rest;

import com.flashsale.server.common.result.Result;
import com.flashsale.server.service.SeckillService;
import com.flashsale.server.vo.SeckillResultVO;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @PostMapping("/do/{activityId}")
    public Result<String> doSeckill(@PathVariable @Positive(message = "activity id must be greater than 0") Long activityId) {
        return Result.success(seckillService.doSeckill(activityId));
    }

    @GetMapping("/result/{activityId}")
    public Result<SeckillResultVO> queryResult(@PathVariable @Positive(message = "activity id must be greater than 0") Long activityId) {
        return Result.success(seckillService.querySeckillResult(activityId));
    }
}

