package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.record.entity.RecordType;
import com.bsbowl.onti.domain.space.entity.SpaceVisibility;
import jakarta.validation.constraints.NotNull;

public record SpaceRecordCreateRequest(@NotNull RecordType type, String title, String content,
                                        String authorParticipantId, String answeredQuestionId,
                                        SpaceVisibility visibility, String occurredAt) {
}
