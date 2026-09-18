package com.bsbowl.onti.domain.book.entity;

import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(name = "book_designs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookDesign extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
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

    /** 표지/내지/책등/뒤표지/타이틀·목차·저자·저작권 페이지 전체 설정(프론트
     * BookDesignSettings)을 그대로 직렬화한 JSON — 프론트가 자유 레이어 배치까지
     * 지원해서 필드마다 컬럼을 두기엔 너무 커, Section.body와 같은 방식으로
     * 통째로 저장한다. */
    @Lob
    private String designJson;

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
                        String fontFamily, String layoutPreset, String designJson) {
        if (coverTemplate != null) this.coverTemplate = coverTemplate;
        if (coverColor != null) this.coverColor = coverColor;
        if (coverImageUrl != null) this.coverImageUrl = coverImageUrl;
        if (fontFamily != null) this.fontFamily = fontFamily;
        if (layoutPreset != null) this.layoutPreset = layoutPreset;
        if (designJson != null) this.designJson = designJson;
    }
}
