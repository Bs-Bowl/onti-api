package com.bsbowl.onti.domain.space.controller;

import com.bsbowl.onti.domain.space.dto.ParticipantCreateRequest;
import com.bsbowl.onti.domain.space.dto.ParticipantResponse;
import com.bsbowl.onti.domain.space.dto.ParticipantUpdateRequest;
import com.bsbowl.onti.domain.space.service.ParticipantService;
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
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping("/api/spaces/{spaceId}/participants")
    public ResponseEntity<ApiResponse<ParticipantResponse>> create(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String spaceId,
                                                                     @Valid @RequestBody ParticipantCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(participantService.create(spaceId, userId, request)));
    }

    @GetMapping("/api/spaces/{spaceId}/participants")
    public ResponseEntity<ApiResponse<List<ParticipantResponse>>> list(@AuthenticationPrincipal String userId,
                                                                         @PathVariable String spaceId) {
        return ResponseEntity.ok(ApiResponse.success(participantService.list(spaceId, userId)));
    }

    @PatchMapping("/api/participants/{participantId}")
    public ResponseEntity<ApiResponse<ParticipantResponse>> update(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String participantId,
                                                                     @RequestBody ParticipantUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(participantService.update(participantId, userId, request)));
    }

    @DeleteMapping("/api/participants/{participantId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String participantId) {
        participantService.delete(participantId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
