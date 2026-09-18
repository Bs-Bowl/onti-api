package com.bsbowl.onti.global.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WebMvcConfigTest {

    @TempDir
    Path existingDir;

    @Test
    void resourceLocation_missingDirectory_stillEndsWithSlash() {
        // 앱이 뜰 때는 업로드 폴더가 아직 없을 수 있다. 이때 끝 슬래시가 빠지면
        // 올린 이미지를 전부 404로 못 찾는 버그가 났다(조용히 실패하므로 고정).
        Path notCreatedYet = existingDir.resolve("아직-없는-폴더");

        assertThat(WebMvcConfig.resourceLocation(notCreatedYet)).startsWith("file:").endsWith("/");
    }

    @Test
    void resourceLocation_existingDirectory_hasSingleTrailingSlash() {
        assertThat(WebMvcConfig.resourceLocation(existingDir)).endsWith("/").doesNotEndWith("//");
    }
}
