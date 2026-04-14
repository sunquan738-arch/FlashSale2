package com.flashsale.server.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivityDetailVO {

    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal productOriginalPrice;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String activityStatus;
}
