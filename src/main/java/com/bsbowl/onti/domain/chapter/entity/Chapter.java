package com.bsbowl.onti.domain.chapter.entity;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chapters")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chapter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int order;

    @Builder
    private Chapter(Book book, String title, int order) {
        this.book = book;
        this.title = title;
        this.order = order;
    }

    public void update(String title, Integer order) {
        if (title != null) this.title = title;
        if (order != null) this.order = order;
    }
}
