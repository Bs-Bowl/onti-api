package com.bsbowl.onti.domain.space.entity;

import com.bsbowl.onti.domain.user.entity.User;
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
@Table(name = "record_spaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordSpace extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User owner;

    @Column(nullable = false)
    private String title;

    private String topic;

    @Column(length = 2000)
    private String description;

    private String subjectName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceVisibility visibility;

    @Builder
    private RecordSpace(User owner, String title, String topic, String description,
                         String subjectName, SpaceVisibility visibility) {
        this.owner = owner;
        this.title = title;
        this.topic = topic;
        this.description = description;
        this.subjectName = subjectName;
        this.kind = SpaceKind.PERSONAL;
        this.visibility = visibility != null ? visibility : SpaceVisibility.PRIVATE;
    }

    public void update(String title, String topic, String description, String subjectName,
                        SpaceKind kind, SpaceVisibility visibility) {
        if (title != null) this.title = title;
        if (topic != null) this.topic = topic;
        if (description != null) this.description = description;
        if (subjectName != null) this.subjectName = subjectName;
        if (kind != null) this.kind = kind;
        if (visibility != null) this.visibility = visibility;
    }

    public boolean isOwnedBy(String userId) {
        return this.owner.getId().equals(userId);
    }
}
