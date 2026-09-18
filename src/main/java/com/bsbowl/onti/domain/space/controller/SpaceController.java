package com.bsbowl.onti.domain.space.controller;

import com.bsbowl.onti.domain.space.dto.SpaceCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceResponse;
import com.bsbowl.onti.domain.space.dto.SpaceUpdateRequest;
import com.bsbowl.onti.domain.space.service.SpaceService;
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
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SpaceResponse>> create(@AuthenticationPrincipal String userId,
                                                               @Valid @RequestBody SpaceCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceService.create(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SpaceResponse>>> list(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(spaceService.list(userId)));
    }

    @GetMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<SpaceResponse>> get(@AuthenticationPrincipal String userId,
                                                            @PathVariable String spaceId) {
        return ResponseEntity.ok(ApiResponse.success(spaceService.get(spaceId, userId)));
    }

    @PatchMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<SpaceResponse>> update(@AuthenticationPrincipal String userId,
                                                               @PathVariable String spaceId,
                                                               @RequestBody SpaceUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceService.update(spaceId, userId, request)));
    }

    @DeleteMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String spaceId) {
        spaceService.delete(spaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
