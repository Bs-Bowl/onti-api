package com.bsbowl.onti.domain.record.dto;

import jakarta.validation.constraints.NotBlank;

public record RecordImageCreateRequest(@NotBlank String url, String caption) {
}
