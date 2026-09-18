package com.bsbowl.onti.domain.space.controller;

import com.bsbowl.onti.domain.space.dto.SpaceRecordCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceRecordImageCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceRecordImageResponse;
import com.bsbowl.onti.domain.space.dto.SpaceRecordResponse;
import com.bsbowl.onti.domain.space.dto.SpaceRecordUpdateRequest;
import com.bsbowl.onti.domain.space.service.SpaceRecordService;
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
public class SpaceRecordController {

    private final SpaceRecordService spaceRecordService;

    public SpaceRecordController(SpaceRecordService spaceRecordService) {
        this.spaceRecordService = spaceRecordService;
    }

    @PostMapping("/api/spaces/{spaceId}/records")
    public ResponseEntity<ApiResponse<SpaceRecordResponse>> create(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String spaceId,
                                                                     @Valid @RequestBody SpaceRecordCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceRecordService.create(spaceId, userId, request)));
    }

    @GetMapping("/api/spaces/{spaceId}/records")
    public ResponseEntity<ApiResponse<List<SpaceRecordResponse>>> list(@AuthenticationPrincipal String userId,
                                                                         @PathVariable String spaceId) {
        return ResponseEntity.ok(ApiResponse.success(spaceRecordService.list(spaceId, userId)));
    }

    @GetMapping("/api/space-records/{recordId}")
    public ResponseEntity<ApiResponse<SpaceRecordResponse>> get(@AuthenticationPrincipal String userId,
                                                                  @PathVariable String recordId) {
        return ResponseEntity.ok(ApiResponse.success(spaceRecordService.get(recordId, userId)));
    }

    @PatchMapping("/api/space-records/{recordId}")
    public ResponseEntity<ApiResponse<SpaceRecordResponse>> update(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String recordId,
                                                                     @RequestBody SpaceRecordUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceRecordService.update(recordId, userId, request)));
    }

    @DeleteMapping("/api/space-records/{recordId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String recordId) {
        spaceRecordService.delete(recordId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/api/space-records/{recordId}/images")
    public ResponseEntity<ApiResponse<SpaceRecordImageResponse>> addImage(@AuthenticationPrincipal String userId,
                                                                            @PathVariable String recordId,
                                                                            @Valid @RequestBody SpaceRecordImageCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceRecordService.addImage(recordId, userId, request)));
    }

    @DeleteMapping("/api/space-records/{recordId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@AuthenticationPrincipal String userId,
                                                           @PathVariable String recordId,
                                                           @PathVariable String imageId) {
        spaceRecordService.deleteImage(recordId, imageId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
