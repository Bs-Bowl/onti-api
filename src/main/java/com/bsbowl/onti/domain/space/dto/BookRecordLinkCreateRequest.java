package com.bsbowl.onti.domain.space.dto;

import jakarta.validation.constraints.NotBlank;

public record BookRecordLinkCreateRequest(@NotBlank String spaceRecordId, String chapterId) {
}
