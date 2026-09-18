package com.bsbowl.onti.domain.record.dto;

import com.bsbowl.onti.domain.record.entity.RecordImage;

public record RecordImageResponse(String id, String url, String caption, int order) {
    public static RecordImageResponse from(RecordImage image) {
        return new RecordImageResponse(image.getId(), image.getUrl(), image.getCaption(), image.getOrder());
    }
}
