package com.bsbowl.onti.domain.upload.service;

import com.bsbowl.onti.domain.upload.dto.ImageUploadResponse;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploadServiceTest {

    private static final long MAX_BYTES = 10 * 1024 * 1024;

    @TempDir
    Path uploadDir;

    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        imageUploadService = new ImageUploadService(uploadDir.toString(), MAX_BYTES);
    }

    @Test
    void store_savesFileToDiskAndReturnsServableUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "사진.png", "image/png", "png-bytes".getBytes());

        ImageUploadResponse response = imageUploadService.store(file);

        assertThat(response.url()).startsWith("/uploads/").endsWith(".png");
        Path saved = uploadDir.resolve(response.url().substring("/uploads/".length()));
        assertThat(Files.exists(saved)).isTrue();
        assertThat(Files.readAllBytes(saved)).isEqualTo("png-bytes".getBytes());
    }

    @Test
    void store_createsUploadDirectoryWhenMissing() {
        Path nested = uploadDir.resolve("없는/하위/폴더");
        ImageUploadService service = new ImageUploadService(nested.toString(), MAX_BYTES);

        ImageUploadResponse response = service.store(
                new MockMultipartFile("file", "a.jpg", "image/jpeg", "jpg".getBytes()));

        assertThat(Files.exists(nested.resolve(response.url().substring("/uploads/".length())))).isTrue();
    }

    @Test
    void store_sameFilenameTwice_doesNotOverwrite() {
        MockMultipartFile first = new MockMultipartFile("file", "같은이름.png", "image/png", "첫번째".getBytes());
        MockMultipartFile second = new MockMultipartFile("file", "같은이름.png", "image/png", "두번째".getBytes());

        String firstUrl = imageUploadService.store(first).url();
        String secondUrl = imageUploadService.store(second).url();

        assertThat(firstUrl).isNotEqualTo(secondUrl);
    }

    @Test
    void store_derivesExtensionFromContentTypeNotUserFilename() {
        // 사용자가 준 파일명은 경로 조작(../)에 쓰일 수 있어 저장 이름으로 쓰지 않는다.
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../../evil.png.exe", "image/webp", "webp".getBytes());

        ImageUploadResponse response = imageUploadService.store(file);

        assertThat(response.url()).endsWith(".webp");
        assertThat(response.url()).doesNotContain("..").doesNotContain("evil");
    }

    @Test
    void store_emptyFile_throwsInvalidImageFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> imageUploadService.store(empty))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    void store_nonImageContentType_throwsInvalidImageFile() {
        MockMultipartFile pdf = new MockMultipartFile("file", "a.pdf", "application/pdf", "pdf".getBytes());

        assertThatThrownBy(() -> imageUploadService.store(pdf))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    void store_missingContentType_throwsInvalidImageFile() {
        MockMultipartFile unknown = new MockMultipartFile("file", "a.png", null, "bytes".getBytes());

        assertThatThrownBy(() -> imageUploadService.store(unknown))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    void store_fileLargerThanLimit_throwsFileTooLarge() {
        ImageUploadService small = new ImageUploadService(uploadDir.toString(), 10);
        MockMultipartFile big = new MockMultipartFile("file", "a.png", "image/png", new byte[11]);

        assertThatThrownBy(() -> small.store(big))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }
}
