package com.flashsale.server.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends BizException {

    public BusinessException(Integer code, String message) {
        super(code, message);
    }
}
