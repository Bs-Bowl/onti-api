package com.bsbowl.onti.domain.user.service;

import com.bsbowl.onti.domain.user.dto.PasswordChangeRequest;
import com.bsbowl.onti.domain.user.dto.UserProfileUpdateRequest;
import com.bsbowl.onti.domain.user.dto.UserResponse;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse updateProfile(String userId, UserProfileUpdateRequest request) {
        User user = getUser(userId);
        user.updateProfile(request.name(), request.avatarUrl(), request.bio());
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(String userId, PasswordChangeRequest request) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void deleteAccount(String userId) {
        userRepository.delete(getUser(userId));
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
