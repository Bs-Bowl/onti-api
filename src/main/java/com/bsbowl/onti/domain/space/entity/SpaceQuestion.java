package com.bsbowl.onti.domain.space.entity;

import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "space_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RecordSpace space;

    @Column(nullable = false, length = 1000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionSource source;

    // ponytail: 참여자 삭제 시 그가 만든 질문도 함께 지운다 (단순한 기본값).
    // 질문만 남기고 작성자만 지우고 싶어지면 SET_NULL + nullable로 바꾸면 됨.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_participant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Participant createdBy;

    @ElementCollection
    @CollectionTable(name = "space_question_recipients", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "participant_id")
    private List<String> sentToParticipantIds = new ArrayList<>();

    @Builder
    private SpaceQuestion(RecordSpace space, String text, QuestionSource source, Participant createdBy,
                           List<String> sentToParticipantIds) {
        this.space = space;
        this.text = text;
        this.source = source;
        this.createdBy = createdBy;
        this.sentToParticipantIds = sentToParticipantIds != null
                ? new ArrayList<>(sentToParticipantIds) : new ArrayList<>();
    }

    public void update(String text, List<String> sentToParticipantIds) {
        if (text != null) this.text = text;
        if (sentToParticipantIds != null) {
            this.sentToParticipantIds.clear();
            this.sentToParticipantIds.addAll(sentToParticipantIds);
        }
    }
}
