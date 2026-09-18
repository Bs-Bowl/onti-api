package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.ParticipantRole;
import jakarta.validation.constraints.NotBlank;

public record ParticipantCreateRequest(@NotBlank String displayName, @NotBlank String email, ParticipantRole role) {
}
