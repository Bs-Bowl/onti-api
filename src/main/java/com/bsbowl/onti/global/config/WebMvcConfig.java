package com.bsbowl.onti.global.config;

import com.bsbowl.onti.domain.upload.service.ImageUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 업로드된 이미지를 그대로 내려주는 설정. 운영에서는 S3(+CDN)가 이 역할을
 * 대신하게 되므로, 여기 있는 로컬 디스크 서빙은 개발용 임시 조치다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Path uploadDir;

    public WebMvcConfig(@Value("${onti.upload.dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ImageUploadService.URL_PREFIX + "**")
                .addResourceLocations(resourceLocation(uploadDir));
    }

    /**
     * 폴더 경로를 스프링 리소스 위치 문자열로 바꾼다.
     * <p>
     * 끝 슬래시가 없으면 스프링이 이 경로를 "폴더"가 아니라 "파일"로 보고, 하위
     * 파일을 형제 경로에서 찾아 전부 404가 된다. {@link Path#toUri()}는 실제로
     * 존재하는 폴더에만 슬래시를 붙여주는데, 첫 업로드 전에는 폴더가 아직 없을
     * 수 있어(앱 시작 시점) 여기서 직접 보장한다. toUri()를 쓰는 이유는 윈도우
     * 경로(C:\...)도 올바른 file: URL이 되게 하기 위함이다.
     */
    static String resourceLocation(Path directory) {
        String location = directory.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
