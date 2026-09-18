package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.BookRecordLink;

public record BookRecordLinkResponse(String id, String bookId, String spaceRecordId, String chapterId, int order) {
    public static BookRecordLinkResponse from(BookRecordLink link) {
        return new BookRecordLinkResponse(link.getId(), link.getBook().getId(), link.getSpaceRecord().getId(),
                link.getChapter() != null ? link.getChapter().getId() : null, link.getOrder());
    }
}
