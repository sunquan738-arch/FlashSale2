package com.flashsale.server.service.impl;

import com.flashsale.server.application.auth.AuthApplicationService;
import com.flashsale.server.dto.LoginRequestDTO;
import com.flashsale.server.service.AuthService;
import com.flashsale.server.vo.LoginResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthApplicationService authApplicationService;

    @Override
    public LoginResponseVO login(LoginRequestDTO loginRequestDTO) {
        return authApplicationService.login(loginRequestDTO);
    }
}
