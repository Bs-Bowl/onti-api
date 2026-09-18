package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.SpaceKind;
import com.bsbowl.onti.domain.space.entity.SpaceVisibility;

public record SpaceUpdateRequest(String title, String topic, String description, String subjectName,
                                  SpaceKind kind, SpaceVisibility visibility) {
}
