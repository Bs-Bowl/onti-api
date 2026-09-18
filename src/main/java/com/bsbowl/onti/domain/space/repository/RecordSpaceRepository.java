package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.RecordSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordSpaceRepository extends JpaRepository<RecordSpace, String> {
    List<RecordSpace> findAllByOwnerId(String ownerId);
}
