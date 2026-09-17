package com.bsbowl.onti.domain.chapter.dto;

import com.bsbowl.onti.domain.chapter.entity.Section;
import com.bsbowl.onti.domain.chapter.entity.SectionStatus;

public record SectionResponse(String id, String chapterId, String title, String body, SectionStatus status, int order) {
    public static SectionResponse from(Section section) {
        return new SectionResponse(section.getId(), section.getChapter().getId(), section.getTitle(),
                section.getBody(), section.getStatus(), section.getOrder());
    }
}
