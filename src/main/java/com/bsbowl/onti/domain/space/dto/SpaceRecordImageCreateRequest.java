package com.bsbowl.onti.domain.space.dto;

import jakarta.validation.constraints.NotBlank;

public record SpaceRecordImageCreateRequest(@NotBlank String url, String caption) {
}
