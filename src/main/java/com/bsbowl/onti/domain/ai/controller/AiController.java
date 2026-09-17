package com.bsbowl.onti.domain.ai.controller;

import com.bsbowl.onti.domain.ai.dto.ReviewSuggestionResponse;
import com.bsbowl.onti.domain.ai.dto.StructureSuggestionResponse;
import com.bsbowl.onti.domain.ai.service.AiService;
import com.bsbowl.onti.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/api/books/{bookId}/ai/structure-suggestions")
    public ResponseEntity<ApiResponse<StructureSuggestionResponse>> suggestStructure(@PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(aiService.suggestStructure(bookId)));
    }

    @PostMapping("/api/sections/{sectionId}/ai/review")
    public ResponseEntity<ApiResponse<ReviewSuggestionResponse>> reviewSection(@PathVariable String sectionId) {
        return ResponseEntity.ok(ApiResponse.success(aiService.reviewSection(sectionId)));
    }
}
