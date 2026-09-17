package com.bsbowl.onti.domain.user.service;

import com.bsbowl.onti.domain.user.dto.AuthResponse;
import com.bsbowl.onti.domain.user.dto.LoginRequest;
import com.bsbowl.onti.domain.user.dto.SignupRequest;
import com.bsbowl.onti.domain.user.dto.UserResponse;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.config.security.JwtTokenProvider;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();
        userRepository.save(user);
        return new AuthResponse(jwtTokenProvider.generateToken(user.getId()));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
        return new AuthResponse(jwtTokenProvider.generateToken(user.getId()));
    }

    public UserResponse getMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }
}
