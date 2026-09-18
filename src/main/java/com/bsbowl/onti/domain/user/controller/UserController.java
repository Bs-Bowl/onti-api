package com.bsbowl.onti.domain.user.controller;

import com.bsbowl.onti.domain.user.dto.PasswordChangeRequest;
import com.bsbowl.onti.domain.user.dto.UserProfileUpdateRequest;
import com.bsbowl.onti.domain.user.dto.UserResponse;
import com.bsbowl.onti.domain.user.service.UserService;
import com.bsbowl.onti.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@AuthenticationPrincipal String userId,
                                                                     @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userId, request)));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal String userId,
                                                              @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@AuthenticationPrincipal String userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
