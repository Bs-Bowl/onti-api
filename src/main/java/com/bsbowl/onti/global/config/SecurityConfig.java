package com.bsbowl.onti.global.config;

import com.bsbowl.onti.global.config.security.JwtAuthenticationFilter;
import com.bsbowl.onti.global.config.security.JwtTokenProvider;
import com.bsbowl.onti.global.config.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final String[] allowedOrigins;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                          @Value("${onti.cors.allowed-origins}") String[] allowedOrigins) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/signup", "/api/auth/login", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 업로드된 이미지 조회는 인증 없이 허용한다 — <img src>는
                        // Authorization 헤더를 붙일 수 없다. 파일명이 UUID라 주소를
                        // 모르면 접근할 수 없다(올리기는 아래 규칙대로 인증 필요).
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration(List.of(allowedOrigins)));
        return source;
    }

    /**
     * 브라우저에서 이 API를 부를 수 있는 출처 목록. 로컬 개발 주소만 하드코딩해
     * 두면 배포된 프론트엔드나 터널 주소에서 부를 수 없어, 환경변수
     * (ONTI_CORS_ORIGINS)로 바꿀 수 있게 열어둔다.
     * <p>
     * setAllowedOrigins가 아니라 setAllowedOriginPatterns를 쓰는 이유는
     * {@code https://*.vercel.app}처럼 배포마다 앞부분이 바뀌는 주소를 한 줄로
     * 허용할 수 있어야 하기 때문이다(정확한 주소를 그대로 넣어도 동작한다).
     */
    static CorsConfiguration corsConfiguration(List<String> originPatterns) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        return configuration;
    }
}
