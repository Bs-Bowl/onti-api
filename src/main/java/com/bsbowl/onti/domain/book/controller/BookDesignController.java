package com.bsbowl.onti.domain.book.controller;

import com.bsbowl.onti.domain.book.dto.BookDesignRequest;
import com.bsbowl.onti.domain.book.dto.BookDesignResponse;
import com.bsbowl.onti.domain.book.service.BookDesignService;
import com.bsbowl.onti.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books/{bookId}/design")
public class BookDesignController {

    private final BookDesignService bookDesignService;

    public BookDesignController(BookDesignService bookDesignService) {
        this.bookDesignService = bookDesignService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<BookDesignResponse>> get(@AuthenticationPrincipal String userId,
                                                                 @PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(bookDesignService.get(bookId, userId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<BookDesignResponse>> upsert(@AuthenticationPrincipal String userId,
                                                                    @PathVariable String bookId,
                                                                    @RequestBody BookDesignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bookDesignService.upsert(bookId, userId, request)));
    }
}
