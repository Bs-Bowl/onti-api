package com.bsbowl.onti.domain.book.dto;

import com.bsbowl.onti.domain.book.entity.BookDesign;

public record BookDesignResponse(String id, String coverTemplate, String coverColor, String coverImageUrl,
                                  String fontFamily, String layoutPreset, String pdfUrl) {
    public static BookDesignResponse from(BookDesign design) {
        return new BookDesignResponse(design.getId(), design.getCoverTemplate(), design.getCoverColor(),
                design.getCoverImageUrl(), design.getFontFamily(), design.getLayoutPreset(), design.getPdfUrl());
    }
}
