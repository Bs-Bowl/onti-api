package com.bsbowl.onti.domain.space.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SpaceQuestionCreateRequest(@NotBlank String text, @NotBlank String createdByParticipantId,
                                          List<String> sentToParticipantIds) {
}
