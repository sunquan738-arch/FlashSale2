package com.flashsale.server.service;

import com.flashsale.server.dto.LoginRequestDTO;
import com.flashsale.server.vo.LoginResponseVO;

public interface AuthService {

    LoginResponseVO login(LoginRequestDTO loginRequestDTO);
}
