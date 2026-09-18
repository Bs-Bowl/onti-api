package com.bsbowl.onti.domain.record.repository;

import com.bsbowl.onti.domain.record.entity.RecordImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordImageRepository extends JpaRepository<RecordImage, String> {
    List<RecordImage> findAllByRecordIdOrderByOrderAsc(String recordId);
}
