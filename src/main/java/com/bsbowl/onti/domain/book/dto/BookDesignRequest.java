package com.bsbowl.onti.domain.book.dto;

public record BookDesignRequest(String coverTemplate, String coverColor, String coverImageUrl,
                                 String fontFamily, String layoutPreset, String designJson) {
}
