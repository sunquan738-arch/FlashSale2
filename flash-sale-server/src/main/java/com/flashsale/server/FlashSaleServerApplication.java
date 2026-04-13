package com.flashsale.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.flashsale.server.mapper")
public class FlashSaleServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleServerApplication.class, args);
    }
}
