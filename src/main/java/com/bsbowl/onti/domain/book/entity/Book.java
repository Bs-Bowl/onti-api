package com.bsbowl.onti.domain.book.entity;

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

@Getter
@Entity
@Table(name = "books")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    private String subtitle;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookStatus status;

    @Builder
    private Book(User user, String title, String subtitle, String description) {
        this.user = user;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.status = BookStatus.DRAFT;
    }

    public void update(String title, String subtitle, String description) {
        if (title != null) this.title = title;
        if (subtitle != null) this.subtitle = subtitle;
        if (description != null) this.description = description;
    }

    public void changeStatus(BookStatus status) {
        this.status = status;
    }

    public boolean isOwnedBy(String userId) {
        return this.user.getId().equals(userId);
    }
}
