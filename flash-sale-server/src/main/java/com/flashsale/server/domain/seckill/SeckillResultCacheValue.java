package com.flashsale.server.domain.seckill;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeckillResultCacheValue {

    private SeckillStatus status;

    private String orderNo;

    private String reason;

    private LocalDateTime updateTime;
}
