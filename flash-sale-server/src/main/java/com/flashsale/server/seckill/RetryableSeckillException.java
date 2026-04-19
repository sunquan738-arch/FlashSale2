package com.flashsale.server.seckill;

public class RetryableSeckillException extends RuntimeException {

    public RetryableSeckillException(String message) {
        super(message);
    }
}
