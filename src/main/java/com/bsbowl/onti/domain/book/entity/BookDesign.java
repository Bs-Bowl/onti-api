package com.bsbowl.onti.domain.book.entity;

import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "book_designs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookDesign extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, unique = true)
    private Book book;

    @Column(nullable = false)
    private String coverTemplate;

    @Column(nullable = false)
    private String coverColor;

    private String coverImageUrl;

    @Column(nullable = false)
    private String fontFamily;

    @Column(nullable = false)
    private String layoutPreset;

    private String pdfUrl;

    @Builder
    private BookDesign(Book book, String coverTemplate, String coverColor, String coverImageUrl,
                        String fontFamily, String layoutPreset) {
        this.book = book;
        this.coverTemplate = coverTemplate != null ? coverTemplate : "default";
        this.coverColor = coverColor != null ? coverColor : "#B66F65";
        this.coverImageUrl = coverImageUrl;
        this.fontFamily = fontFamily != null ? fontFamily : "Pretendard";
        this.layoutPreset = layoutPreset != null ? layoutPreset : "editorial";
    }

    public void update(String coverTemplate, String coverColor, String coverImageUrl,
                        String fontFamily, String layoutPreset) {
        if (coverTemplate != null) this.coverTemplate = coverTemplate;
        if (coverColor != null) this.coverColor = coverColor;
        if (coverImageUrl != null) this.coverImageUrl = coverImageUrl;
        if (fontFamily != null) this.fontFamily = fontFamily;
        if (layoutPreset != null) this.layoutPreset = layoutPreset;
    }
}
