package com.bsbowl.onti.domain.space.entity;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(name = "book_record_links", uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "record_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookRecordLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SpaceRecord spaceRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Chapter chapter;

    @Column(name = "sort_order", nullable = false)
    private int order;

    @Builder
    private BookRecordLink(Book book, SpaceRecord spaceRecord, Chapter chapter, int order) {
        this.book = book;
        this.spaceRecord = spaceRecord;
        this.chapter = chapter;
        this.order = order;
    }
}
