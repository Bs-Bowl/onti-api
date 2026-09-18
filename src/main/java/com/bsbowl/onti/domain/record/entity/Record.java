package com.bsbowl.onti.domain.record.entity;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Record extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Chapter chapter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordType type;

    private String title;

    @Column(length = 4000)
    private String content;

    private String mediaUrl;

    private String memo;

    private LocalDateTime recordedAt;

    @Column(name = "sort_order", nullable = false)
    private int order;

    @Builder
    private Record(Book book, RecordType type, String title, String content, String mediaUrl, String memo,
                    LocalDateTime recordedAt, int order) {
        this.book = book;
        this.type = type;
        this.title = title;
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.memo = memo;
        this.recordedAt = recordedAt;
        this.order = order;
    }

    public void update(String title, String content, String mediaUrl, String memo, LocalDateTime recordedAt, Integer order) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (mediaUrl != null) this.mediaUrl = mediaUrl;
        if (memo != null) this.memo = memo;
        if (recordedAt != null) this.recordedAt = recordedAt;
        if (order != null) this.order = order;
    }

    public void linkChapter(Chapter chapter) {
        this.chapter = chapter;
    }
}
