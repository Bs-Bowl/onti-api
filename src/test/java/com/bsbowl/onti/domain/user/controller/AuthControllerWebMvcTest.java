package com.bsbowl.onti.domain.user.controller;

import com.bsbowl.onti.domain.user.service.AuthService;
import com.bsbowl.onti.global.config.SecurityConfig;
import com.bsbowl.onti.global.config.security.JwtTokenProvider;
import com.bsbowl.onti.global.config.security.RestAuthenticationEntryPoint;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import com.bsbowl.onti.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises AuthController over the real Spring MVC + Security filter chain
 * (not direct method calls), pinning:
 *  - C1: @Valid failures on @RequestBody map to 400 INVALID_INPUT, not 500.
 *  - I3: unauthenticated requests to a protected endpoint return 401 in the
 *        project's {success:false,...} shape via RestAuthenticationEntryPoint.
 *  - CustomException Korean messages survive MockMvc dispatch UTF-8 intact.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtTokenProvider.class, RestAuthenticationEntryPoint.class})
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void signup_invalidBody_returns400WithInvalidInput() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void me_withoutAuthorizationHeader_returns401InProjectErrorShape() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void signup_emailAlreadyExists_returnsKoreanMessageUtf8Encoded() throws Exception {
        when(authService.signup(any())).thenThrow(new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS));

        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@onti.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.error.message").value("이미 가입된 이메일입니다."))
                .andReturn();

        // Decode the raw response bytes as UTF-8 to prove the Korean text is not mangled on the wire.
        String rawBody = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(rawBody).contains("이미 가입된 이메일입니다.");
    }
}
