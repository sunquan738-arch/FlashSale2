package com.flashsale.server.interfaces.rest;

import com.flashsale.server.common.result.Result;
import com.flashsale.server.dto.LoginRequestDTO;
import com.flashsale.server.service.AuthService;
import com.flashsale.server.vo.LoginResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponseVO> login(@RequestBody @Valid LoginRequestDTO loginRequestDTO) {
        return Result.success(authService.login(loginRequestDTO));
    }
}

