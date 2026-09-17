package com.bsbowl.onti.domain.book.dto;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.entity.BookStatus;

public record BookResponse(String id, String title, String subtitle, String description, BookStatus status) {
    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getSubtitle(), book.getDescription(), book.getStatus());
    }
}
