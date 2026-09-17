package com.bsbowl.onti.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        String name
) {
}
