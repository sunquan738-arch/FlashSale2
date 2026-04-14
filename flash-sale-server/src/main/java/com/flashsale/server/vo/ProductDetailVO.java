package com.flashsale.server.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDetailVO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal originalPrice;
    private String imageUrl;
    private Integer stock;
    private Integer status;
}
