package com.bsbowl.onti.domain.record.controller;

import com.bsbowl.onti.domain.record.dto.RecordCreateRequest;
import com.bsbowl.onti.domain.record.dto.RecordImageCreateRequest;
import com.bsbowl.onti.domain.record.dto.RecordImageResponse;
import com.bsbowl.onti.domain.record.dto.RecordImageUpdateRequest;
import com.bsbowl.onti.domain.record.dto.RecordResponse;
import com.bsbowl.onti.domain.record.dto.RecordUpdateRequest;
import com.bsbowl.onti.domain.record.service.RecordService;
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
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping("/api/books/{bookId}/records")
    public ResponseEntity<ApiResponse<RecordResponse>> create(@AuthenticationPrincipal String userId,
                                                                @PathVariable String bookId,
                                                                @Valid @RequestBody RecordCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recordService.create(bookId, userId, request)));
    }

    @GetMapping("/api/books/{bookId}/records")
    public ResponseEntity<ApiResponse<List<RecordResponse>>> list(@AuthenticationPrincipal String userId,
                                                                    @PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(recordService.list(bookId, userId)));
    }

    @PatchMapping("/api/records/{recordId}")
    public ResponseEntity<ApiResponse<RecordResponse>> update(@AuthenticationPrincipal String userId,
                                                                @PathVariable String recordId,
                                                                @RequestBody RecordUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recordService.update(recordId, userId, request)));
    }

    @DeleteMapping("/api/records/{recordId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String recordId) {
        recordService.delete(recordId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/api/records/{recordId}/images")
    public ResponseEntity<ApiResponse<RecordImageResponse>> addImage(@AuthenticationPrincipal String userId,
                                                                       @PathVariable String recordId,
                                                                       @Valid @RequestBody RecordImageCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recordService.addImage(recordId, userId, request)));
    }

    @PatchMapping("/api/records/{recordId}/images/{imageId}")
    public ResponseEntity<ApiResponse<RecordImageResponse>> updateImage(@AuthenticationPrincipal String userId,
                                                                          @PathVariable String recordId,
                                                                          @PathVariable String imageId,
                                                                          @RequestBody RecordImageUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recordService.updateImage(recordId, imageId, userId, request)));
    }

    @DeleteMapping("/api/records/{recordId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@AuthenticationPrincipal String userId,
                                                           @PathVariable String recordId,
                                                           @PathVariable String imageId) {
        recordService.deleteImage(recordId, imageId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
