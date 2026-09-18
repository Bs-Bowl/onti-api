package com.bsbowl.onti.domain.space.controller;

import com.bsbowl.onti.domain.space.dto.BookRecordLinkCreateRequest;
import com.bsbowl.onti.domain.space.dto.BookRecordLinkResponse;
import com.bsbowl.onti.domain.space.service.BookRecordLinkService;
import com.bsbowl.onti.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookRecordLinkController {

    private final BookRecordLinkService bookRecordLinkService;

    public BookRecordLinkController(BookRecordLinkService bookRecordLinkService) {
        this.bookRecordLinkService = bookRecordLinkService;
    }

    @PostMapping("/api/books/{bookId}/record-links")
    public ResponseEntity<ApiResponse<BookRecordLinkResponse>> create(@AuthenticationPrincipal String userId,
                                                                        @PathVariable String bookId,
                                                                        @Valid @RequestBody BookRecordLinkCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bookRecordLinkService.create(bookId, userId, request)));
    }

    @GetMapping("/api/books/{bookId}/record-links")
    public ResponseEntity<ApiResponse<List<BookRecordLinkResponse>>> list(@AuthenticationPrincipal String userId,
                                                                            @PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(bookRecordLinkService.list(bookId, userId)));
    }

    @DeleteMapping("/api/record-links/{linkId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String linkId) {
        bookRecordLinkService.delete(linkId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
