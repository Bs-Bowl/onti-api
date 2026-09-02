# ONTI API

> 당신의 이야기를 ON.

흩어진 기록과 생각이 연결되고 정리되어 한 권의 이야기로 완성되는
기록·출판 플랫폼 ONTI의 백엔드 API 서버입니다.

## 왜 만드나

기록하는 일은 어렵지 않지만, 쌓인 기록을 정리해 하나의 결과물로
완성하는 일은 늘 막막합니다. ONTI는 그 과정을 함께 정리하고
완성까지 이끕니다.

## 서비스 흐름

기록 모으기 → 구조 만들기 → 집필하기 → 점검하기 → 책 디자인 → 완성

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

## 컨벤션

브랜치, 커밋, 코드 컨벤션은 [CLAUDE.md](./CLAUDE.md)를 참고하세요.