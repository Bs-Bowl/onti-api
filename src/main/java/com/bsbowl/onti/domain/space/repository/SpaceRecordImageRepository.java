package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.SpaceRecordImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceRecordImageRepository extends JpaRepository<SpaceRecordImage, String> {
    List<SpaceRecordImage> findAllBySpaceRecordIdOrderByOrderAsc(String spaceRecordId);
}
