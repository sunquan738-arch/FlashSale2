package com.flashsale.server.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderItemVO {

    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private String resultStatus;
    private LocalDateTime createTime;
}
