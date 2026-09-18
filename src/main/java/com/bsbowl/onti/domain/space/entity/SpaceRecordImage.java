package com.bsbowl.onti.domain.space.entity;

import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "space_record_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceRecordImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_record_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SpaceRecord spaceRecord;

    @Column(nullable = false)
    private String url;

    private String caption;

    @Column(name = "sort_order", nullable = false)
    private int order;

    @Builder
    private SpaceRecordImage(SpaceRecord spaceRecord, String url, String caption, int order) {
        this.spaceRecord = spaceRecord;
        this.url = url;
        this.caption = caption;
        this.order = order;
    }
}
