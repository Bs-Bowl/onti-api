package com.bsbowl.onti.domain.book.controller;

import com.bsbowl.onti.domain.book.dto.BookCreateRequest;
import com.bsbowl.onti.domain.book.dto.BookResponse;
import com.bsbowl.onti.domain.book.dto.BookUpdateRequest;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookResponse>> create(@AuthenticationPrincipal String userId,
                                                              @Valid @RequestBody BookCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bookService.create(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookResponse>>> list(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(bookService.list(userId)));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookResponse>> get(@AuthenticationPrincipal String userId,
                                                           @PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(bookService.get(bookId, userId)));
    }

    @PatchMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookResponse>> update(@AuthenticationPrincipal String userId,
                                                              @PathVariable String bookId,
                                                              @RequestBody BookUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bookService.update(bookId, userId, request)));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String bookId) {
        bookService.delete(bookId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
