package com.bsbowl.onti.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private static final List<String> ORIGINS =
            List.of("http://localhost:3000", "https://onti-blue.vercel.app", "https://*.vercel.app");

    @Test
    void corsConfiguration_allowsLocalAndDeployedFrontend() {
        CorsConfiguration configuration = SecurityConfig.corsConfiguration(ORIGINS);

        assertThat(configuration.checkOrigin("http://localhost:3000")).isNotNull();
        assertThat(configuration.checkOrigin("https://onti-blue.vercel.app")).isNotNull();
    }

    @Test
    void corsConfiguration_allowsVercelPreviewDeploys() {
        // preview 배포는 커밋마다 주소가 바뀌어서 와일드카드로 받아야 한다.
        CorsConfiguration configuration = SecurityConfig.corsConfiguration(ORIGINS);

        assertThat(configuration.checkOrigin("https://onti-blue-git-develop-bsbowl.vercel.app")).isNotNull();
    }

    @Test
    void corsConfiguration_rejectsUnknownOrigin() {
        CorsConfiguration configuration = SecurityConfig.corsConfiguration(ORIGINS);

        assertThat(configuration.checkOrigin("https://evil.example.com")).isNull();
        // vercel.app으로 끝나는 것처럼 보이지만 실제 도메인은 다른 경우.
        assertThat(configuration.checkOrigin("https://vercel.app.evil.com")).isNull();
    }

    @Test
    void corsConfiguration_allowsMethodsUsedByFrontend() {
        CorsConfiguration configuration = SecurityConfig.corsConfiguration(ORIGINS);

        assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "PATCH", "DELETE", "OPTIONS");
    }
}
