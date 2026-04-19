package com.flashsale.server.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.server.common.handler.GlobalExceptionHandler;
import com.flashsale.server.service.AuthService;
import com.flashsale.server.vo.LoginResponseVO;
import com.flashsale.server.vo.UserInfoVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginApiTest {

    private final AuthService authService = Mockito.mock(AuthService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginShouldReturnToken() throws Exception {
        LoginResponseVO response = new LoginResponseVO();
        response.setToken("token-abc");

        UserInfoVO user = new UserInfoVO();
        user.setId(1L);
        user.setUsername("alice");
        response.setUser(user);

        Mockito.when(authService.login(Mockito.any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("alice", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("token-abc"));
    }

    private record LoginPayload(String username, String password) {
    }
}
