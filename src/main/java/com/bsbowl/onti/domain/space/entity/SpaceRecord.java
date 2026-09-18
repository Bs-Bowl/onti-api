package com.bsbowl.onti.domain.space.entity;

import com.bsbowl.onti.domain.record.entity.RecordType;
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

@Getter
@Entity
@Table(name = "space_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RecordSpace space;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordType type;

    private String title;

    @Column(length = 4000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_participant_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Participant author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_question_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private SpaceQuestion answeredQuestion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceVisibility visibility;

    private String occurredAt;

    @Builder
    private SpaceRecord(RecordSpace space, RecordType type, String title, String content, Participant author,
                         SpaceQuestion answeredQuestion, SpaceVisibility visibility, String occurredAt) {
        this.space = space;
        this.type = type;
        this.title = title;
        this.content = content;
        this.author = author;
        this.answeredQuestion = answeredQuestion;
        this.visibility = visibility != null ? visibility : SpaceVisibility.PRIVATE;
        this.occurredAt = occurredAt;
    }

    public void update(String title, String content, SpaceVisibility visibility, String occurredAt) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (visibility != null) this.visibility = visibility;
        if (occurredAt != null) this.occurredAt = occurredAt;
    }
}
