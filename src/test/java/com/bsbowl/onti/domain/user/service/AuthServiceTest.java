package com.bsbowl.onti.domain.user.service;

import com.bsbowl.onti.domain.user.dto.LoginRequest;
import com.bsbowl.onti.domain.user.dto.SignupRequest;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.config.security.JwtTokenProvider;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @InjectMocks
    private AuthService authService;

    @Test
    void signup_savesUserAndReturnsToken() {
        SignupRequest request = new SignupRequest("test@onti.com", "password123", "테스터");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(jwtTokenProvider.generateToken(any())).thenReturn("token");

        var response = authService.signup(request);

        assertThat(response.accessToken()).isEqualTo("token");
    }

    @Test
    void signup_duplicateEmail_throwsEmailAlreadyExists() {
        SignupRequest request = new SignupRequest("test@onti.com", "password123", "테스터");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("test@onti.com", "wrong");
        User user = User.builder().email("test@onti.com").password("encoded").name("테스터").build();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }
}
