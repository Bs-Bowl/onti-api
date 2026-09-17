package com.bsbowl.onti.domain.record.dto;

import com.bsbowl.onti.domain.record.entity.Record;
import com.bsbowl.onti.domain.record.entity.RecordType;

import java.time.LocalDateTime;

public record RecordResponse(String id, String bookId, String chapterId, RecordType type, String content,
                              String mediaUrl, String memo, LocalDateTime recordedAt, int order) {
    public static RecordResponse from(Record record) {
        return new RecordResponse(
                record.getId(),
                record.getBook().getId(),
                record.getChapter() != null ? record.getChapter().getId() : null,
                record.getType(),
                record.getContent(),
                record.getMediaUrl(),
                record.getMemo(),
                record.getRecordedAt(),
                record.getOrder()
        );
    }
}
