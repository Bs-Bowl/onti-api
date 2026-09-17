package com.bsbowl.onti.domain.book.repository;

import com.bsbowl.onti.domain.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, String> {
    List<Book> findAllByUserId(String userId);
}
