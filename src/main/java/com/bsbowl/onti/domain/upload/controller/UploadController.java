package com.bsbowl.onti.domain.upload.controller;

import com.bsbowl.onti.domain.upload.dto.ImageUploadResponse;
import com.bsbowl.onti.domain.upload.service.ImageUploadService;
import com.bsbowl.onti.global.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ImageUploadService imageUploadService;

    public UploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    /**
     * 사진 한 장을 올리고, 그 사진을 가리키는 경로를 받는다. 프론트엔드는 받은
     * url을 기록/책 데이터에 그대로 넣으면 된다 — 지금까지 사진을 base64
     * data URL로 들고 있어 저장 용량을 크게 잡아먹던 문제를 이걸로 대체한다.
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(imageUploadService.store(file)));
    }
}
