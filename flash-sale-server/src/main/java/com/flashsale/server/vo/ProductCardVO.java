package com.flashsale.server.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCardVO {

    private Long id;
    private String name;
    private BigDecimal originalPrice;
    private String imageUrl;
    private Integer stock;
}
