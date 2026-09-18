package com.bsbowl.onti.domain.user.entity;

import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;

    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    @Builder
    private User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }

    public void updateProfile(String name, String avatarUrl, String bio) {
        if (name != null) this.name = name;
        if (avatarUrl != null) this.avatarUrl = avatarUrl;
        if (bio != null) this.bio = bio;
    }

    public void changePassword(String newEncodedPassword) {
        this.password = newEncodedPassword;
    }
}
