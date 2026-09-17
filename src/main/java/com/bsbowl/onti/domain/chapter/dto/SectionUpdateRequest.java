package com.bsbowl.onti.domain.chapter.dto;

import com.bsbowl.onti.domain.chapter.entity.SectionStatus;

public record SectionUpdateRequest(String title, String body, SectionStatus status, Integer order) {
}
