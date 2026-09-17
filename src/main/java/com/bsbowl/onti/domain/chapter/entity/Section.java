package com.bsbowl.onti.domain.chapter.entity;

import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "sections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Section extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    private String title;

    @Lob
    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectionStatus status;

    @Column(nullable = false)
    private int order;

    @Builder
    private Section(Chapter chapter, String title, int order) {
        this.chapter = chapter;
        this.title = title;
        this.body = "";
        this.status = SectionStatus.EMPTY;
        this.order = order;
    }

    public void update(String title, String body, SectionStatus status, Integer order) {
        if (title != null) this.title = title;
        if (body != null) {
            this.body = body;
            if (this.status == SectionStatus.EMPTY) {
                this.status = SectionStatus.DRAFTING;
            }
        }
        if (status != null) this.status = status;
        if (order != null) this.order = order;
    }
}
