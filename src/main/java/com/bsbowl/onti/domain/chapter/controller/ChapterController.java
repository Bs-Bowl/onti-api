package com.bsbowl.onti.domain.chapter.controller;

import com.bsbowl.onti.domain.chapter.dto.ChapterCreateRequest;
import com.bsbowl.onti.domain.chapter.dto.ChapterResponse;
import com.bsbowl.onti.domain.chapter.dto.ChapterUpdateRequest;
import com.bsbowl.onti.domain.chapter.service.ChapterService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping("/api/books/{bookId}/chapters")
    public ResponseEntity<ApiResponse<ChapterResponse>> create(@AuthenticationPrincipal String userId,
                                                                 @PathVariable String bookId,
                                                                 @Valid @RequestBody ChapterCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chapterService.create(bookId, userId, request)));
    }

    @GetMapping("/api/books/{bookId}/chapters")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> list(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(chapterService.list(bookId, userId)));
    }

    @PatchMapping("/api/chapters/{chapterId}")
    public ResponseEntity<ApiResponse<ChapterResponse>> update(@AuthenticationPrincipal String userId,
                                                                 @PathVariable String chapterId,
                                                                 @RequestBody ChapterUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chapterService.update(chapterId, userId, request)));
    }

    @DeleteMapping("/api/chapters/{chapterId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String chapterId) {
        chapterService.delete(chapterId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
