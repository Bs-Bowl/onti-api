package com.bsbowl.onti.domain.book.service;

import com.bsbowl.onti.domain.book.dto.BookCreateRequest;
import com.bsbowl.onti.domain.book.dto.BookResponse;
import com.bsbowl.onti.domain.book.dto.BookUpdateRequest;
import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.repository.BookRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public BookService(BookRepository bookRepository, UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookResponse create(String userId, BookCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = Book.builder()
                .user(user)
                .title(request.title())
                .subtitle(request.subtitle())
                .description(request.description())
                .build();
        return BookResponse.from(bookRepository.save(book));
    }

    public List<BookResponse> list(String userId) {
        return bookRepository.findAllByUserId(userId).stream()
                .map(BookResponse::from)
                .toList();
    }

    public BookResponse get(String bookId, String userId) {
        return BookResponse.from(getOwnedBook(bookId, userId));
    }

    @Transactional
    public BookResponse update(String bookId, String userId, BookUpdateRequest request) {
        Book book = getOwnedBook(bookId, userId);
        book.update(request.title(), request.subtitle(), request.description());
        if (request.status() != null) {
            book.changeStatus(request.status());
        }
        return BookResponse.from(book);
    }

    @Transactional
    public void delete(String bookId, String userId) {
        bookRepository.delete(getOwnedBook(bookId, userId));
    }

    public Book getOwnedBook(String bookId, String userId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        if (!book.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return book;
    }
}
