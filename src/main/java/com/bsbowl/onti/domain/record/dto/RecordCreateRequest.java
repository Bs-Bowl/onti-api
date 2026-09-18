package com.bsbowl.onti.domain.record.dto;

import com.bsbowl.onti.domain.record.entity.RecordType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RecordCreateRequest(@NotNull RecordType type, String title, String content, String mediaUrl,
                                   String memo, LocalDateTime recordedAt) {
}
