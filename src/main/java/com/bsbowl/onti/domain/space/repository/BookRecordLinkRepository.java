package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.BookRecordLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRecordLinkRepository extends JpaRepository<BookRecordLink, String> {
    List<BookRecordLink> findAllByBookIdOrderByOrderAsc(String bookId);

    boolean existsByBookIdAndSpaceRecordId(String bookId, String spaceRecordId);
}
