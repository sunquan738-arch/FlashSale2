package com.flashsale.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.server.common.enums.ResultCode;
import com.flashsale.server.common.exception.BusinessException;
import com.flashsale.server.dto.LoginRequestDTO;
import com.flashsale.server.entity.User;
import com.flashsale.server.mapper.UserMapper;
import com.flashsale.server.service.AuthService;
import com.flashsale.server.utils.JwtUtils;
import com.flashsale.server.vo.LoginResponseVO;
import com.flashsale.server.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    @Override
    public LoginResponseVO login(LoginRequestDTO loginRequestDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginRequestDTO.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null || !StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "invalid username or password");
        }

        if (!user.getPassword().equals(loginRequestDTO.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "invalid username or password");
        }

        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "account is disabled");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setNickname(user.getNickname());
        userInfoVO.setPhone(user.getPhone());

        LoginResponseVO responseVO = new LoginResponseVO();
        responseVO.setToken(token);
        responseVO.setUser(userInfoVO);
        return responseVO;
    }
}
