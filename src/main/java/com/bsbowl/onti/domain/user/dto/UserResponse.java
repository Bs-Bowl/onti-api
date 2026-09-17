package com.bsbowl.onti.domain.user.dto;

import com.bsbowl.onti.domain.user.entity.User;

public record UserResponse(String id, String email, String name, String avatarUrl) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl());
    }
}
