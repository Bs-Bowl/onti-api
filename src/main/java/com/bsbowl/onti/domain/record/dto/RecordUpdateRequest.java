package com.bsbowl.onti.domain.record.dto;

import java.time.LocalDateTime;

public record RecordUpdateRequest(String content, String mediaUrl, String memo, LocalDateTime recordedAt,
                                   Integer order, String chapterId, boolean unlinkChapter) {
}
