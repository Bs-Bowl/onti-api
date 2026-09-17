# ONTI 백엔드 초기 도메인 스캐폴딩 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** onti-api를 빈 레포에서 Gradle/Spring Boot 프로젝트로 스캐폴딩하고, User/Book/Record/Chapter/Section/BookDesign 전체 도메인 + JWT 인증 + AI 스텁까지 한 라운드에 구현한다.

**Architecture:** CLAUDE.md의 도메인형 패키지 구조(`domain/{user,book,record,chapter,ai}` + `global/{config,exception,common}`)를 그대로 따른다. Section은 chapter 도메인에, BookDesign은 book 도메인에 포함한다. 엔티티 간 관계는 "많은 쪽"에서만 FK를 참조하고 JPA 양방향 컬렉션은 쓰지 않는다 (Repository 조회로 대체) — Chapter/Record 사이의 순환 참조를 피하고 엔티티를 단순하게 유지하기 위함이다. 인증은 Stateless JWT (Bearer 토큰), 소유권 검증은 각 도메인 서비스가 상위 Book까지 거슬러 올라가 확인한다.

**Tech Stack:** Java 21 (Gradle 툴체인 자동 다운로드), Spring Boot 3.3.4, Spring Data JPA, Spring Security, PostgreSQL 16, jjwt 0.12.6, springdoc-openapi 2.6.0, Lombok, JUnit5 + Mockito + AssertJ.

**Spec:** `docs/superpowers/specs/2026-09-17-backend-domain-scaffold-design.md`

## Global Constraints

- Java 21, Spring Boot 3.x, Spring Data JPA, PostgreSQL 16, Spring Security + JWT, Gradle(Groovy DSL), springdoc-openapi — CLAUDE.md 기술 스택
- 패키지는 도메인형 구성 (계층형 금지): `domain/{user,book,record,chapter,ai}` + `global/{config,exception,common}`
- 모든 API 응답은 `{ success, data, error }`로 감싼다. JSON 키 camelCase. 에러 코드는 대문자 스네이크. 에러 메시지는 한국어.
- 엔티티에 `@Setter` 금지, 변경은 의미 있는 메서드로. 생성자는 `@Builder`, 기본 생성자는 protected.
- Controller는 DTO만 다루고 엔티티를 노출하지 않는다.
- 예외는 `CustomException` + `ErrorCode` enum, 전역 핸들러(`GlobalExceptionHandler`)에서 응답 변환.
- `application-local.yml`, `.env` 등 시크릿 파일 커밋 금지. 실제 DB 접속 정보/API 키 하드코딩 금지.
- AI 제안/PDF 생성/파일 업로드/refresh token은 이번 라운드 범위 밖 (스펙 문서의 "범위 밖" 참고) — 스텁만 만들거나 아예 만들지 않는다.
- Record.mediaUrl, BookDesign.coverImageUrl은 URL 문자열만 받는다 (업로드 API 없음).
- 커밋 메시지: `feat / fix / docs / refactor / test / chore` + 한국어 설명. `develop` 브랜치에서 작업.

---

### Task 1: 프로젝트 스캐폴딩 (Gradle + Spring Boot + Docker + Gradle Wrapper)

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/bsbowl/onti/OntiApiApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
- Modify: `.gitignore`

**Interfaces:**
- Produces: 컴파일 가능한 빈 Spring Boot 앱, `./gradlew test`/`./gradlew build` 명령. 이후 모든 태스크가 이 빌드 설정 위에서 클래스를 추가한다.

- [ ] **Step 1: settings.gradle 작성**

```groovy
rootProject.name = 'onti-api'
```

- [ ] **Step 2: build.gradle 작성**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.4'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.bsbowl'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
    runtimeOnly 'org.postgresql:postgresql'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 3: docker-compose.yml 작성**

```yaml
services:
  postgres:
    image: postgres:16
    container_name: onti-postgres
    environment:
      POSTGRES_DB: onti
      POSTGRES_USER: onti
      POSTGRES_PASSWORD: onti
    ports:
      - "5432:5432"
    volumes:
      - onti-postgres-data:/var/lib/postgresql/data

volumes:
  onti-postgres-data:
```

- [ ] **Step 4: 메인 애플리케이션 클래스 작성**

`src/main/java/com/bsbowl/onti/OntiApiApplication.java`

```java
package com.bsbowl.onti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OntiApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OntiApiApplication.class, args);
    }
}
```

- [ ] **Step 5: application.yml 작성**

`src/main/resources/application.yml`

```yaml
spring:
  application:
    name: onti-api
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
  datasource:
    url: jdbc:postgresql://localhost:5432/onti
    username: onti
    password: onti

jwt:
  secret: ${JWT_SECRET:local-dev-secret-key-please-change-before-prod-1234567890}
  expiration-ms: 86400000

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

- [ ] **Step 6: .gitignore를 Java/Gradle 프로젝트용으로 보강**

기존 `.gitignore`(CLAUDE.md 제외 등)는 유지하고 아래 내용을 추가한다. 기존 `*.jar` 규칙이 `gradle-wrapper.jar`까지 지워버리므로 예외 처리를 반드시 포함한다.

```gitignore

# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
.vscode/

# Spring Boot
HELP.md

# Secrets
application-local.yml
.env
```

- [ ] **Step 7: Gradle Wrapper 파일 다운로드**

로컬에 Gradle이 설치되어 있지 않으므로 공식 wrapper 파일을 직접 받는다 (Gradle 8.10 기준).

```bash
mkdir -p gradle/wrapper
curl -sL -o gradlew https://raw.githubusercontent.com/gradle/gradle/v8.10.0/gradlew
curl -sL -o gradlew.bat https://raw.githubusercontent.com/gradle/gradle/v8.10.0/gradlew.bat
curl -sL -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/v8.10.0/gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew
```

`gradle/wrapper/gradle-wrapper.properties` 작성:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 8: 빌드 확인**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL` (처음 실행 시 Gradle이 JDK 21 툴체인과 의존성을 자동 다운로드하므로 몇 분 걸릴 수 있음)

- [ ] **Step 9: 커밋**

```bash
git add settings.gradle build.gradle docker-compose.yml src .gitignore gradlew gradlew.bat gradle
git commit -m "chore: Gradle/Spring Boot 프로젝트 스캐폴딩"
```

---

### Task 2: 전역 공통 인프라 (BaseEntity, ApiResponse, ErrorCode, 예외 처리)

**Files:**
- Create: `src/main/java/com/bsbowl/onti/global/common/BaseEntity.java`
- Create: `src/main/java/com/bsbowl/onti/global/common/ApiResponse.java`
- Create: `src/main/java/com/bsbowl/onti/global/common/ErrorResponse.java`
- Create: `src/main/java/com/bsbowl/onti/global/exception/ErrorCode.java`
- Create: `src/main/java/com/bsbowl/onti/global/exception/CustomException.java`
- Create: `src/main/java/com/bsbowl/onti/global/exception/GlobalExceptionHandler.java`
- Create: `src/main/java/com/bsbowl/onti/global/config/JpaAuditingConfig.java`
- Test: `src/test/java/com/bsbowl/onti/global/common/ApiResponseTest.java`
- Test: `src/test/java/com/bsbowl/onti/global/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: Task 1의 Spring Boot 프로젝트 골격
- Produces: `BaseEntity`(id/createdAt/updatedAt, 이후 모든 엔티티가 상속), `ApiResponse<T>.success(T)` / `ApiResponse.success()` / `ApiResponse.error(ErrorCode)`, `ErrorCode` enum(모든 도메인이 여기에 코드 추가), `CustomException(ErrorCode)`, `GlobalExceptionHandler`

- [ ] **Step 1: BaseEntity 작성**

`src/main/java/com/bsbowl/onti/global/common/BaseEntity.java`

```java
package com.bsbowl.onti.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: JPA Auditing 활성화**

`src/main/java/com/bsbowl/onti/global/config/JpaAuditingConfig.java`

```java
package com.bsbowl.onti.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

- [ ] **Step 3: ErrorCode, ApiResponse, ErrorResponse, CustomException, GlobalExceptionHandler 작성**

`src/main/java/com/bsbowl/onti/global/exception/ErrorCode.java`

```java
package com.bsbowl.onti.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."),
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다."),
    CHAPTER_NOT_FOUND(HttpStatus.NOT_FOUND, "챕터를 찾을 수 없습니다."),
    SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "섹션을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
```

`src/main/java/com/bsbowl/onti/global/exception/CustomException.java`

```java
package com.bsbowl.onti.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

`src/main/java/com/bsbowl/onti/global/common/ErrorResponse.java`

```java
package com.bsbowl.onti.global.common;

public record ErrorResponse(String code, String message) {
}
```

`src/main/java/com/bsbowl/onti/global/common/ApiResponse.java`

```java
package com.bsbowl.onti.global.common;

import com.bsbowl.onti.global.exception.ErrorCode;

public record ApiResponse<T>(boolean success, T data, ErrorResponse error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorResponse(errorCode.name(), errorCode.getMessage()));
    }
}
```

`src/main/java/com/bsbowl/onti/global/exception/GlobalExceptionHandler.java`

```java
package com.bsbowl.onti.global.exception;

import com.bsbowl.onti.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
```

- [ ] **Step 4: 테스트 작성**

`src/test/java/com/bsbowl/onti/global/common/ApiResponseTest.java`

```java
package com.bsbowl.onti.global.common;

import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_wrapsDataWithSuccessTrue() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("hello");
        assertThat(response.error()).isNull();
    }

    @Test
    void error_wrapsErrorCodeWithSuccessFalse() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.BOOK_NOT_FOUND);

        assertThat(response.success()).isFalse();
        assertThat(response.error().code()).isEqualTo("BOOK_NOT_FOUND");
        assertThat(response.error().message()).isEqualTo("책을 찾을 수 없습니다.");
    }
}
```

`src/test/java/com/bsbowl/onti/global/exception/GlobalExceptionHandlerTest.java`

```java
package com.bsbowl.onti.global.exception;

import com.bsbowl.onti.global.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleCustomException_mapsToErrorCodeStatusAndBody() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleCustomException(new CustomException(ErrorCode.FORBIDDEN));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void handleException_returns500WithGenericError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
```

- [ ] **Step 5: 테스트 실행 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.global.*"`
Expected: `BUILD SUCCESSFUL`, 4개 테스트 통과

- [ ] **Step 6: 커밋**

```bash
git add src
git commit -m "feat: 공통 응답 포맷/예외 처리/BaseEntity 구현"
```

---

### Task 3: JWT 발급/검증 + Security 설정 + Swagger

**Files:**
- Create: `src/main/java/com/bsbowl/onti/global/config/security/JwtTokenProvider.java`
- Create: `src/main/java/com/bsbowl/onti/global/config/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/bsbowl/onti/global/config/security/RestAuthenticationEntryPoint.java`
- Create: `src/main/java/com/bsbowl/onti/global/config/SecurityConfig.java`
- Create: `src/main/java/com/bsbowl/onti/global/config/SwaggerConfig.java`
- Test: `src/test/java/com/bsbowl/onti/global/config/security/JwtTokenProviderTest.java`

**Interfaces:**
- Consumes: Task 1 스캐폴딩, `application.yml`의 `jwt.secret`/`jwt.expiration-ms`, `ApiResponse`/`ErrorCode`(Task 2)
- Produces: `JwtTokenProvider.generateToken(String userId): String`, `JwtTokenProvider.getUserId(String token): String`, `JwtTokenProvider.isValid(String token): boolean` — 이후 user 도메인의 `AuthService`가 사용. `SecurityConfig`가 `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`를 permitAll, 나머지는 인증 요구. 인증된 요청은 `@AuthenticationPrincipal String userId`로 컨트롤러에서 꺼낸다. 인증되지 않은 요청은 `RestAuthenticationEntryPoint`가 가로채 CLAUDE.md 응답 포맷(`{success:false, error:{code:"UNAUTHORIZED", ...}}`)으로 401을 내려준다 (그냥 두면 Spring Security 기본 401 응답이 나가 프론트의 `res.success` 분기와 어긋남).

- [ ] **Step 1: JwtTokenProvider 작성**

`src/main/java/com/bsbowl/onti/global/config/security/JwtTokenProvider.java`

```java
package com.bsbowl.onti.global.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

- [ ] **Step 2: 테스트 작성 및 실행 확인**

`src/test/java/com/bsbowl/onti/global/config/security/JwtTokenProviderTest.java`

```java
package com.bsbowl.onti.global.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-32-bytes-long-1234567890",
                86400000L
        );
    }

    @Test
    void generateToken_thenGetUserId_returnsOriginalUserId() {
        String token = jwtTokenProvider.generateToken("user-123");

        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo("user-123");
    }

    @Test
    void isValid_forTamperedToken_returnsFalse() {
        String token = jwtTokenProvider.generateToken("user-123");

        assertThat(jwtTokenProvider.isValid(token + "tampered")).isFalse();
    }

    @Test
    void isValid_forExpiredToken_returnsFalse() {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-32-bytes-long-1234567890",
                -1000L
        );
        String token = shortLived.generateToken("user-123");

        assertThat(shortLived.isValid(token)).isFalse();
    }
}
```

Run: `./gradlew test --tests "com.bsbowl.onti.global.config.security.*"`
Expected: `BUILD SUCCESSFUL`, 3개 테스트 통과

- [ ] **Step 3: RestAuthenticationEntryPoint 작성**

인증되지 않은 요청이 Spring Security 기본 401 응답(우리 응답 포맷을 따르지 않음) 대신 `{success:false, error:{...}}` 형태로 응답하도록 만든다.

`src/main/java/com/bsbowl/onti/global/config/security/RestAuthenticationEntryPoint.java`

```java
package com.bsbowl.onti.global.config.security;

import com.bsbowl.onti.global.common.ApiResponse;
import com.bsbowl.onti.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(ErrorCode.UNAUTHORIZED)));
    }
}
```

- [ ] **Step 4: JwtAuthenticationFilter 작성**

`src/main/java/com/bsbowl/onti/global/config/security/JwtAuthenticationFilter.java`

```java
package com.bsbowl.onti.global.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && jwtTokenProvider.isValid(token)) {
            String userId = jwtTokenProvider.getUserId(token);
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 5: SecurityConfig 작성**

`src/main/java/com/bsbowl/onti/global/config/SecurityConfig.java`

```java
package com.bsbowl.onti.global.config;

import com.bsbowl.onti.global.config.security.JwtAuthenticationFilter;
import com.bsbowl.onti.global.config.security.JwtTokenProvider;
import com.bsbowl.onti.global.config.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
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
                        .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

- [ ] **Step 6: SwaggerConfig 작성**

`src/main/java/com/bsbowl/onti/global/config/SwaggerConfig.java`

```java
package com.bsbowl.onti.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("ONTI API").version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
```

- [ ] **Step 7: 전체 빌드로 컴파일 확인**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL` (아직 도메인 컨트롤러가 없어 `anyRequest().authenticated()`가 실제로 걸리는 엔드포인트는 없지만 컴파일과 컨텍스트 로딩은 성공해야 함)

- [ ] **Step 8: 커밋**

```bash
git add src
git commit -m "feat: JWT 발급/검증 및 Security, Swagger 설정 구현"
```

---

### Task 4: User/Auth 도메인 (회원가입, 로그인, 내 정보)

**Files:**
- Create: `src/main/java/com/bsbowl/onti/domain/user/entity/User.java`
- Create: `src/main/java/com/bsbowl/onti/domain/user/repository/UserRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/user/dto/SignupRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/user/dto/LoginRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/user/dto/AuthResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/user/dto/UserResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/user/service/AuthService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/user/controller/AuthController.java`
- Test: `src/test/java/com/bsbowl/onti/domain/user/service/AuthServiceTest.java`

**Interfaces:**
- Consumes: `BaseEntity`, `CustomException`/`ErrorCode`(Task 2), `JwtTokenProvider`(Task 3), `PasswordEncoder` 빈(Task 3 SecurityConfig)
- Produces: `User` 엔티티(`getId()/getEmail()/getPassword()/getName()/getAvatarUrl()`), `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/auth/me` — 이후 Book 도메인이 `UserRepository`로 소유자를 조회한다.

- [ ] **Step 1: User 엔티티 작성**

`src/main/java/com/bsbowl/onti/domain/user/entity/User.java`

```java
package com.bsbowl.onti.domain.user.entity;

import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;

    private String avatarUrl;

    @Builder
    private User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }
}
```

- [ ] **Step 2: Repository, DTO 작성**

`src/main/java/com/bsbowl/onti/domain/user/repository/UserRepository.java`

```java
package com.bsbowl.onti.domain.user.repository;

import com.bsbowl.onti.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}
```

`src/main/java/com/bsbowl/onti/domain/user/dto/SignupRequest.java`

```java
package com.bsbowl.onti.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        String name
) {
}
```

`src/main/java/com/bsbowl/onti/domain/user/dto/LoginRequest.java`

```java
package com.bsbowl.onti.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {
}
```

`src/main/java/com/bsbowl/onti/domain/user/dto/AuthResponse.java`

```java
package com.bsbowl.onti.domain.user.dto;

public record AuthResponse(String accessToken) {
}
```

`src/main/java/com/bsbowl/onti/domain/user/dto/UserResponse.java`

```java
package com.bsbowl.onti.domain.user.dto;

import com.bsbowl.onti.domain.user.entity.User;

public record UserResponse(String id, String email, String name, String avatarUrl) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl());
    }
}
```

- [ ] **Step 3: 실패하는 AuthServiceTest 작성**

`src/test/java/com/bsbowl/onti/domain/user/service/AuthServiceTest.java`

```java
package com.bsbowl.onti.domain.user.service;

import com.bsbowl.onti.domain.user.dto.LoginRequest;
import com.bsbowl.onti.domain.user.dto.SignupRequest;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.config.security.JwtTokenProvider;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @InjectMocks
    private AuthService authService;

    @Test
    void signup_savesUserAndReturnsToken() {
        SignupRequest request = new SignupRequest("test@onti.com", "password123", "테스터");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(jwtTokenProvider.generateToken(any())).thenReturn("token");

        var response = authService.signup(request);

        assertThat(response.accessToken()).isEqualTo("token");
    }

    @Test
    void signup_duplicateEmail_throwsEmailAlreadyExists() {
        SignupRequest request = new SignupRequest("test@onti.com", "password123", "테스터");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("test@onti.com", "wrong");
        User user = User.builder().email("test@onti.com").password("encoded").name("테스터").build();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }
}
```

- [ ] **Step 4: 테스트 실패 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.user.service.AuthServiceTest"`
Expected: FAIL (컴파일 에러 — `AuthService` 클래스가 아직 없음)

- [ ] **Step 5: AuthService 구현**

`src/main/java/com/bsbowl/onti/domain/user/service/AuthService.java`

```java
package com.bsbowl.onti.domain.user.service;

import com.bsbowl.onti.domain.user.dto.AuthResponse;
import com.bsbowl.onti.domain.user.dto.LoginRequest;
import com.bsbowl.onti.domain.user.dto.SignupRequest;
import com.bsbowl.onti.domain.user.dto.UserResponse;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.config.security.JwtTokenProvider;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();
        userRepository.save(user);
        return new AuthResponse(jwtTokenProvider.generateToken(user.getId()));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
        return new AuthResponse(jwtTokenProvider.generateToken(user.getId()));
    }

    public UserResponse getMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.user.service.AuthServiceTest"`
Expected: `BUILD SUCCESSFUL`, 3개 테스트 통과

- [ ] **Step 7: AuthController 구현**

`src/main/java/com/bsbowl/onti/domain/user/controller/AuthController.java`

```java
package com.bsbowl.onti.domain.user.controller;

import com.bsbowl.onti.domain.user.dto.AuthResponse;
import com.bsbowl.onti.domain.user.dto.LoginRequest;
import com.bsbowl.onti.domain.user.dto.SignupRequest;
import com.bsbowl.onti.domain.user.dto.UserResponse;
import com.bsbowl.onti.domain.user.service.AuthService;
import com.bsbowl.onti.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(authService.getMe(userId)));
    }
}
```

- [ ] **Step 8: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: 회원가입/로그인/내정보 조회 API 구현"
```

---

### Task 5: Book 도메인 (CRUD + 상태 전환)

**Files:**
- Create: `src/main/java/com/bsbowl/onti/domain/book/entity/BookStatus.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/entity/Book.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/repository/BookRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/dto/BookCreateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/dto/BookUpdateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/dto/BookResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/service/BookService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/controller/BookController.java`
- Test: `src/test/java/com/bsbowl/onti/domain/book/service/BookServiceTest.java`

**Interfaces:**
- Consumes: `User`/`UserRepository`(Task 4), `CustomException`/`ErrorCode`(Task 2)
- Produces: `Book` 엔티티(`getId()/getUser()/getTitle()/getStatus()`, `isOwnedBy(String userId): boolean`), `BookService.getOwnedBook(String bookId, String userId): Book` (책이 없으면 `BOOK_NOT_FOUND`, 소유자가 아니면 `FORBIDDEN`) — Record/Chapter/BookDesign 서비스가 그대로 재사용. `POST/GET /api/books`, `GET/PATCH/DELETE /api/books/{bookId}`

- [ ] **Step 1: BookStatus, Book 엔티티 작성**

`src/main/java/com/bsbowl/onti/domain/book/entity/BookStatus.java`

```java
package com.bsbowl.onti.domain.book.entity;

public enum BookStatus {
    DRAFT, RECORDING, STRUCTURING, WRITING, REVIEWING, DESIGNING, COMPLETED
}
```

`src/main/java/com/bsbowl/onti/domain/book/entity/Book.java`

```java
package com.bsbowl.onti.domain.book.entity;

import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "books")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    private String subtitle;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookStatus status;

    @Builder
    private Book(User user, String title, String subtitle, String description) {
        this.user = user;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.status = BookStatus.DRAFT;
    }

    public void update(String title, String subtitle, String description) {
        if (title != null) this.title = title;
        if (subtitle != null) this.subtitle = subtitle;
        if (description != null) this.description = description;
    }

    public void changeStatus(BookStatus status) {
        this.status = status;
    }

    public boolean isOwnedBy(String userId) {
        return this.user.getId().equals(userId);
    }
}
```

- [ ] **Step 2: Repository, DTO 작성**

`src/main/java/com/bsbowl/onti/domain/book/repository/BookRepository.java`

```java
package com.bsbowl.onti.domain.book.repository;

import com.bsbowl.onti.domain.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, String> {
    List<Book> findAllByUserId(String userId);
}
```

`src/main/java/com/bsbowl/onti/domain/book/dto/BookCreateRequest.java`

```java
package com.bsbowl.onti.domain.book.dto;

import jakarta.validation.constraints.NotBlank;

public record BookCreateRequest(@NotBlank String title, String subtitle, String description) {
}
```

`src/main/java/com/bsbowl/onti/domain/book/dto/BookUpdateRequest.java`

```java
package com.bsbowl.onti.domain.book.dto;

import com.bsbowl.onti.domain.book.entity.BookStatus;

public record BookUpdateRequest(String title, String subtitle, String description, BookStatus status) {
}
```

`src/main/java/com/bsbowl/onti/domain/book/dto/BookResponse.java`

```java
package com.bsbowl.onti.domain.book.dto;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.entity.BookStatus;

public record BookResponse(String id, String title, String subtitle, String description, BookStatus status) {
    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getSubtitle(), book.getDescription(), book.getStatus());
    }
}
```

- [ ] **Step 3: 실패하는 BookServiceTest 작성**

`src/test/java/com/bsbowl/onti/domain/book/service/BookServiceTest.java`

```java
package com.bsbowl.onti.domain.book.service;

import com.bsbowl.onti.domain.book.dto.BookCreateRequest;
import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.entity.BookStatus;
import com.bsbowl.onti.domain.book.repository.BookRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private BookService bookService;

    @Test
    void create_savesBookWithDraftStatus() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookService.create("user-1", new BookCreateRequest("내 책", null, null));

        assertThat(response.status()).isEqualTo(BookStatus.DRAFT);
        assertThat(response.title()).isEqualTo("내 책");
    }

    @Test
    void getOwnedBook_notOwner_throwsForbidden() {
        User owner = User.builder().email("owner@onti.com").password("x").name("owner").build();
        ReflectionTestUtils.setField(owner, "id", "owner-id");
        Book book = Book.builder().user(owner).title("책").build();
        when(bookRepository.findById("book-1")).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.getOwnedBook("book-1", "other-id"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void getOwnedBook_notFound_throwsBookNotFound() {
        when(bookRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getOwnedBook("missing", "user-1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }
}
```

- [ ] **Step 4: 테스트 실패 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.book.service.BookServiceTest"`
Expected: FAIL (컴파일 에러 — `BookService` 클래스가 아직 없음)

- [ ] **Step 5: BookService 구현**

`src/main/java/com/bsbowl/onti/domain/book/service/BookService.java`

```java
package com.bsbowl.onti.domain.book.service;

import com.bsbowl.onti.domain.book.dto.BookCreateRequest;
import com.bsbowl.onti.domain.book.dto.BookResponse;
import com.bsbowl.onti.domain.book.dto.BookUpdateRequest;
import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.repository.BookRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public BookService(BookRepository bookRepository, UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookResponse create(String userId, BookCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = Book.builder()
                .user(user)
                .title(request.title())
                .subtitle(request.subtitle())
                .description(request.description())
                .build();
        return BookResponse.from(bookRepository.save(book));
    }

    public List<BookResponse> list(String userId) {
        return bookRepository.findAllByUserId(userId).stream()
                .map(BookResponse::from)
                .toList();
    }

    public BookResponse get(String bookId, String userId) {
        return BookResponse.from(getOwnedBook(bookId, userId));
    }

    @Transactional
    public BookResponse update(String bookId, String userId, BookUpdateRequest request) {
        Book book = getOwnedBook(bookId, userId);
        book.update(request.title(), request.subtitle(), request.description());
        if (request.status() != null) {
            book.changeStatus(request.status());
        }
        return BookResponse.from(book);
    }

    @Transactional
    public void delete(String bookId, String userId) {
        bookRepository.delete(getOwnedBook(bookId, userId));
    }

    public Book getOwnedBook(String bookId, String userId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        if (!book.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return book;
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.book.service.BookServiceTest"`
Expected: `BUILD SUCCESSFUL`, 3개 테스트 통과

- [ ] **Step 7: BookController 구현**

`src/main/java/com/bsbowl/onti/domain/book/controller/BookController.java`

```java
package com.bsbowl.onti.domain.book.controller;

import com.bsbowl.onti.domain.book.dto.BookCreateRequest;
import com.bsbowl.onti.domain.book.dto.BookResponse;
import com.bsbowl.onti.domain.book.dto.BookUpdateRequest;
import com.bsbowl.onti.domain.book.service.BookService;
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
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookResponse>> create(@AuthenticationPrincipal String userId,
                                                              @Valid @RequestBody BookCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bookService.create(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookResponse>>> list(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(bookService.list(userId)));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookResponse>> get(@AuthenticationPrincipal String userId,
                                                           @PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(bookService.get(bookId, userId)));
    }

    @PatchMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookResponse>> update(@AuthenticationPrincipal String userId,
                                                              @PathVariable String bookId,
                                                              @RequestBody BookUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bookService.update(bookId, userId, request)));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String bookId) {
        bookService.delete(bookId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
```

- [ ] **Step 8: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: Book CRUD 및 상태 전환 API 구현"
```

---

### Task 6: BookDesign (표지/내지 설정 1:1 upsert)

**Files:**
- Create: `src/main/java/com/bsbowl/onti/domain/book/entity/BookDesign.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/repository/BookDesignRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/dto/BookDesignRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/dto/BookDesignResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/service/BookDesignService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/book/controller/BookDesignController.java`
- Test: `src/test/java/com/bsbowl/onti/domain/book/service/BookDesignServiceTest.java`

**Interfaces:**
- Consumes: `BookService.getOwnedBook(String, String): Book`(Task 5)
- Produces: `GET/PUT /api/books/{bookId}/design`

- [ ] **Step 1: BookDesign 엔티티, Repository, DTO 작성**

`src/main/java/com/bsbowl/onti/domain/book/entity/BookDesign.java`

```java
package com.bsbowl.onti.domain.book.entity;

import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "book_designs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookDesign extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, unique = true)
    private Book book;

    @Column(nullable = false)
    private String coverTemplate;

    @Column(nullable = false)
    private String coverColor;

    private String coverImageUrl;

    @Column(nullable = false)
    private String fontFamily;

    @Column(nullable = false)
    private String layoutPreset;

    private String pdfUrl;

    @Builder
    private BookDesign(Book book, String coverTemplate, String coverColor, String coverImageUrl,
                        String fontFamily, String layoutPreset) {
        this.book = book;
        this.coverTemplate = coverTemplate != null ? coverTemplate : "default";
        this.coverColor = coverColor != null ? coverColor : "#B66F65";
        this.coverImageUrl = coverImageUrl;
        this.fontFamily = fontFamily != null ? fontFamily : "Pretendard";
        this.layoutPreset = layoutPreset != null ? layoutPreset : "editorial";
    }

    public void update(String coverTemplate, String coverColor, String coverImageUrl,
                        String fontFamily, String layoutPreset) {
        if (coverTemplate != null) this.coverTemplate = coverTemplate;
        if (coverColor != null) this.coverColor = coverColor;
        if (coverImageUrl != null) this.coverImageUrl = coverImageUrl;
        if (fontFamily != null) this.fontFamily = fontFamily;
        if (layoutPreset != null) this.layoutPreset = layoutPreset;
    }
}
```

`src/main/java/com/bsbowl/onti/domain/book/repository/BookDesignRepository.java`

```java
package com.bsbowl.onti.domain.book.repository;

import com.bsbowl.onti.domain.book.entity.BookDesign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookDesignRepository extends JpaRepository<BookDesign, String> {
    Optional<BookDesign> findByBookId(String bookId);
}
```

`src/main/java/com/bsbowl/onti/domain/book/dto/BookDesignRequest.java`

```java
package com.bsbowl.onti.domain.book.dto;

public record BookDesignRequest(String coverTemplate, String coverColor, String coverImageUrl,
                                 String fontFamily, String layoutPreset) {
}
```

`src/main/java/com/bsbowl/onti/domain/book/dto/BookDesignResponse.java`

```java
package com.bsbowl.onti.domain.book.dto;

import com.bsbowl.onti.domain.book.entity.BookDesign;

public record BookDesignResponse(String id, String coverTemplate, String coverColor, String coverImageUrl,
                                  String fontFamily, String layoutPreset, String pdfUrl) {
    public static BookDesignResponse from(BookDesign design) {
        return new BookDesignResponse(design.getId(), design.getCoverTemplate(), design.getCoverColor(),
                design.getCoverImageUrl(), design.getFontFamily(), design.getLayoutPreset(), design.getPdfUrl());
    }
}
```

- [ ] **Step 2: 실패하는 BookDesignServiceTest 작성**

`src/test/java/com/bsbowl/onti/domain/book/service/BookDesignServiceTest.java`

```java
package com.bsbowl.onti.domain.book.service;

import com.bsbowl.onti.domain.book.dto.BookDesignRequest;
import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.entity.BookDesign;
import com.bsbowl.onti.domain.book.repository.BookDesignRepository;
import com.bsbowl.onti.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookDesignServiceTest {

    @Mock
    private BookDesignRepository bookDesignRepository;
    @Mock
    private BookService bookService;
    @InjectMocks
    private BookDesignService bookDesignService;

    @Test
    void upsert_createsDesignWhenAbsent() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(bookDesignRepository.findByBookId("book-1")).thenReturn(Optional.empty());
        when(bookDesignRepository.save(any(BookDesign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookDesignService.upsert("book-1", "user-1",
                new BookDesignRequest("classic", "#111111", null, "Pretendard", "editorial"));

        assertThat(response.coverTemplate()).isEqualTo("classic");
        assertThat(response.coverColor()).isEqualTo("#111111");
    }

    @Test
    void upsert_updatesExistingDesign() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        BookDesign existing = BookDesign.builder().book(book).build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(bookDesignRepository.findByBookId("book-1")).thenReturn(Optional.of(existing));

        var response = bookDesignService.upsert("book-1", "user-1",
                new BookDesignRequest(null, "#222222", null, null, null));

        assertThat(response.coverColor()).isEqualTo("#222222");
        assertThat(response.coverTemplate()).isEqualTo("default");
    }
}
```

- [ ] **Step 3: 테스트 실패 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.book.service.BookDesignServiceTest"`
Expected: FAIL (컴파일 에러 — `BookDesignService` 클래스가 아직 없음)

- [ ] **Step 4: BookDesignService 구현**

`src/main/java/com/bsbowl/onti/domain/book/service/BookDesignService.java`

```java
package com.bsbowl.onti.domain.book.service;

import com.bsbowl.onti.domain.book.dto.BookDesignRequest;
import com.bsbowl.onti.domain.book.dto.BookDesignResponse;
import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.entity.BookDesign;
import com.bsbowl.onti.domain.book.repository.BookDesignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookDesignService {

    private final BookDesignRepository bookDesignRepository;
    private final BookService bookService;

    public BookDesignService(BookDesignRepository bookDesignRepository, BookService bookService) {
        this.bookDesignRepository = bookDesignRepository;
        this.bookService = bookService;
    }

    public BookDesignResponse get(String bookId, String userId) {
        Book book = bookService.getOwnedBook(bookId, userId);
        BookDesign design = bookDesignRepository.findByBookId(bookId)
                .orElseGet(() -> BookDesign.builder().book(book).build());
        return BookDesignResponse.from(design);
    }

    @Transactional
    public BookDesignResponse upsert(String bookId, String userId, BookDesignRequest request) {
        Book book = bookService.getOwnedBook(bookId, userId);
        BookDesign design = bookDesignRepository.findByBookId(bookId)
                .orElseGet(() -> bookDesignRepository.save(BookDesign.builder().book(book).build()));
        design.update(request.coverTemplate(), request.coverColor(), request.coverImageUrl(),
                request.fontFamily(), request.layoutPreset());
        return BookDesignResponse.from(design);
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.book.service.BookDesignServiceTest"`
Expected: `BUILD SUCCESSFUL`, 2개 테스트 통과

- [ ] **Step 6: BookDesignController 구현**

`src/main/java/com/bsbowl/onti/domain/book/controller/BookDesignController.java`

```java
package com.bsbowl.onti.domain.book.controller;

import com.bsbowl.onti.domain.book.dto.BookDesignRequest;
import com.bsbowl.onti.domain.book.dto.BookDesignResponse;
import com.bsbowl.onti.domain.book.service.BookDesignService;
import com.bsbowl.onti.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books/{bookId}/design")
public class BookDesignController {

    private final BookDesignService bookDesignService;

    public BookDesignController(BookDesignService bookDesignService) {
        this.bookDesignService = bookDesignService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<BookDesignResponse>> get(@AuthenticationPrincipal String userId,
                                                                 @PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(bookDesignService.get(bookId, userId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<BookDesignResponse>> upsert(@AuthenticationPrincipal String userId,
                                                                    @PathVariable String bookId,
                                                                    @RequestBody BookDesignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bookDesignService.upsert(bookId, userId, request)));
    }
}
```

- [ ] **Step 7: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: BookDesign 조회/upsert API 구현"
```

---

### Task 7: Chapter + Section 도메인

**Files:**
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/entity/Chapter.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/entity/Section.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/entity/SectionStatus.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/repository/ChapterRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/repository/SectionRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/dto/ChapterCreateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/dto/ChapterUpdateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/dto/ChapterResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/dto/SectionCreateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/dto/SectionUpdateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/dto/SectionResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/service/ChapterService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/service/SectionService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/controller/ChapterController.java`
- Create: `src/main/java/com/bsbowl/onti/domain/chapter/controller/SectionController.java`
- Test: `src/test/java/com/bsbowl/onti/domain/chapter/service/ChapterServiceTest.java`
- Test: `src/test/java/com/bsbowl/onti/domain/chapter/service/SectionServiceTest.java`

**Interfaces:**
- Consumes: `BookService.getOwnedBook(String, String): Book`(Task 5)
- Produces: `Chapter` 엔티티(양방향 컬렉션 없음, `Record`는 이 엔티티를 참조하지 않는다), `ChapterService.getOwnedChapter(String chapterId, String userId): Chapter`(Task 8 SectionService/RecordService가 재사용), `POST/GET /api/books/{bookId}/chapters`, `PATCH/DELETE /api/chapters/{chapterId}`, `POST/GET /api/chapters/{chapterId}/sections`, `PATCH/DELETE /api/sections/{sectionId}`

- [ ] **Step 1: SectionStatus, Chapter, Section 엔티티 작성**

`src/main/java/com/bsbowl/onti/domain/chapter/entity/SectionStatus.java`

```java
package com.bsbowl.onti.domain.chapter.entity;

public enum SectionStatus {
    EMPTY, DRAFTING, REVIEWED
}
```

`src/main/java/com/bsbowl/onti/domain/chapter/entity/Chapter.java`

```java
package com.bsbowl.onti.domain.chapter.entity;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chapters")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chapter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int order;

    @Builder
    private Chapter(Book book, String title, int order) {
        this.book = book;
        this.title = title;
        this.order = order;
    }

    public void update(String title, Integer order) {
        if (title != null) this.title = title;
        if (order != null) this.order = order;
    }
}
```

`src/main/java/com/bsbowl/onti/domain/chapter/entity/Section.java`

```java
package com.bsbowl.onti.domain.chapter.entity;

import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "sections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Section extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    private String title;

    @Lob
    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectionStatus status;

    @Column(nullable = false)
    private int order;

    @Builder
    private Section(Chapter chapter, String title, int order) {
        this.chapter = chapter;
        this.title = title;
        this.body = "";
        this.status = SectionStatus.EMPTY;
        this.order = order;
    }

    public void update(String title, String body, SectionStatus status, Integer order) {
        if (title != null) this.title = title;
        if (body != null) {
            this.body = body;
            if (this.status == SectionStatus.EMPTY) {
                this.status = SectionStatus.DRAFTING;
            }
        }
        if (status != null) this.status = status;
        if (order != null) this.order = order;
    }
}
```

- [ ] **Step 2: Repository, DTO 작성**

`src/main/java/com/bsbowl/onti/domain/chapter/repository/ChapterRepository.java`

```java
package com.bsbowl.onti.domain.chapter.repository;

import com.bsbowl.onti.domain.chapter.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, String> {
    List<Chapter> findAllByBookIdOrderByOrderAsc(String bookId);
}
```

`src/main/java/com/bsbowl/onti/domain/chapter/repository/SectionRepository.java`

```java
package com.bsbowl.onti.domain.chapter.repository;

import com.bsbowl.onti.domain.chapter.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, String> {
    List<Section> findAllByChapterIdOrderByOrderAsc(String chapterId);
}
```

`src/main/java/com/bsbowl/onti/domain/chapter/dto/ChapterCreateRequest.java`

```java
package com.bsbowl.onti.domain.chapter.dto;

import jakarta.validation.constraints.NotBlank;

public record ChapterCreateRequest(@NotBlank String title) {
}
```

`src/main/java/com/bsbowl/onti/domain/chapter/dto/ChapterUpdateRequest.java`

```java
package com.bsbowl.onti.domain.chapter.dto;

public record ChapterUpdateRequest(String title, Integer order) {
}
```

`src/main/java/com/bsbowl/onti/domain/chapter/dto/ChapterResponse.java`

```java
package com.bsbowl.onti.domain.chapter.dto;

import com.bsbowl.onti.domain.chapter.entity.Chapter;

public record ChapterResponse(String id, String bookId, String title, int order) {
    public static ChapterResponse from(Chapter chapter) {
        return new ChapterResponse(chapter.getId(), chapter.getBook().getId(), chapter.getTitle(), chapter.getOrder());
    }
}
```

`src/main/java/com/bsbowl/onti/domain/chapter/dto/SectionCreateRequest.java`

```java
package com.bsbowl.onti.domain.chapter.dto;

public record SectionCreateRequest(String title) {
}
```

`src/main/java/com/bsbowl/onti/domain/chapter/dto/SectionUpdateRequest.java`

```java
package com.bsbowl.onti.domain.chapter.dto;

import com.bsbowl.onti.domain.chapter.entity.SectionStatus;

public record SectionUpdateRequest(String title, String body, SectionStatus status, Integer order) {
}
```

`src/main/java/com/bsbowl/onti/domain/chapter/dto/SectionResponse.java`

```java
package com.bsbowl.onti.domain.chapter.dto;

import com.bsbowl.onti.domain.chapter.entity.Section;
import com.bsbowl.onti.domain.chapter.entity.SectionStatus;

public record SectionResponse(String id, String chapterId, String title, String body, SectionStatus status, int order) {
    public static SectionResponse from(Section section) {
        return new SectionResponse(section.getId(), section.getChapter().getId(), section.getTitle(),
                section.getBody(), section.getStatus(), section.getOrder());
    }
}
```

- [ ] **Step 3: 실패하는 ChapterServiceTest, SectionServiceTest 작성**

`src/test/java/com/bsbowl/onti/domain/chapter/service/ChapterServiceTest.java`

```java
package com.bsbowl.onti.domain.chapter.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.dto.ChapterCreateRequest;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private BookService bookService;
    @InjectMocks
    private ChapterService chapterService;

    @Test
    void create_assignsNextOrder() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(chapterRepository.findAllByBookIdOrderByOrderAsc("book-1")).thenReturn(Collections.emptyList());
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = chapterService.create("book-1", "user-1", new ChapterCreateRequest("1장"));

        assertThat(response.order()).isEqualTo(0);
        assertThat(response.title()).isEqualTo("1장");
    }

    @Test
    void getOwnedChapter_notFound_throwsChapterNotFound() {
        when(chapterRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.getOwnedChapter("missing", "user-1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAPTER_NOT_FOUND);
    }
}
```

`src/test/java/com/bsbowl/onti/domain/chapter/service/SectionServiceTest.java`

```java
package com.bsbowl.onti.domain.chapter.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.chapter.dto.SectionCreateRequest;
import com.bsbowl.onti.domain.chapter.dto.SectionUpdateRequest;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.entity.Section;
import com.bsbowl.onti.domain.chapter.entity.SectionStatus;
import com.bsbowl.onti.domain.chapter.repository.SectionRepository;
import com.bsbowl.onti.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private ChapterService chapterService;
    @InjectMocks
    private SectionService sectionService;

    @Test
    void create_assignsNextOrder() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        Chapter chapter = Chapter.builder().book(book).title("1장").order(0).build();
        when(chapterService.getOwnedChapter("chapter-1", "user-1")).thenReturn(chapter);
        when(sectionRepository.findAllByChapterIdOrderByOrderAsc("chapter-1")).thenReturn(Collections.emptyList());
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = sectionService.create("chapter-1", "user-1", new SectionCreateRequest("소제목"));

        assertThat(response.order()).isEqualTo(0);
        assertThat(response.status()).isEqualTo(SectionStatus.EMPTY);
    }

    @Test
    void update_settingBody_movesStatusFromEmptyToDrafting() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        Chapter chapter = Chapter.builder().book(book).title("1장").order(0).build();
        Section section = Section.builder().chapter(chapter).title("소제목").order(0).build();
        when(sectionRepository.findById("section-1")).thenReturn(Optional.of(section));
        when(chapterService.getOwnedChapter(any(), any())).thenReturn(chapter);

        var response = sectionService.update("section-1", "user-1",
                new SectionUpdateRequest(null, "본문 내용", null, null));

        assertThat(response.status()).isEqualTo(SectionStatus.DRAFTING);
        assertThat(response.body()).isEqualTo("본문 내용");
    }
}
```

- [ ] **Step 4: 테스트 실패 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.chapter.service.*"`
Expected: FAIL (컴파일 에러 — `ChapterService`/`SectionService` 클래스가 아직 없음)

- [ ] **Step 5: ChapterService, SectionService 구현**

`src/main/java/com/bsbowl/onti/domain/chapter/service/ChapterService.java`

```java
package com.bsbowl.onti.domain.chapter.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.dto.ChapterCreateRequest;
import com.bsbowl.onti.domain.chapter.dto.ChapterResponse;
import com.bsbowl.onti.domain.chapter.dto.ChapterUpdateRequest;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final BookService bookService;

    public ChapterService(ChapterRepository chapterRepository, BookService bookService) {
        this.chapterRepository = chapterRepository;
        this.bookService = bookService;
    }

    @Transactional
    public ChapterResponse create(String bookId, String userId, ChapterCreateRequest request) {
        Book book = bookService.getOwnedBook(bookId, userId);
        int nextOrder = chapterRepository.findAllByBookIdOrderByOrderAsc(bookId).size();
        Chapter chapter = Chapter.builder().book(book).title(request.title()).order(nextOrder).build();
        return ChapterResponse.from(chapterRepository.save(chapter));
    }

    public List<ChapterResponse> list(String bookId, String userId) {
        bookService.getOwnedBook(bookId, userId);
        return chapterRepository.findAllByBookIdOrderByOrderAsc(bookId).stream()
                .map(ChapterResponse::from)
                .toList();
    }

    @Transactional
    public ChapterResponse update(String chapterId, String userId, ChapterUpdateRequest request) {
        Chapter chapter = getOwnedChapter(chapterId, userId);
        chapter.update(request.title(), request.order());
        return ChapterResponse.from(chapter);
    }

    @Transactional
    public void delete(String chapterId, String userId) {
        chapterRepository.delete(getOwnedChapter(chapterId, userId));
    }

    public Chapter getOwnedChapter(String chapterId, String userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));
        bookService.getOwnedBook(chapter.getBook().getId(), userId);
        return chapter;
    }
}
```

`src/main/java/com/bsbowl/onti/domain/chapter/service/SectionService.java`

```java
package com.bsbowl.onti.domain.chapter.service;

import com.bsbowl.onti.domain.chapter.dto.SectionCreateRequest;
import com.bsbowl.onti.domain.chapter.dto.SectionResponse;
import com.bsbowl.onti.domain.chapter.dto.SectionUpdateRequest;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.entity.Section;
import com.bsbowl.onti.domain.chapter.repository.SectionRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SectionService {

    private final SectionRepository sectionRepository;
    private final ChapterService chapterService;

    public SectionService(SectionRepository sectionRepository, ChapterService chapterService) {
        this.sectionRepository = sectionRepository;
        this.chapterService = chapterService;
    }

    @Transactional
    public SectionResponse create(String chapterId, String userId, SectionCreateRequest request) {
        Chapter chapter = chapterService.getOwnedChapter(chapterId, userId);
        int nextOrder = sectionRepository.findAllByChapterIdOrderByOrderAsc(chapterId).size();
        Section section = Section.builder().chapter(chapter).title(request.title()).order(nextOrder).build();
        return SectionResponse.from(sectionRepository.save(section));
    }

    public List<SectionResponse> list(String chapterId, String userId) {
        chapterService.getOwnedChapter(chapterId, userId);
        return sectionRepository.findAllByChapterIdOrderByOrderAsc(chapterId).stream()
                .map(SectionResponse::from)
                .toList();
    }

    @Transactional
    public SectionResponse update(String sectionId, String userId, SectionUpdateRequest request) {
        Section section = getOwnedSection(sectionId, userId);
        section.update(request.title(), request.body(), request.status(), request.order());
        return SectionResponse.from(section);
    }

    @Transactional
    public void delete(String sectionId, String userId) {
        sectionRepository.delete(getOwnedSection(sectionId, userId));
    }

    private Section getOwnedSection(String sectionId, String userId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SECTION_NOT_FOUND));
        chapterService.getOwnedChapter(section.getChapter().getId(), userId);
        return section;
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.chapter.service.*"`
Expected: `BUILD SUCCESSFUL`, 4개 테스트 통과

- [ ] **Step 7: ChapterController, SectionController 구현**

`src/main/java/com/bsbowl/onti/domain/chapter/controller/ChapterController.java`

```java
package com.bsbowl.onti.domain.chapter.controller;

import com.bsbowl.onti.domain.chapter.dto.ChapterCreateRequest;
import com.bsbowl.onti.domain.chapter.dto.ChapterResponse;
import com.bsbowl.onti.domain.chapter.dto.ChapterUpdateRequest;
import com.bsbowl.onti.domain.chapter.service.ChapterService;
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
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping("/api/books/{bookId}/chapters")
    public ResponseEntity<ApiResponse<ChapterResponse>> create(@AuthenticationPrincipal String userId,
                                                                 @PathVariable String bookId,
                                                                 @Valid @RequestBody ChapterCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chapterService.create(bookId, userId, request)));
    }

    @GetMapping("/api/books/{bookId}/chapters")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> list(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(chapterService.list(bookId, userId)));
    }

    @PatchMapping("/api/chapters/{chapterId}")
    public ResponseEntity<ApiResponse<ChapterResponse>> update(@AuthenticationPrincipal String userId,
                                                                 @PathVariable String chapterId,
                                                                 @RequestBody ChapterUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chapterService.update(chapterId, userId, request)));
    }

    @DeleteMapping("/api/chapters/{chapterId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String chapterId) {
        chapterService.delete(chapterId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
```

`src/main/java/com/bsbowl/onti/domain/chapter/controller/SectionController.java`

```java
package com.bsbowl.onti.domain.chapter.controller;

import com.bsbowl.onti.domain.chapter.dto.SectionCreateRequest;
import com.bsbowl.onti.domain.chapter.dto.SectionResponse;
import com.bsbowl.onti.domain.chapter.dto.SectionUpdateRequest;
import com.bsbowl.onti.domain.chapter.service.SectionService;
import com.bsbowl.onti.global.common.ApiResponse;
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
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @PostMapping("/api/chapters/{chapterId}/sections")
    public ResponseEntity<ApiResponse<SectionResponse>> create(@AuthenticationPrincipal String userId,
                                                                 @PathVariable String chapterId,
                                                                 @RequestBody SectionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.create(chapterId, userId, request)));
    }

    @GetMapping("/api/chapters/{chapterId}/sections")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> list(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String chapterId) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.list(chapterId, userId)));
    }

    @PatchMapping("/api/sections/{sectionId}")
    public ResponseEntity<ApiResponse<SectionResponse>> update(@AuthenticationPrincipal String userId,
                                                                 @PathVariable String sectionId,
                                                                 @RequestBody SectionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.update(sectionId, userId, request)));
    }

    @DeleteMapping("/api/sections/{sectionId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String sectionId) {
        sectionService.delete(sectionId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
```

- [ ] **Step 8: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: Chapter/Section CRUD API 구현"
```

---

### Task 8: Record 도메인

**Files:**
- Create: `src/main/java/com/bsbowl/onti/domain/record/entity/RecordType.java`
- Create: `src/main/java/com/bsbowl/onti/domain/record/entity/Record.java`
- Create: `src/main/java/com/bsbowl/onti/domain/record/repository/RecordRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/record/dto/RecordCreateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/record/dto/RecordUpdateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/record/dto/RecordResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/record/service/RecordService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/record/controller/RecordController.java`
- Test: `src/test/java/com/bsbowl/onti/domain/record/service/RecordServiceTest.java`

**Interfaces:**
- Consumes: `BookService.getOwnedBook(String, String): Book`(Task 5), `ChapterRepository`(Task 7, 챕터 연결용)
- Produces: `POST/GET /api/books/{bookId}/records`, `PATCH/DELETE /api/records/{recordId}` (챕터 연결/해제는 `chapterId`/`unlinkChapter` 필드로)

- [ ] **Step 1: RecordType, Record 엔티티 작성**

`src/main/java/com/bsbowl/onti/domain/record/entity/RecordType.java`

```java
package com.bsbowl.onti.domain.record.entity;

public enum RecordType {
    MEMO, PHOTO, TEXT
}
```

`src/main/java/com/bsbowl/onti/domain/record/entity/Record.java`

```java
package com.bsbowl.onti.domain.record.entity;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Record extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordType type;

    @Column(length = 4000)
    private String content;

    private String mediaUrl;

    private String memo;

    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private int order;

    @Builder
    private Record(Book book, RecordType type, String content, String mediaUrl, String memo,
                    LocalDateTime recordedAt, int order) {
        this.book = book;
        this.type = type;
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.memo = memo;
        this.recordedAt = recordedAt;
        this.order = order;
    }

    public void update(String content, String mediaUrl, String memo, LocalDateTime recordedAt, Integer order) {
        if (content != null) this.content = content;
        if (mediaUrl != null) this.mediaUrl = mediaUrl;
        if (memo != null) this.memo = memo;
        if (recordedAt != null) this.recordedAt = recordedAt;
        if (order != null) this.order = order;
    }

    public void linkChapter(Chapter chapter) {
        this.chapter = chapter;
    }
}
```

- [ ] **Step 2: Repository, DTO 작성**

`src/main/java/com/bsbowl/onti/domain/record/repository/RecordRepository.java`

```java
package com.bsbowl.onti.domain.record.repository;

import com.bsbowl.onti.domain.record.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordRepository extends JpaRepository<Record, String> {
    List<Record> findAllByBookIdOrderByOrderAsc(String bookId);
}
```

`src/main/java/com/bsbowl/onti/domain/record/dto/RecordCreateRequest.java`

```java
package com.bsbowl.onti.domain.record.dto;

import com.bsbowl.onti.domain.record.entity.RecordType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RecordCreateRequest(@NotNull RecordType type, String content, String mediaUrl, String memo,
                                   LocalDateTime recordedAt) {
}
```

`src/main/java/com/bsbowl/onti/domain/record/dto/RecordUpdateRequest.java`

```java
package com.bsbowl.onti.domain.record.dto;

import java.time.LocalDateTime;

public record RecordUpdateRequest(String content, String mediaUrl, String memo, LocalDateTime recordedAt,
                                   Integer order, String chapterId, boolean unlinkChapter) {
}
```

`src/main/java/com/bsbowl/onti/domain/record/dto/RecordResponse.java`

```java
package com.bsbowl.onti.domain.record.dto;

import com.bsbowl.onti.domain.record.entity.Record;
import com.bsbowl.onti.domain.record.entity.RecordType;

import java.time.LocalDateTime;

public record RecordResponse(String id, String bookId, String chapterId, RecordType type, String content,
                              String mediaUrl, String memo, LocalDateTime recordedAt, int order) {
    public static RecordResponse from(Record record) {
        return new RecordResponse(
                record.getId(),
                record.getBook().getId(),
                record.getChapter() != null ? record.getChapter().getId() : null,
                record.getType(),
                record.getContent(),
                record.getMediaUrl(),
                record.getMemo(),
                record.getRecordedAt(),
                record.getOrder()
        );
    }
}
```

- [ ] **Step 3: 실패하는 RecordServiceTest 작성**

`src/test/java/com/bsbowl/onti/domain/record/service/RecordServiceTest.java`

```java
package com.bsbowl.onti.domain.record.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
import com.bsbowl.onti.domain.record.dto.RecordCreateRequest;
import com.bsbowl.onti.domain.record.dto.RecordUpdateRequest;
import com.bsbowl.onti.domain.record.entity.Record;
import com.bsbowl.onti.domain.record.entity.RecordType;
import com.bsbowl.onti.domain.record.repository.RecordRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

    @Mock
    private RecordRepository recordRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private BookService bookService;
    @InjectMocks
    private RecordService recordService;

    @Test
    void create_savesRecordUnderBook() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(recordRepository.save(any(Record.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = recordService.create("book-1", "user-1",
                new RecordCreateRequest(RecordType.MEMO, "메모 내용", null, null, null));

        assertThat(response.type()).isEqualTo(RecordType.MEMO);
        assertThat(response.content()).isEqualTo("메모 내용");
    }

    @Test
    void update_notFound_throwsRecordNotFound() {
        when(recordRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.update("missing", "user-1",
                new RecordUpdateRequest(null, null, null, null, null, null, false)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECORD_NOT_FOUND);
    }

    @Test
    void update_unlinkChapter_removesChapterAssociation() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        Record record = Record.builder().book(book).type(RecordType.MEMO).content("메모").order(0).build();
        when(recordRepository.findById("record-1")).thenReturn(Optional.of(record));
        when(bookService.getOwnedBook(any(), any())).thenReturn(book);

        var response = recordService.update("record-1", "user-1",
                new RecordUpdateRequest(null, null, null, null, null, null, true));

        assertThat(response.chapterId()).isNull();
    }
}
```

- [ ] **Step 4: 테스트 실패 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.record.service.RecordServiceTest"`
Expected: FAIL (컴파일 에러 — `RecordService` 클래스가 아직 없음)

- [ ] **Step 5: RecordService 구현**

`src/main/java/com/bsbowl/onti/domain/record/service/RecordService.java`

```java
package com.bsbowl.onti.domain.record.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
import com.bsbowl.onti.domain.record.dto.RecordCreateRequest;
import com.bsbowl.onti.domain.record.dto.RecordResponse;
import com.bsbowl.onti.domain.record.dto.RecordUpdateRequest;
import com.bsbowl.onti.domain.record.entity.Record;
import com.bsbowl.onti.domain.record.repository.RecordRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RecordService {

    private final RecordRepository recordRepository;
    private final ChapterRepository chapterRepository;
    private final BookService bookService;

    public RecordService(RecordRepository recordRepository, ChapterRepository chapterRepository, BookService bookService) {
        this.recordRepository = recordRepository;
        this.chapterRepository = chapterRepository;
        this.bookService = bookService;
    }

    @Transactional
    public RecordResponse create(String bookId, String userId, RecordCreateRequest request) {
        Book book = bookService.getOwnedBook(bookId, userId);
        Record record = Record.builder()
                .book(book)
                .type(request.type())
                .content(request.content())
                .mediaUrl(request.mediaUrl())
                .memo(request.memo())
                .recordedAt(request.recordedAt())
                .order(0)
                .build();
        return RecordResponse.from(recordRepository.save(record));
    }

    public List<RecordResponse> list(String bookId, String userId) {
        bookService.getOwnedBook(bookId, userId);
        return recordRepository.findAllByBookIdOrderByOrderAsc(bookId).stream()
                .map(RecordResponse::from)
                .toList();
    }

    @Transactional
    public RecordResponse update(String recordId, String userId, RecordUpdateRequest request) {
        Record record = getOwnedRecord(recordId, userId);
        record.update(request.content(), request.mediaUrl(), request.memo(), request.recordedAt(), request.order());
        if (request.unlinkChapter()) {
            record.linkChapter(null);
        } else if (request.chapterId() != null) {
            Chapter chapter = chapterRepository.findById(request.chapterId())
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));
            record.linkChapter(chapter);
        }
        return RecordResponse.from(record);
    }

    @Transactional
    public void delete(String recordId, String userId) {
        recordRepository.delete(getOwnedRecord(recordId, userId));
    }

    private Record getOwnedRecord(String recordId, String userId) {
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        bookService.getOwnedBook(record.getBook().getId(), userId);
        return record;
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.record.service.RecordServiceTest"`
Expected: `BUILD SUCCESSFUL`, 3개 테스트 통과

- [ ] **Step 7: RecordController 구현**

`src/main/java/com/bsbowl/onti/domain/record/controller/RecordController.java`

```java
package com.bsbowl.onti.domain.record.controller;

import com.bsbowl.onti.domain.record.dto.RecordCreateRequest;
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
        return ResponseEntity.ok(ApiResponse.success());
    }
}
```

- [ ] **Step 8: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: Record CRUD 및 챕터 연결/해제 API 구현"
```

---

### Task 9: AI 스텁 도메인

**Files:**
- Create: `src/main/java/com/bsbowl/onti/domain/ai/dto/StructureSuggestionResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/ai/dto/ReviewSuggestionResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/ai/service/AiService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/ai/controller/AiController.java`

**Interfaces:**
- Produces: `POST /api/books/{bookId}/ai/structure-suggestions`, `POST /api/sections/{sectionId}/ai/review` — 둘 다 빈 결과를 반환하는 스텁. 로직 없는 고정 반환값이라 별도 테스트는 만들지 않는다 (trivial pass-through).

- [ ] **Step 1: DTO 작성**

`src/main/java/com/bsbowl/onti/domain/ai/dto/StructureSuggestionResponse.java`

```java
package com.bsbowl.onti.domain.ai.dto;

import java.util.List;

public record StructureSuggestionResponse(List<String> suggestedChapterTitles) {
}
```

`src/main/java/com/bsbowl/onti/domain/ai/dto/ReviewSuggestionResponse.java`

```java
package com.bsbowl.onti.domain.ai.dto;

import java.util.List;

public record ReviewSuggestionResponse(List<String> issues) {
}
```

- [ ] **Step 2: AiService(스텁) 작성**

`src/main/java/com/bsbowl/onti/domain/ai/service/AiService.java`

```java
package com.bsbowl.onti.domain.ai.service;

import com.bsbowl.onti.domain.ai.dto.ReviewSuggestionResponse;
import com.bsbowl.onti.domain.ai.dto.StructureSuggestionResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiService {

    // TODO: 실제 AI 모델 연동 전까지는 빈 제안 목록을 반환하는 스텁
    public StructureSuggestionResponse suggestStructure(String bookId) {
        return new StructureSuggestionResponse(List.of());
    }

    // TODO: 실제 맞춤법/중복/맥락 점검 로직 연동 필요
    public ReviewSuggestionResponse reviewSection(String sectionId) {
        return new ReviewSuggestionResponse(List.of());
    }
}
```

- [ ] **Step 3: AiController 작성**

`src/main/java/com/bsbowl/onti/domain/ai/controller/AiController.java`

```java
package com.bsbowl.onti.domain.ai.controller;

import com.bsbowl.onti.domain.ai.dto.ReviewSuggestionResponse;
import com.bsbowl.onti.domain.ai.dto.StructureSuggestionResponse;
import com.bsbowl.onti.domain.ai.service.AiService;
import com.bsbowl.onti.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/api/books/{bookId}/ai/structure-suggestions")
    public ResponseEntity<ApiResponse<StructureSuggestionResponse>> suggestStructure(@PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(aiService.suggestStructure(bookId)));
    }

    @PostMapping("/api/sections/{sectionId}/ai/review")
    public ResponseEntity<ApiResponse<ReviewSuggestionResponse>> reviewSection(@PathVariable String sectionId) {
        return ResponseEntity.ok(ApiResponse.success(aiService.reviewSection(sectionId)));
    }
}
```

- [ ] **Step 4: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: AI 제안/점검 스텁 API 추가"
```

---

### Task 10: 엔드투엔드 스모크 테스트

**Files:** 없음 (검증 전용 태스크)

**Interfaces:**
- Consumes: Task 1~9에서 만든 전체 애플리케이션

- [ ] **Step 1: 전체 단위 테스트 실행**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL`, Task 2~8에서 작성한 모든 서비스 테스트 통과 (총 17개: ApiResponseTest 2 + GlobalExceptionHandlerTest 2 + JwtTokenProviderTest 3 + AuthServiceTest 3 + BookServiceTest 3 + BookDesignServiceTest 2 + ChapterServiceTest 2 + SectionServiceTest 2 + RecordServiceTest 3, 정확한 개수는 실행 결과로 확인)

- [ ] **Step 2: PostgreSQL 기동**

Run: `docker compose up -d`
Expected: `onti-postgres` 컨테이너가 healthy 상태로 실행

- [ ] **Step 3: 서버 기동**

Run (background): `./gradlew bootRun`
Expected: 콘솔에 `Tomcat started on port 8080` 로그, 에러 없이 기동

- [ ] **Step 4: 회원가입 → 로그인 → 책 생성 → 챕터 생성 → 기록 생성 → 섹션 생성 → 디자인 조회 흐름을 curl로 확인**

```bash
# 회원가입
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke@onti.com","password":"password123","name":"스모크"}'
# 응답의 data.accessToken을 TOKEN 변수에 저장했다고 가정

TOKEN="<위 응답의 accessToken>"

# 책 생성
curl -s -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"나의 첫 책"}'
# 응답의 data.id를 BOOK_ID로 저장

BOOK_ID="<위 응답의 id>"

# 챕터 생성
curl -s -X POST "http://localhost:8080/api/books/$BOOK_ID/chapters" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"1장"}'

# 기록 생성
curl -s -X POST "http://localhost:8080/api/books/$BOOK_ID/records" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"type":"MEMO","content":"첫 기록"}'

# 디자인 조회 (기본값 반환)
curl -s "http://localhost:8080/api/books/$BOOK_ID/design" -H "Authorization: Bearer $TOKEN"

# 인증 없이 books 조회 시 401 확인
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/books
```

Expected: 각 응답이 `{"success":true,...}` 형태, 인증 없는 마지막 요청은 `401`

- [ ] **Step 5: Swagger UI 확인**

Run: `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui/index.html`
Expected: `200`

- [ ] **Step 6: 서버 종료 및 정리**

bootRun 프로세스를 종료한다 (백그라운드 실행 방식에 맞게 kill). `docker compose down`은 로컬 DB를 계속 쓸 것이므로 실행하지 않는다.

- [ ] **Step 7: 최종 커밋 (필요 시)**

스모크 테스트 중 발견된 문제를 수정했다면 커밋한다. 문제가 없었다면 이 태스크는 커밋 없이 종료.
