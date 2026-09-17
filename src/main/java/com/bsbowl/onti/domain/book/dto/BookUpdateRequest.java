package com.bsbowl.onti.domain.book.dto;

import com.bsbowl.onti.domain.book.entity.BookStatus;

public record BookUpdateRequest(String title, String subtitle, String description, BookStatus status) {
}
