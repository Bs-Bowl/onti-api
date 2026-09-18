package com.bsbowl.onti.domain.space.entity;

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
@Table(name = "participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RecordSpace space;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status;

    private LocalDateTime joinedAt;

    private String photoUrl;

    @Builder
    private Participant(RecordSpace space, String displayName, String email, ParticipantRole role, String photoUrl) {
        this.space = space;
        this.displayName = displayName;
        this.email = email;
        this.role = role != null ? role : ParticipantRole.PARTICIPANT;
        this.status = ParticipantStatus.PENDING;
        this.photoUrl = photoUrl;
    }

    public void update(String displayName, ParticipantRole role, ParticipantStatus status, String photoUrl) {
        if (displayName != null) this.displayName = displayName;
        if (role != null) this.role = role;
        if (status != null) {
            this.status = status;
            if (status == ParticipantStatus.JOINED && this.joinedAt == null) {
                this.joinedAt = LocalDateTime.now();
            }
        }
        if (photoUrl != null) this.photoUrl = photoUrl;
    }
}
