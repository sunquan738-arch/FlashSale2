package com.flashsale.server.vo;

import lombok.Data;

@Data
public class SeckillResultVO {

    /**
     * 排队中 / 抢购成功 / 抢购失败
     */
    private String status;

    /**
     * 抢购成功时返回订单号
     */
    private String orderNo;
}
