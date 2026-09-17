package com.bsbowl.onti.domain.book.repository;

import com.bsbowl.onti.domain.book.entity.BookDesign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookDesignRepository extends JpaRepository<BookDesign, String> {
    Optional<BookDesign> findByBookId(String bookId);
}
