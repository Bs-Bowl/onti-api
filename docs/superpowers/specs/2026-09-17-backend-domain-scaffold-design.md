# ONTI 백엔드 초기 도메인 스캐폴딩 설계

- 날짜: 2026-09-17
- 대상 레포: onti-api
- 상태: 승인됨

## 배경

onti-api는 README/CLAUDE.md/LICENSE만 있고 실제 코드(`src`, `build.gradle`)가 없는
빈 상태. 프론트(onti-web)는 랜딩부터 6단계 워크스페이스(RECORDS/STRUCTURE/WRITE/
REVIEW/DESIGN/COMPLETE)까지 화면이 거의 다 구현되어 있고, 자체 `prisma/schema.prisma`로
프로토타입 데이터 모델을 이미 정의해둔 상태. 백엔드를 이 스키마 기준으로 처음부터
전체 도메인 한 번에 만든다.

## 범위 (이번 라운드에 포함)

- Gradle/Spring Boot 프로젝트 스캐폴딩, docker-compose(PostgreSQL), 전역 설정
  (CORS, Security, Swagger, 공통 응답 포맷, 예외 처리)
- 인증: 회원가입/로그인/JWT 발급·검증까지 실제 구현 (refresh token은 제외)
- User/Book/Record/Chapter/Section/BookDesign 엔티티 + 기본 CRUD API
- AI 제안/점검 엔드포인트는 스텁만 (실제 AI 미연동, 빈 응답 + TODO)
- Record.mediaUrl, BookDesign.coverImageUrl은 URL 문자열만 받음 (파일 업로드 미구현)
- 서비스 레이어 단위 테스트 (JUnit5 + Mockito)

## 범위 밖 (이번 라운드 제외, 다음에)

- Refresh token / 로그아웃·토큰 재발급 흐름
- 실제 AI 연동 (목차 제안, 맞춤법/중복 점검)
- 실제 PDF 생성 (BookDesign.pdfUrl은 필드만 존재, 값 세팅 로직 없음)
- 파일 업로드 API (S3 등 스토리지 연동)
- order 필드용 벌크 재정렬 API (프론트가 PATCH 여러 번 호출)
- Testcontainers 등 통합 테스트 셋업

## 엔티티 설계

Prisma 스키마(`onti-web/prisma/schema.prisma`)를 JPA로 옮기되, id는 프론트의
cuid 문자열과 포맷만 맞춘 **UUID(String)**로 발급한다 (DB는 서로 다름, 공유하지 않음).

| 엔티티 | 주요 필드 | 관계 |
|---|---|---|
| User | id, email(unique), password(BCrypt), name, avatarUrl | 1:N Book |
| Book | id, title, subtitle, description, status(BookStatus enum) | N:1 User, 1:N Record/Chapter, 1:1 BookDesign |
| Record | id, type(MEMO/PHOTO/TEXT), content, mediaUrl, memo, recordedAt, order | N:1 Book, N:1 Chapter(nullable) |
| Chapter | id, title, order | N:1 Book, 1:N Record/Section |
| Section | id, title, body, status(EMPTY/DRAFTING/REVIEWED), order | N:1 Chapter |
| BookDesign | id, coverTemplate, coverColor, coverImageUrl, fontFamily, layoutPreset, pdfUrl(nullable) | 1:1 Book |

BookStatus enum: `DRAFT, RECORDING, STRUCTURING, WRITING, REVIEWING, DESIGNING, COMPLETED`
(Prisma 스키마와 동일).

CLAUDE.md 컨벤션 준수: 엔티티에 `@Setter` 금지, `@Builder` + protected 기본 생성자,
의미 있는 변경 메서드.

## 패키지 구조

CLAUDE.md에 정의된 도메인형 구조를 따르되, Section은 Chapter 도메인에,
BookDesign은 Book 도메인에 포함한다 (독립 도메인으로 분리할 만큼 크지 않음).

```
com.bsbowl.onti
├── domain
│   ├── user      # User 엔티티, 회원가입/로그인/JWT 발급
│   ├── book      # Book, BookDesign
│   ├── record    # Record
│   ├── chapter   # Chapter, Section
│   └── ai        # 스텁 컨트롤러/서비스만
└── global
    ├── config    # CORS, Security, Swagger
    ├── exception # CustomException, ErrorCode, 전역 핸들러
    └── common    # ApiResponse, BaseEntity
```

각 도메인 하위는 controller / service / repository / entity / dto.

## 인증 흐름

- `POST /api/auth/signup` — 이메일 중복 체크 후 User 생성 (비밀번호 BCrypt 해싱)
- `POST /api/auth/login` — 이메일/비밀번호 검증 후 JWT access token 발급
- `GET /api/auth/me` — 토큰으로 현재 유저 조회
- JWT 필터가 `Authorization: Bearer <token>`을 검증해 SecurityContext에 userId 세팅
- Book 이하 모든 리소스는 소유자(Book.userId) 검증 후 접근 허용, 아니면 403 FORBIDDEN

## API 엔드포인트

| 도메인 | 엔드포인트 |
|---|---|
| Auth | `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/auth/me` |
| Book | `POST /api/books`, `GET /api/books`, `GET/PATCH/DELETE /api/books/{id}` |
| Record | `POST/GET /api/books/{bookId}/records`, `PATCH/DELETE /api/records/{id}` |
| Chapter | `POST/GET /api/books/{bookId}/chapters`, `PATCH/DELETE /api/chapters/{id}` |
| Section | `POST/GET /api/chapters/{chapterId}/sections`, `PATCH/DELETE /api/sections/{id}` |
| Design | `GET/PUT /api/books/{bookId}/design` (1:1 upsert) |
| AI(스텁) | `POST /api/books/{bookId}/ai/structure-suggestions`, `POST /api/sections/{id}/ai/review` |

Record의 chapter 연결/해제는 `PATCH /api/records/{id}`의 `chapterId` 필드로 처리.
order 변경도 같은 PATCH로 개별 갱신 (벌크 API 없음).

## 응답 포맷 / 에러 코드

CLAUDE.md 포맷 유지: `{ success, data, error }`, camelCase, 에러 메시지 한국어.

추가할 `ErrorCode`:
- `BOOK_NOT_FOUND`, `RECORD_NOT_FOUND`, `CHAPTER_NOT_FOUND`, `SECTION_NOT_FOUND`
- `EMAIL_ALREADY_EXISTS`, `INVALID_CREDENTIALS`
- `UNAUTHORIZED`(토큰 없음/무효), `FORBIDDEN`(소유자 아님)

## 테스트

- 서비스 레이어 단위 테스트 (JUnit5 + Mockito): 도메인별 생성/조회/권한없음/not-found
- 엔티티는 로직 없는 매핑이라 별도 테스트 없음
- 통합 테스트(Testcontainers 등)는 범위 밖
