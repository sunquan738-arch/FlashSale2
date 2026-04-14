package com.flashsale.server.seckill;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SeckillMessage implements Serializable {

    private Long userId;
    private Long activityId;
    private Long productId;
    private BigDecimal seckillPrice;
}
