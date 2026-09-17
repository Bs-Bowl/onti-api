package com.bsbowl.onti.domain.chapter.dto;

import jakarta.validation.constraints.NotBlank;

public record ChapterCreateRequest(@NotBlank String title) {
}
