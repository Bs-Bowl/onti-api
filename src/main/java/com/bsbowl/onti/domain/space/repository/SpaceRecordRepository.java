package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceRecordRepository extends JpaRepository<SpaceRecord, String> {
    List<SpaceRecord> findAllBySpaceId(String spaceId);
}
