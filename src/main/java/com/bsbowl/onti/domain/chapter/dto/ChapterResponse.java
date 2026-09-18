package com.bsbowl.onti.domain.chapter.dto;

import com.bsbowl.onti.domain.chapter.entity.Chapter;

public record ChapterResponse(String id, String bookId, String title, String note, int order) {
    public static ChapterResponse from(Chapter chapter) {
        return new ChapterResponse(chapter.getId(), chapter.getBook().getId(), chapter.getTitle(),
                chapter.getNote(), chapter.getOrder());
    }
}
