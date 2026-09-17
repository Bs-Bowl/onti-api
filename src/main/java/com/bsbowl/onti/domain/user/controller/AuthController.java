package com.bsbowl.onti.domain.user.controller;

import com.bsbowl.onti.domain.user.dto.AuthResponse;
import com.bsbowl.onti.domain.user.dto.LoginRequest;
import com.bsbowl.onti.domain.user.dto.SignupRequest;
import com.bsbowl.onti.domain.user.dto.UserResponse;
import com.bsbowl.onti.domain.user.service.AuthService;
import com.bsbowl.onti.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(authService.getMe(userId)));
    }
}
