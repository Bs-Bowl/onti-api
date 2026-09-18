# ONTI 백엔드 Space 도메인 설계

- 날짜: 2026-09-18
- 대상 레포: onti-api
- 상태: 승인됨

## 배경

프론트(onti-web)가 "내 기록"/"함께 쓰기" 화면을 새 데이터 모델(RecordSpace/
Participant/SpaceQuestion/SpaceRecord/BookRecordLink)로 이미 개편해서 배포했다.
기존 백엔드의 `Record`는 여전히 워크스페이스 6단계(RECORDS~DESIGN)가 쓰는
책-1:1 소속 모델이고, 프론트 자신도 "아직 통합 전 단계"라고 명시하고 있다.
사용자(백엔드 담당)가 프론트 팀원과 협의한 결과, 이번 라운드는 아래 화면을
연동 가능하게 만드는 것이 목표다: **내 기록(Space), 함께 쓰기, 내 책(이미 완료),
계정 설정, 로그인(이미 완료)**. 둘러보기/커뮤니티는 UI 재작업 중이라 제외.

## 범위 (이번 라운드)

- `RecordSpace` (기록 공간) CRUD — kind(PERSONAL/COLLABORATIVE)로 "함께 쓰기"까지
  같은 엔티티가 처리. 전용 "함께 쓰기" API는 만들지 않는다.
- `Participant` (공간 참여자) CRUD — 소유자가 명단을 직접 관리하는 형태.
  실제 이메일 초대 발송/가입 연동은 범위 밖.
- `SpaceQuestion` (질문) CRUD
- `SpaceRecord` (공간 소속 기록, title + 여러 장 사진) CRUD + 이미지
  추가/삭제 하위 리소스
- `BookRecordLink` — `SpaceRecord`를 여러 `Book`(+선택적으로 `Chapter`)에
  연결/해제하는 M:N 매핑
- 계정 설정: 프로필 수정(name/avatarUrl/bio), 비밀번호 변경, 회원 탈퇴(cascade)

## 범위 밖

- 기존 `Record`/`Chapter`/`Section` 도메인과의 통합 — 프론트가 아직 안 했으므로
  백엔드도 지금 하지 않는다
- 실제 이메일 발송 기반 초대/가입 플로우, `Participant.linkedUserId` 실제 연결
- 둘러보기(공개 게시)/커뮤니티(글·댓글)/고객지원(FAQ 등) — 이번엔 제외
- 이미지 실제 업로드 — 기존 Record와 동일하게 URL 문자열만 받는다
- refresh token, 이메일 변경

## 엔티티 설계

프론트 코드(`recordSpaces.ts`, `spaceRecords.ts`)에서 실제 뽑아낸 필드를
기준으로 하되, id는 기존 패턴대로 UUID(String)로 발급한다 (프론트와 DB를
공유하지 않으므로 포맷만 맞춘다).

### RecordSpace

| 필드 | 타입 | 비고 |
|---|---|---|
| owner | User (N:1) | |
| title | String | 필수 |
| topic | String | nullable |
| description | String | nullable |
| subjectName | String | nullable, "이 공간이 누구의 삶을 다루는지" |
| kind | enum PERSONAL/COLLABORATIVE | 생성 시 항상 PERSONAL (프론트가 생성 시 kind를 안 받음), PATCH로 변경 가능하게 열어둠 |
| visibility | enum PRIVATE/SPACE/PUBLIC | 기본 PRIVATE |

### Participant

| 필드 | 타입 | 비고 |
|---|---|---|
| space | RecordSpace (N:1) | |
| displayName | String | |
| email | String | |
| role | enum OWNER/PARTICIPANT/VIEWER | |
| status | enum PENDING/JOINED | 기본 PENDING |
| joinedAt | LocalDateTime | nullable |
| photoUrl | String | nullable, URL 문자열만 |

### SpaceQuestion

| 필드 | 타입 | 비고 |
|---|---|---|
| space | RecordSpace (N:1) | |
| text | String | |
| source | enum ONTI/CUSTOM | |
| createdBy | Participant (N:1) | |
| sentToParticipantIds | List\<String\> (`@ElementCollection`) | 비어있으면 공간 전체 공개 |

### SpaceRecord

| 필드 | 타입 | 비고 |
|---|---|---|
| space | RecordSpace (N:1) | |
| type | 기존 `domain.record.entity.RecordType` 재사용 (MEMO/PHOTO/TEXT) | 새 enum 안 만듦 |
| title | String | 기존 Record와 달리 title 있음 |
| content | String | |
| author | Participant (N:1, optional) | nullable |
| answeredQuestion | SpaceQuestion (N:1, optional) | nullable |
| visibility | enum PRIVATE/SPACE/PUBLIC (RecordSpace와 같은 enum 재사용) | |
| occurredAt | String | 자유 문자열 허용 ("1998년 봄" 등), LocalDateTime 아님 |

### SpaceRecordImage (자식 테이블, 여러 장 지원)

| 필드 | 타입 | 비고 |
|---|---|---|
| spaceRecord | SpaceRecord (N:1) | |
| url | String | URL 문자열만, 업로드 API 없음 |
| caption | String | nullable |
| order | int, 컬럼명 `sort_order` | Task 10에서 겪은 PostgreSQL 예약어 버그를 처음부터 피함 |

### BookRecordLink

| 필드 | 타입 | 비고 |
|---|---|---|
| book | Book (N:1) | |
| spaceRecord | SpaceRecord (N:1) | |
| chapter | Chapter (N:1, optional) | nullable |
| order | int, 컬럼명 `sort_order` | |

`(book_id, record_id)` 유니크 제약 — 같은 기록을 같은 책에 두 번 링크할 수 없음.

### User 확장

기존 `User` 엔티티에 `bio` 필드만 추가 (name=닉네임, avatarUrl=프로필사진은
이미 있음, 그대로 재사용).

## 패키지 구조

```
domain
└── space
    ├── entity      # RecordSpace, SpaceKind, Participant, ParticipantRole,
    │                 ParticipantStatus, SpaceQuestion, QuestionSource,
    │                 SpaceRecord, SpaceRecordImage, SpaceVisibility,
    │                 BookRecordLink
    ├── repository
    ├── dto
    ├── service
    └── controller
```

`SpaceVisibility`는 `RecordSpace`와 `SpaceRecord`가 공유하므로 `space.entity`
패키지에 둔다. `BookRecordLink`는 `book`과 `record`(space) 양쪽을 참조하므로
`space` 패키지에 둔다 (Book/Chapter는 참조만, 소유하지 않음).

User 확장(`bio` 필드, 프로필/비밀번호/탈퇴 API)은 기존 `domain.user` 패키지에
`UserController`/`UserService`를 새로 추가해 담당한다 (`AuthController`는
signup/login/me 읽기 전용으로 그대로 둠 — 역할 분리).

## 소유권 검증

기존 패턴(`BookService.getOwnedBook`, `ChapterService.getOwnedChapter`)을
그대로 따른다.

- `SpaceService.getOwnedSpace(spaceId, userId)`: `SPACE_NOT_FOUND` → 소유자
  아니면 `FORBIDDEN`. Participant/SpaceQuestion/SpaceRecord 서비스가 전부
  이걸 거쳐 공간 소유권을 확인한다.
- `SpaceRecordService.getOwnedSpaceRecord(recordId, userId)`:
  `SPACE_RECORD_NOT_FOUND` → `spaceService.getOwnedSpace(record.getSpace().getId(), userId)`.
- `BookRecordLink` 생성 시 **양쪽 소유권을 모두 검증**한다 — `bookService.getOwnedBook(bookId, userId)`
  AND `spaceRecordService.getOwnedSpaceRecord(spaceRecordId, userId)`. Task 8에서
  Record↔Chapter 교차 소유권 검증을 빠뜨렸던 것과 같은 실수를 반복하지 않기
  위해 설계 단계에서 명시한다.

## API 응답 포맷

기존과 동일: `{ success, data, error }`, camelCase, 에러 메시지 한국어.
새 `ErrorCode`: `SPACE_NOT_FOUND`, `PARTICIPANT_NOT_FOUND`,
`SPACE_QUESTION_NOT_FOUND`, `SPACE_RECORD_NOT_FOUND`,
`SPACE_RECORD_IMAGE_NOT_FOUND`, `BOOK_RECORD_LINK_NOT_FOUND`,
`INVALID_CURRENT_PASSWORD`(비밀번호 변경 시 현재 비밀번호 불일치).

## API 엔드포인트

| 도메인 | 엔드포인트 |
|---|---|
| Space | `POST/GET /api/spaces`, `GET/PATCH/DELETE /api/spaces/{spaceId}` |
| Participant | `POST/GET /api/spaces/{spaceId}/participants`, `PATCH/DELETE /api/participants/{participantId}` |
| SpaceQuestion | `POST/GET /api/spaces/{spaceId}/questions`, `PATCH/DELETE /api/questions/{questionId}` |
| SpaceRecord | `POST/GET /api/spaces/{spaceId}/records`, `GET/PATCH/DELETE /api/space-records/{recordId}` |
| SpaceRecordImage | `POST /api/space-records/{recordId}/images`, `DELETE /api/space-records/{recordId}/images/{imageId}` |
| BookRecordLink | `POST /api/books/{bookId}/record-links`, `GET /api/books/{bookId}/record-links`, `DELETE /api/record-links/{linkId}` |
| User 프로필 | `PATCH /api/users/me` (name/avatarUrl/bio), `PATCH /api/users/me/password` (currentPassword/newPassword), `DELETE /api/users/me` |

`/api/space-records/*`는 기존 `/api/records/*`(워크스페이스용 Record)와
경로가 겹치지 않도록 의도적으로 분리한 이름이다.

## 삭제 cascade

Task 10에서 도입한 `@OnDelete` 패턴을 그대로 따른다.

- `RecordSpace` 삭제 → `Participant`/`SpaceQuestion`/`SpaceRecord` CASCADE
- `SpaceRecord` 삭제 → `SpaceRecordImage` CASCADE, `BookRecordLink` CASCADE
- `SpaceQuestion` 삭제 → `SpaceRecord.answeredQuestion` SET_NULL (질문이
  없어져도 기록 자체는 남아야 함)
- `Book`/`Chapter` 삭제 → `BookRecordLink` CASCADE. `@OnDelete`는 항상
  FK를 들고 있는 자식 쪽(`BookRecordLink.book`/`BookRecordLink.chapter`)에
  붙이므로, 기존 `Book`/`Chapter` 엔티티는 수정할 필요가 없다 — `BookRecordLink`를
  새로 만들 때 그 두 필드에만 붙이면 된다.
- `User` 탈퇴 → 소유한 `Book`, `RecordSpace` 모두 CASCADE. 이전 라운드(Task 10)의
  cascade 작업은 Book/Chapter/Record/Section/BookDesign 사이의 관계만
  다뤘고 `User` → `Book`은 다루지 않았다 — 이번에 기존 `Book.java`의
  `user` 필드(`@JoinColumn(name = "user_id")`)에 `@OnDelete(action = OnDeleteAction.CASCADE)`를
  추가해야 한다 (기존 엔티티를 수정하는 유일한 지점). `User` → `RecordSpace`는
  `RecordSpace.owner` 필드에 새로 붙이면 된다.

## 테스트

기존 컨벤션 그대로: 서비스 레이어 단위 테스트(JUnit5 + Mockito), 소유권
검증(not-found/forbidden)과 핵심 로직(이미지 order 계산, cascade 대상 필드
등)을 중심으로. BookRecordLink는 Task 8 스타일로 "다른 사용자 소유 자원 연결
시도" 테스트를 반드시 포함한다.
