package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.QuestionSource;
import com.bsbowl.onti.domain.space.entity.SpaceQuestion;

import java.util.List;

public record SpaceQuestionResponse(String id, String spaceId, String text, QuestionSource source,
                                     String createdByParticipantId, List<String> sentToParticipantIds) {
    public static SpaceQuestionResponse from(SpaceQuestion question) {
        return new SpaceQuestionResponse(question.getId(), question.getSpace().getId(), question.getText(),
                question.getSource(), question.getCreatedBy().getId(), question.getSentToParticipantIds());
    }
}
