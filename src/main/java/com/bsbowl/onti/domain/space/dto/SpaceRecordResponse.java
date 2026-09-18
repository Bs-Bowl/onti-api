package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.record.entity.RecordType;
import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import com.bsbowl.onti.domain.space.entity.SpaceVisibility;

import java.util.List;

public record SpaceRecordResponse(String id, String spaceId, RecordType type, String title, String content,
                                   String authorParticipantId, String answeredQuestionId, SpaceVisibility visibility,
                                   String occurredAt, List<SpaceRecordImageResponse> images) {
    public static SpaceRecordResponse from(SpaceRecord record, List<SpaceRecordImageResponse> images) {
        return new SpaceRecordResponse(
                record.getId(),
                record.getSpace().getId(),
                record.getType(),
                record.getTitle(),
                record.getContent(),
                record.getAuthor() != null ? record.getAuthor().getId() : null,
                record.getAnsweredQuestion() != null ? record.getAnsweredQuestion().getId() : null,
                record.getVisibility(),
                record.getOccurredAt(),
                images
        );
    }
}
