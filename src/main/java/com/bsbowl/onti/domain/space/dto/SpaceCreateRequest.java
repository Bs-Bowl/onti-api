package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.SpaceVisibility;
import jakarta.validation.constraints.NotBlank;

public record SpaceCreateRequest(@NotBlank String title, String topic, String description,
                                  String subjectName, SpaceVisibility visibility) {
}
