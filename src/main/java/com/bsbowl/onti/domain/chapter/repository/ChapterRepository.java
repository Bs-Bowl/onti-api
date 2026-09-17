package com.bsbowl.onti.domain.chapter.repository;

import com.bsbowl.onti.domain.chapter.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, String> {
    List<Chapter> findAllByBookIdOrderByOrderAsc(String bookId);
}
