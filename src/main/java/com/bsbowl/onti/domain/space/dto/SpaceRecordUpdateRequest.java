package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.SpaceVisibility;

public record SpaceRecordUpdateRequest(String title, String content, SpaceVisibility visibility, String occurredAt) {
}
