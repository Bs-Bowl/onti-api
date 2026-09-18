package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.SpaceRecordImage;

public record SpaceRecordImageResponse(String id, String url, String caption, int order) {
    public static SpaceRecordImageResponse from(SpaceRecordImage image) {
        return new SpaceRecordImageResponse(image.getId(), image.getUrl(), image.getCaption(), image.getOrder());
    }
}
