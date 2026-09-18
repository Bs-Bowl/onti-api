package com.bsbowl.onti.domain.space.dto;

import java.util.List;

public record SpaceQuestionUpdateRequest(String text, List<String> sentToParticipantIds) {
}
