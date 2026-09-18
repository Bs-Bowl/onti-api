package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceKind;
import com.bsbowl.onti.domain.space.entity.SpaceVisibility;

public record SpaceResponse(String id, String title, String topic, String description, String subjectName,
                             SpaceKind kind, SpaceVisibility visibility) {
    public static SpaceResponse from(RecordSpace space) {
        return new SpaceResponse(space.getId(), space.getTitle(), space.getTopic(), space.getDescription(),
                space.getSubjectName(), space.getKind(), space.getVisibility());
    }
}
