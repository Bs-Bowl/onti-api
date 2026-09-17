package com.bsbowl.onti.domain.chapter.repository;

import com.bsbowl.onti.domain.chapter.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, String> {
    List<Section> findAllByChapterIdOrderByOrderAsc(String chapterId);
}
