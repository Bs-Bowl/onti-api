package com.bsbowl.onti.domain.record.repository;

import com.bsbowl.onti.domain.record.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordRepository extends JpaRepository<Record, String> {
    List<Record> findAllByBookIdOrderByOrderAsc(String bookId);
}
