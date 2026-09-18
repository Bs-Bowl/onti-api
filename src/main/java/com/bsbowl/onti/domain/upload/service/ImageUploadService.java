package com.bsbowl.onti.domain.upload.service;

import com.bsbowl.onti.domain.upload.dto.ImageUploadResponse;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * 업로드한 이미지를 서버 로컬 디스크에 저장한다. 운영에서는 S3로 바꿀 예정이라,
 * 바깥에서 보이는 것은 저장 위치와 무관한 `/uploads/{파일명}` 경로 하나뿐이다 —
 * 나중에 이 클래스의 내부만 갈아끼우면 된다.
 */
@Service
public class ImageUploadService {

    /** 정적 서빙 경로(WebMvcConfig의 리소스 핸들러)와 반드시 같아야 한다. */
    public static final String URL_PREFIX = "/uploads/";

    /**
     * 허용하는 이미지 형식과, 저장할 때 붙일 확장자. 확장자를 사용자가 준
     * 파일명에서 뽑지 않고 이 표에서 가져오는 이유는, 파일명에 `../`나
     * `.exe`가 섞여 들어와도 저장 경로에 영향을 줄 수 없게 하기 위함이다.
     */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    private final Path uploadDir;
    private final long maxBytes;

    public ImageUploadService(@Value("${onti.upload.dir}") String uploadDir,
                             @Value("${onti.upload.max-bytes}") long maxBytes) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    public ImageUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
        String extension = ALLOWED_TYPES.get(normalizedContentType(file.getContentType()));
        if (extension == null) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
        // 톰캣의 multipart 제한(application.yml)이 먼저 걸리는 게 보통이지만,
        // 이 서비스를 직접 호출하는 경우에도 같은 한도를 지키게 여기서도 검사한다.
        if (file.getSize() > maxBytes) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }

        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(uploadDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return new ImageUploadResponse(URL_PREFIX + filename);
    }

    /** 브라우저가 `image/png; charset=...`처럼 파라미터를 붙여 보내는 경우가 있다. */
    private String normalizedContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int separator = contentType.indexOf(';');
        String type = separator < 0 ? contentType : contentType.substring(0, separator);
        return type.trim().toLowerCase();
    }
}
