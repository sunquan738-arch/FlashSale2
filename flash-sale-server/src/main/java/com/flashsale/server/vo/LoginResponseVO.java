package com.flashsale.server.vo;

import lombok.Data;

@Data
public class LoginResponseVO {

    private String token;
    private UserInfoVO user;
}
