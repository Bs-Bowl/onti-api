package com.bsbowl.onti.domain.space.controller;

import com.bsbowl.onti.domain.space.dto.SpaceQuestionCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceQuestionResponse;
import com.bsbowl.onti.domain.space.dto.SpaceQuestionUpdateRequest;
import com.bsbowl.onti.domain.space.service.SpaceQuestionService;
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
public class SpaceQuestionController {

    private final SpaceQuestionService spaceQuestionService;

    public SpaceQuestionController(SpaceQuestionService spaceQuestionService) {
        this.spaceQuestionService = spaceQuestionService;
    }

    @PostMapping("/api/spaces/{spaceId}/questions")
    public ResponseEntity<ApiResponse<SpaceQuestionResponse>> create(@AuthenticationPrincipal String userId,
                                                                       @PathVariable String spaceId,
                                                                       @Valid @RequestBody SpaceQuestionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceQuestionService.create(spaceId, userId, request)));
    }

    @GetMapping("/api/spaces/{spaceId}/questions")
    public ResponseEntity<ApiResponse<List<SpaceQuestionResponse>>> list(@AuthenticationPrincipal String userId,
                                                                           @PathVariable String spaceId) {
        return ResponseEntity.ok(ApiResponse.success(spaceQuestionService.list(spaceId, userId)));
    }

    @PatchMapping("/api/questions/{questionId}")
    public ResponseEntity<ApiResponse<SpaceQuestionResponse>> update(@AuthenticationPrincipal String userId,
                                                                       @PathVariable String questionId,
                                                                       @RequestBody SpaceQuestionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceQuestionService.update(questionId, userId, request)));
    }

    @DeleteMapping("/api/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String questionId) {
        spaceQuestionService.delete(questionId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
