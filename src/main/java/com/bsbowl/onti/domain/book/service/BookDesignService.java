package com.bsbowl.onti.domain.book.service;

import com.bsbowl.onti.domain.book.dto.BookDesignRequest;
import com.bsbowl.onti.domain.book.dto.BookDesignResponse;
import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.entity.BookDesign;
import com.bsbowl.onti.domain.book.repository.BookDesignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookDesignService {

    private final BookDesignRepository bookDesignRepository;
    private final BookService bookService;

    public BookDesignService(BookDesignRepository bookDesignRepository, BookService bookService) {
        this.bookDesignRepository = bookDesignRepository;
        this.bookService = bookService;
    }

    public BookDesignResponse get(String bookId, String userId) {
        Book book = bookService.getOwnedBook(bookId, userId);
        BookDesign design = bookDesignRepository.findByBookId(bookId)
                .orElseGet(() -> BookDesign.builder().book(book).build());
        return BookDesignResponse.from(design);
    }

    @Transactional
    public BookDesignResponse upsert(String bookId, String userId, BookDesignRequest request) {
        Book book = bookService.getOwnedBook(bookId, userId);
        BookDesign design = bookDesignRepository.findByBookId(bookId)
                .orElseGet(() -> bookDesignRepository.save(BookDesign.builder().book(book).build()));
        design.update(request.coverTemplate(), request.coverColor(), request.coverImageUrl(),
                request.fontFamily(), request.layoutPreset());
        return BookDesignResponse.from(design);
    }
}
