# ONTI API

기록·출판 플랫폼 [ONTI](https://github.com/Bs-Bowl)의 백엔드 API 서버입니다.
프로젝트 전체 소개는 [Bs-Bowl/.github](https://github.com/Bs-Bowl/.github)를 참고하세요.

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| ORM | Spring Data JPA |
| Database | PostgreSQL 16 |
| Auth | Spring Security, JWT |
| Docs | springdoc-openapi |
| Build | Gradle |

## 패키지 구조

도메인형으로 구성합니다.

com.bsbowl.onti
├── domain
│   ├── user      # 회원, 인증
│   ├── book      # 책 프로젝트
│   ├── record    # 기록 (메모/글/사진)
│   ├── chapter   # 챕터, 목차
│   └── ai        # 목차 제안, 점검
└── global
    ├── config    # CORS, Security, Swagger
    ├── exception
    └── common    # 공통 응답 포맷, BaseEntity

## 실행 방법

### 요구 사항
- JDK 21
- Docker Desktop

### 로컬 실행

# 1. 저장소 클론
git clone https://github.com/Bs-Bowl/onti-api.git
cd onti-api

# 2. 데이터베이스 기동
docker compose up -d

# 3. 서버 실행
./gradlew bootRun          # macOS / Linux
gradlew.bat bootRun        # Windows

서버: http://localhost:8080
API 문서: http://localhost:8080/swagger-ui.html

## 관련 저장소

- 프론트엔드: [onti-web](https://github.com/Bs-Bowl/onti-web)

## 팀

| 이름 | 역할 |
| --- | --- |
| jongheecode | Backend |
|  | Frontend, Design |
