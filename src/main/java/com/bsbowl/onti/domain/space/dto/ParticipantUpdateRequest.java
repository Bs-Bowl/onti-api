package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.ParticipantRole;
import com.bsbowl.onti.domain.space.entity.ParticipantStatus;

public record ParticipantUpdateRequest(String displayName, ParticipantRole role, ParticipantStatus status, String photoUrl) {
}
