package com.bsbowl.onti.domain.record.dto;

import com.bsbowl.onti.domain.record.entity.Record;
import com.bsbowl.onti.domain.record.entity.RecordType;

import java.time.LocalDateTime;
import java.util.List;

public record RecordResponse(String id, String bookId, String chapterId, RecordType type, String title,
                              String content, String mediaUrl, String memo, LocalDateTime recordedAt, int order,
                              List<RecordImageResponse> images) {
    public static RecordResponse from(Record record, List<RecordImageResponse> images) {
        return new RecordResponse(
                record.getId(),
                record.getBook().getId(),
                record.getChapter() != null ? record.getChapter().getId() : null,
                record.getType(),
                record.getTitle(),
                record.getContent(),
                record.getMediaUrl(),
                record.getMemo(),
                record.getRecordedAt(),
                record.getOrder(),
                images
        );
    }
}
