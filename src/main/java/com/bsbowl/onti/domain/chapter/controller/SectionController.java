package com.bsbowl.onti.domain.chapter.controller;

import com.bsbowl.onti.domain.chapter.dto.SectionCreateRequest;
import com.bsbowl.onti.domain.chapter.dto.SectionResponse;
import com.bsbowl.onti.domain.chapter.dto.SectionUpdateRequest;
import com.bsbowl.onti.domain.chapter.service.SectionService;
import com.bsbowl.onti.global.common.ApiResponse;
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
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @PostMapping("/api/chapters/{chapterId}/sections")
    public ResponseEntity<ApiResponse<SectionResponse>> create(@AuthenticationPrincipal String userId,
                                                                 @PathVariable String chapterId,
                                                                 @RequestBody SectionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.create(chapterId, userId, request)));
    }

    @GetMapping("/api/chapters/{chapterId}/sections")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> list(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String chapterId) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.list(chapterId, userId)));
    }

    @PatchMapping("/api/sections/{sectionId}")
    public ResponseEntity<ApiResponse<SectionResponse>> update(@AuthenticationPrincipal String userId,
                                                                 @PathVariable String sectionId,
                                                                 @RequestBody SectionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.update(sectionId, userId, request)));
    }

    @DeleteMapping("/api/sections/{sectionId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String sectionId) {
        sectionService.delete(sectionId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
