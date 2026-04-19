package com.flashsale.server.application.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.server.common.enums.ResultCode;
import com.flashsale.server.common.exception.BusinessException;
import com.flashsale.server.config.properties.PasswordMigrationProperties;
import com.flashsale.server.dto.LoginRequestDTO;
import com.flashsale.server.entity.User;
import com.flashsale.server.infrastructure.security.PasswordCodec;
import com.flashsale.server.mapper.UserMapper;
import com.flashsale.server.utils.JwtUtils;
import com.flashsale.server.vo.LoginResponseVO;
import com.flashsale.server.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final PasswordCodec passwordCodec;
    private final PasswordMigrationProperties passwordMigrationProperties;

    public LoginResponseVO login(LoginRequestDTO loginRequestDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginRequestDTO.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null || !StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "invalid username or password");
        }

        boolean verified = passwordCodec.matches(loginRequestDTO.getPassword(), user.getPassword());

        if (!verified
                && passwordMigrationProperties.isAllowPlainCompat()
                && !passwordCodec.isEncodedPassword(user.getPassword())
                && passwordCodec.matchesLegacyPlain(loginRequestDTO.getPassword(), user.getPassword())) {
            verified = true;
            if (passwordMigrationProperties.isAutoUpgradeOnLogin()) {
                User patch = new User();
                patch.setId(user.getId());
                patch.setPassword(passwordCodec.encode(loginRequestDTO.getPassword()));
                userMapper.updateById(patch);
            }
        }

        if (!verified) {
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
