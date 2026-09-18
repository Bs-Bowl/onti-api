package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.ParticipantRole;
import com.bsbowl.onti.domain.space.entity.ParticipantStatus;

import java.time.LocalDateTime;

public record ParticipantResponse(String id, String spaceId, String displayName, String email, ParticipantRole role,
                                   ParticipantStatus status, LocalDateTime joinedAt, String photoUrl) {
    public static ParticipantResponse from(Participant participant) {
        return new ParticipantResponse(participant.getId(), participant.getSpace().getId(), participant.getDisplayName(),
                participant.getEmail(), participant.getRole(), participant.getStatus(), participant.getJoinedAt(),
                participant.getPhotoUrl());
    }
}
