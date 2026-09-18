# Space 도메인(내 기록/함께 쓰기/계정 설정) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** onti-web의 "내 기록"/"함께 쓰기" 화면(RecordSpace/Participant/SpaceQuestion/SpaceRecord/BookRecordLink)과 계정 설정(프로필/비밀번호/탈퇴)을 백엔드에서 연동 가능하게 만든다. 기존 Book/Record/Chapter/Section 도메인은 건드리지 않는다.

**Architecture:** 새 `domain/space` 패키지에 5개 엔티티(RecordSpace, Participant, SpaceQuestion, SpaceRecord+SpaceRecordImage, BookRecordLink)를 CLAUDE.md 컨벤션 그대로 추가한다. 소유권 검증은 기존 `getOwnedX` 체이닝 패턴을 그대로 따르고, 다른 공간/다른 책 소속 자원을 잘못 연결하는 것을 막는 교차 검증(Task 8의 Record↔Chapter 패턴)을 신규 관계마다 명시적으로 넣는다. 삭제는 Task 10에서 도입한 `@OnDelete` DB-레벨 cascade 패턴을 그대로 따른다. 계정 설정은 기존 `domain/user` 패키지에 `UserService`/`UserController`를 추가해서 담당한다(`AuthService`/`AuthController`는 signup/login/me 읽기 전용으로 그대로 둠).

**Tech Stack:** 기존과 동일 — Java 21, Spring Boot 3.3.4, Spring Data JPA, PostgreSQL 16, JUnit5 + Mockito + AssertJ. 신규 의존성 없음.

**Spec:** `docs/superpowers/specs/2026-09-18-space-domain-design.md`

## Global Constraints

- 모든 API 응답은 `{ success, data, error }`로 감싼다. `ApiResponse`는 `success(T data)`/`error(ErrorCode)`만 있고 무인자 `success()`는 없다 — void 응답은 `ApiResponse.success(null)`.
- 엔티티에 `@Setter` 금지, 변경은 의미 있는 메서드로. 생성자는 `@Builder`, 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- Controller는 DTO만 다루고 엔티티를 노출하지 않는다.
- 예외는 `CustomException` + `ErrorCode` enum, 전역 핸들러에서 응답 변환.
- 컬럼명에 SQL 예약어(`order` 등)를 쓰지 않는다 — 처음부터 `@Column(name = "sort_order")`.
- 삭제 cascade는 FK를 들고 있는 자식 엔티티의 `@JoinColumn`에 `org.hibernate.annotations.OnDelete(action = OnDeleteAction.CASCADE|SET_NULL)`를 붙인다 (부모 쪽 엔티티는 건드리지 않는다).
- 교차 도메인 참조(다른 공간/다른 책 소속 자원을 잘못 연결하는 것 방지)는 반드시 같은 부모 소속인지 확인하고, 아니면 `NOT_FOUND` 계열을 던진다 (`FORBIDDEN`이 아님 — 다른 사용자 자원의 존재 자체를 흘리지 않기 위함).
- 사진/이미지는 URL 문자열만 받는다. 업로드 API는 만들지 않는다.
- 이번 라운드 범위 밖: 기존 Record/Chapter/Section과의 통합, 실제 이메일 발송 기반 초대, 둘러보기/커뮤니티/고객지원, refresh token.
- 커밋 메시지: `feat / fix / docs / refactor / test / chore` + 한국어 설명.

---

### Task 1: Space 도메인 기초 (RecordSpace + 신규 ErrorCode 전체)

**Files:**
- Modify: `src/main/java/com/bsbowl/onti/global/exception/ErrorCode.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/entity/SpaceKind.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/entity/SpaceVisibility.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/entity/RecordSpace.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/repository/RecordSpaceRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/SpaceCreateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/SpaceUpdateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/SpaceResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/service/SpaceService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/controller/SpaceController.java`
- Test: `src/test/java/com/bsbowl/onti/domain/space/service/SpaceServiceTest.java`

**Interfaces:**
- Consumes: `User`/`UserRepository`(기존), `BaseEntity`/`CustomException`/`ErrorCode`(기존)
- Produces: `RecordSpace` 엔티티(`getOwner()`, `isOwnedBy(userId): boolean`), `SpaceService.getOwnedSpace(String spaceId, String userId): RecordSpace` (public, `SPACE_NOT_FOUND` → `FORBIDDEN` 순서) — Task 2,3,4,5가 이 메서드를 그대로 재사용한다. 이번 태스크에서 이 플랜 전체가 쓰는 `ErrorCode` 7개를 한 번에 추가한다: `SPACE_NOT_FOUND`, `PARTICIPANT_NOT_FOUND`, `SPACE_QUESTION_NOT_FOUND`, `SPACE_RECORD_NOT_FOUND`, `SPACE_RECORD_IMAGE_NOT_FOUND`, `BOOK_RECORD_LINK_NOT_FOUND`, `INVALID_CURRENT_PASSWORD`.

- [ ] **Step 1: ErrorCode에 신규 코드 7개 추가**

`src/main/java/com/bsbowl/onti/global/exception/ErrorCode.java`의 기존 `SECTION_NOT_FOUND` 항목 바로 뒤에 추가한다 (기존 항목은 그대로 두고, 아래 7줄만 새로 넣는다):

```java
    SPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "기록 공간을 찾을 수 없습니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여자를 찾을 수 없습니다."),
    SPACE_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    SPACE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다."),
    SPACE_RECORD_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."),
    BOOK_RECORD_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "연결된 기록을 찾을 수 없습니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
```

주의: 파일의 마지막 항목은 세미콜론(`;`)으로 끝나야 한다 — 기존 `SECTION_NOT_FOUND(...)`(콤마로 끝났던 것)를 콤마로 바꾸고, 위 7개 중 마지막(`INVALID_CURRENT_PASSWORD`)에 세미콜론을 붙인다.

- [ ] **Step 2: SpaceKind, SpaceVisibility enum 작성**

`src/main/java/com/bsbowl/onti/domain/space/entity/SpaceKind.java`

```java
package com.bsbowl.onti.domain.space.entity;

public enum SpaceKind {
    PERSONAL, COLLABORATIVE
}
```

`src/main/java/com/bsbowl/onti/domain/space/entity/SpaceVisibility.java`

```java
package com.bsbowl.onti.domain.space.entity;

public enum SpaceVisibility {
    PRIVATE, SPACE, PUBLIC
}
```

- [ ] **Step 3: RecordSpace 엔티티 작성**

`src/main/java/com/bsbowl/onti/domain/space/entity/RecordSpace.java`

```java
package com.bsbowl.onti.domain.space.entity;

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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(name = "record_spaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordSpace extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User owner;

    @Column(nullable = false)
    private String title;

    private String topic;

    @Column(length = 2000)
    private String description;

    private String subjectName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceVisibility visibility;

    @Builder
    private RecordSpace(User owner, String title, String topic, String description,
                         String subjectName, SpaceVisibility visibility) {
        this.owner = owner;
        this.title = title;
        this.topic = topic;
        this.description = description;
        this.subjectName = subjectName;
        this.kind = SpaceKind.PERSONAL;
        this.visibility = visibility != null ? visibility : SpaceVisibility.PRIVATE;
    }

    public void update(String title, String topic, String description, String subjectName,
                        SpaceKind kind, SpaceVisibility visibility) {
        if (title != null) this.title = title;
        if (topic != null) this.topic = topic;
        if (description != null) this.description = description;
        if (subjectName != null) this.subjectName = subjectName;
        if (kind != null) this.kind = kind;
        if (visibility != null) this.visibility = visibility;
    }

    public boolean isOwnedBy(String userId) {
        return this.owner.getId().equals(userId);
    }
}
```

- [ ] **Step 4: Repository, DTO 작성**

`src/main/java/com/bsbowl/onti/domain/space/repository/RecordSpaceRepository.java`

```java
package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.RecordSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordSpaceRepository extends JpaRepository<RecordSpace, String> {
    List<RecordSpace> findAllByOwnerId(String ownerId);
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/SpaceCreateRequest.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.SpaceVisibility;
import jakarta.validation.constraints.NotBlank;

public record SpaceCreateRequest(@NotBlank String title, String topic, String description,
                                  String subjectName, SpaceVisibility visibility) {
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/SpaceUpdateRequest.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.SpaceKind;
import com.bsbowl.onti.domain.space.entity.SpaceVisibility;

public record SpaceUpdateRequest(String title, String topic, String description, String subjectName,
                                  SpaceKind kind, SpaceVisibility visibility) {
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/SpaceResponse.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceKind;
import com.bsbowl.onti.domain.space.entity.SpaceVisibility;

public record SpaceResponse(String id, String title, String topic, String description, String subjectName,
                             SpaceKind kind, SpaceVisibility visibility) {
    public static SpaceResponse from(RecordSpace space) {
        return new SpaceResponse(space.getId(), space.getTitle(), space.getTopic(), space.getDescription(),
                space.getSubjectName(), space.getKind(), space.getVisibility());
    }
}
```

- [ ] **Step 5: 실패하는 SpaceServiceTest 작성**

`src/test/java/com/bsbowl/onti/domain/space/service/SpaceServiceTest.java`

```java
package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.SpaceCreateRequest;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceKind;
import com.bsbowl.onti.domain.space.repository.RecordSpaceRepository;
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
class SpaceServiceTest {

    @Mock
    private RecordSpaceRepository recordSpaceRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private SpaceService spaceService;

    @Test
    void create_savesSpaceWithPersonalKind() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(recordSpaceRepository.save(any(RecordSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = spaceService.create("user-1", new SpaceCreateRequest("엄마 이야기", "부모님의 삶", null, "엄마", null));

        assertThat(response.kind()).isEqualTo(SpaceKind.PERSONAL);
        assertThat(response.title()).isEqualTo("엄마 이야기");
    }

    @Test
    void getOwnedSpace_notOwner_throwsForbidden() {
        User owner = User.builder().email("owner@onti.com").password("x").name("owner").build();
        ReflectionTestUtils.setField(owner, "id", "owner-id");
        RecordSpace space = RecordSpace.builder().owner(owner).title("공간").build();
        when(recordSpaceRepository.findById("space-1")).thenReturn(Optional.of(space));

        assertThatThrownBy(() -> spaceService.getOwnedSpace("space-1", "other-id"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void getOwnedSpace_notFound_throwsSpaceNotFound() {
        when(recordSpaceRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spaceService.getOwnedSpace("missing", "user-1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPACE_NOT_FOUND);
    }
}
```

- [ ] **Step 6: 테스트 실패 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.space.service.SpaceServiceTest"`
Expected: FAIL (컴파일 에러 — `SpaceService` 클래스가 아직 없음)

- [ ] **Step 7: SpaceService 구현**

`src/main/java/com/bsbowl/onti/domain/space/service/SpaceService.java`

```java
package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.SpaceCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceResponse;
import com.bsbowl.onti.domain.space.dto.SpaceUpdateRequest;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.repository.RecordSpaceRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SpaceService {

    private final RecordSpaceRepository recordSpaceRepository;
    private final UserRepository userRepository;

    public SpaceService(RecordSpaceRepository recordSpaceRepository, UserRepository userRepository) {
        this.recordSpaceRepository = recordSpaceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SpaceResponse create(String userId, SpaceCreateRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        RecordSpace space = RecordSpace.builder()
                .owner(owner)
                .title(request.title())
                .topic(request.topic())
                .description(request.description())
                .subjectName(request.subjectName())
                .visibility(request.visibility())
                .build();
        return SpaceResponse.from(recordSpaceRepository.save(space));
    }

    public List<SpaceResponse> list(String userId) {
        return recordSpaceRepository.findAllByOwnerId(userId).stream()
                .map(SpaceResponse::from)
                .toList();
    }

    public SpaceResponse get(String spaceId, String userId) {
        return SpaceResponse.from(getOwnedSpace(spaceId, userId));
    }

    @Transactional
    public SpaceResponse update(String spaceId, String userId, SpaceUpdateRequest request) {
        RecordSpace space = getOwnedSpace(spaceId, userId);
        space.update(request.title(), request.topic(), request.description(), request.subjectName(),
                request.kind(), request.visibility());
        return SpaceResponse.from(space);
    }

    @Transactional
    public void delete(String spaceId, String userId) {
        recordSpaceRepository.delete(getOwnedSpace(spaceId, userId));
    }

    public RecordSpace getOwnedSpace(String spaceId, String userId) {
        RecordSpace space = recordSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPACE_NOT_FOUND));
        if (!space.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return space;
    }
}
```

- [ ] **Step 8: 테스트 통과 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.space.service.SpaceServiceTest"`
Expected: `BUILD SUCCESSFUL`, 3개 테스트 통과

- [ ] **Step 9: SpaceController 구현**

`src/main/java/com/bsbowl/onti/domain/space/controller/SpaceController.java`

```java
package com.bsbowl.onti.domain.space.controller;

import com.bsbowl.onti.domain.space.dto.SpaceCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceResponse;
import com.bsbowl.onti.domain.space.dto.SpaceUpdateRequest;
import com.bsbowl.onti.domain.space.service.SpaceService;
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
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SpaceResponse>> create(@AuthenticationPrincipal String userId,
                                                               @Valid @RequestBody SpaceCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceService.create(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SpaceResponse>>> list(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(spaceService.list(userId)));
    }

    @GetMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<SpaceResponse>> get(@AuthenticationPrincipal String userId,
                                                            @PathVariable String spaceId) {
        return ResponseEntity.ok(ApiResponse.success(spaceService.get(spaceId, userId)));
    }

    @PatchMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<SpaceResponse>> update(@AuthenticationPrincipal String userId,
                                                               @PathVariable String spaceId,
                                                               @RequestBody SpaceUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceService.update(spaceId, userId, request)));
    }

    @DeleteMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String spaceId) {
        spaceService.delete(spaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

- [ ] **Step 10: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: Space(기록 공간) 도메인 기초 및 신규 ErrorCode 추가"
```

---

### Task 2: Participant 도메인

**Files:**
- Create: `src/main/java/com/bsbowl/onti/domain/space/entity/ParticipantRole.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/entity/ParticipantStatus.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/entity/Participant.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/repository/ParticipantRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/ParticipantCreateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/ParticipantUpdateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/ParticipantResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/service/ParticipantService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/controller/ParticipantController.java`
- Test: `src/test/java/com/bsbowl/onti/domain/space/service/ParticipantServiceTest.java`

**Interfaces:**
- Consumes: `SpaceService.getOwnedSpace(String, String): RecordSpace`(Task 1)
- Produces: `Participant` 엔티티, `POST/GET /api/spaces/{spaceId}/participants`, `PATCH/DELETE /api/participants/{participantId}`. Task 3/4가 `ParticipantRepository`를 직접 써서 "같은 공간 소속인지" 교차 검증을 한다 — `Participant`는 자체 소유권 메서드가 없고, 상위 `RecordSpace`를 통해서만 검증한다.

- [ ] **Step 1: ParticipantRole, ParticipantStatus, Participant 엔티티 작성**

`src/main/java/com/bsbowl/onti/domain/space/entity/ParticipantRole.java`

```java
package com.bsbowl.onti.domain.space.entity;

public enum ParticipantRole {
    OWNER, PARTICIPANT, VIEWER
}
```

`src/main/java/com/bsbowl/onti/domain/space/entity/ParticipantStatus.java`

```java
package com.bsbowl.onti.domain.space.entity;

public enum ParticipantStatus {
    PENDING, JOINED
}
```

`src/main/java/com/bsbowl/onti/domain/space/entity/Participant.java`

```java
package com.bsbowl.onti.domain.space.entity;

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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RecordSpace space;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status;

    private LocalDateTime joinedAt;

    private String photoUrl;

    @Builder
    private Participant(RecordSpace space, String displayName, String email, ParticipantRole role, String photoUrl) {
        this.space = space;
        this.displayName = displayName;
        this.email = email;
        this.role = role != null ? role : ParticipantRole.PARTICIPANT;
        this.status = ParticipantStatus.PENDING;
        this.photoUrl = photoUrl;
    }

    public void update(String displayName, ParticipantRole role, ParticipantStatus status, String photoUrl) {
        if (displayName != null) this.displayName = displayName;
        if (role != null) this.role = role;
        if (status != null) {
            this.status = status;
            if (status == ParticipantStatus.JOINED && this.joinedAt == null) {
                this.joinedAt = LocalDateTime.now();
            }
        }
        if (photoUrl != null) this.photoUrl = photoUrl;
    }
}
```

- [ ] **Step 2: Repository, DTO 작성**

`src/main/java/com/bsbowl/onti/domain/space/repository/ParticipantRepository.java`

```java
package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, String> {
    List<Participant> findAllBySpaceId(String spaceId);
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/ParticipantCreateRequest.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.ParticipantRole;
import jakarta.validation.constraints.NotBlank;

public record ParticipantCreateRequest(@NotBlank String displayName, @NotBlank String email, ParticipantRole role) {
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/ParticipantUpdateRequest.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.ParticipantRole;
import com.bsbowl.onti.domain.space.entity.ParticipantStatus;

public record ParticipantUpdateRequest(String displayName, ParticipantRole role, ParticipantStatus status, String photoUrl) {
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/ParticipantResponse.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.ParticipantRole;
import com.bsbowl.onti.domain.space.entity.ParticipantStatus;

import java.time.LocalDateTime;

public record ParticipantResponse(String id, String spaceId, String displayName, String email, ParticipantRole role,
                                   ParticipantStatus status, LocalDateTime joinedAt, String photoUrl) {
    public static ParticipantResponse from(Participant participant) {
        return new ParticipantResponse(participant.getId(), participant.getSpace().getId(), participant.getDisplayName(),
                participant.getEmail(), participant.getRole(), participant.getStatus(), participant.getJoinedAt(),
                participant.getPhotoUrl());
    }
}
```

- [ ] **Step 3: 실패하는 ParticipantServiceTest 작성**

`src/test/java/com/bsbowl/onti/domain/space/service/ParticipantServiceTest.java`

```java
package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.ParticipantCreateRequest;
import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.ParticipantRole;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
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
class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private SpaceService spaceService;
    @InjectMocks
    private ParticipantService participantService;

    @Test
    void create_savesParticipantWithPendingStatus() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        when(spaceService.getOwnedSpace("space-1", "user-1")).thenReturn(space);
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = participantService.create("space-1", "user-1",
                new ParticipantCreateRequest("엄마", "mom@onti.com", ParticipantRole.PARTICIPANT));

        assertThat(response.displayName()).isEqualTo("엄마");
        assertThat(response.status()).isEqualTo(com.bsbowl.onti.domain.space.entity.ParticipantStatus.PENDING);
    }

    @Test
    void update_notFound_throwsParticipantNotFound() {
        when(participantRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participantService.update("missing", "user-1",
                new com.bsbowl.onti.domain.space.dto.ParticipantUpdateRequest(null, null, null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }
}
```

- [ ] **Step 4: 테스트 실패 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.space.service.ParticipantServiceTest"`
Expected: FAIL (컴파일 에러 — `ParticipantService` 클래스가 아직 없음)

- [ ] **Step 5: ParticipantService 구현**

`src/main/java/com/bsbowl/onti/domain/space/service/ParticipantService.java`

```java
package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.ParticipantCreateRequest;
import com.bsbowl.onti.domain.space.dto.ParticipantResponse;
import com.bsbowl.onti.domain.space.dto.ParticipantUpdateRequest;
import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final SpaceService spaceService;

    public ParticipantService(ParticipantRepository participantRepository, SpaceService spaceService) {
        this.participantRepository = participantRepository;
        this.spaceService = spaceService;
    }

    @Transactional
    public ParticipantResponse create(String spaceId, String userId, ParticipantCreateRequest request) {
        RecordSpace space = spaceService.getOwnedSpace(spaceId, userId);
        Participant participant = Participant.builder()
                .space(space)
                .displayName(request.displayName())
                .email(request.email())
                .role(request.role())
                .build();
        return ParticipantResponse.from(participantRepository.save(participant));
    }

    public List<ParticipantResponse> list(String spaceId, String userId) {
        spaceService.getOwnedSpace(spaceId, userId);
        return participantRepository.findAllBySpaceId(spaceId).stream()
                .map(ParticipantResponse::from)
                .toList();
    }

    @Transactional
    public ParticipantResponse update(String participantId, String userId, ParticipantUpdateRequest request) {
        Participant participant = getOwnedParticipant(participantId, userId);
        participant.update(request.displayName(), request.role(), request.status(), request.photoUrl());
        return ParticipantResponse.from(participant);
    }

    @Transactional
    public void delete(String participantId, String userId) {
        participantRepository.delete(getOwnedParticipant(participantId, userId));
    }

    private Participant getOwnedParticipant(String participantId, String userId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND));
        spaceService.getOwnedSpace(participant.getSpace().getId(), userId);
        return participant;
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.space.service.ParticipantServiceTest"`
Expected: `BUILD SUCCESSFUL`, 2개 테스트 통과

- [ ] **Step 7: ParticipantController 구현**

`src/main/java/com/bsbowl/onti/domain/space/controller/ParticipantController.java`

```java
package com.bsbowl.onti.domain.space.controller;

import com.bsbowl.onti.domain.space.dto.ParticipantCreateRequest;
import com.bsbowl.onti.domain.space.dto.ParticipantResponse;
import com.bsbowl.onti.domain.space.dto.ParticipantUpdateRequest;
import com.bsbowl.onti.domain.space.service.ParticipantService;
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
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping("/api/spaces/{spaceId}/participants")
    public ResponseEntity<ApiResponse<ParticipantResponse>> create(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String spaceId,
                                                                     @Valid @RequestBody ParticipantCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(participantService.create(spaceId, userId, request)));
    }

    @GetMapping("/api/spaces/{spaceId}/participants")
    public ResponseEntity<ApiResponse<List<ParticipantResponse>>> list(@AuthenticationPrincipal String userId,
                                                                         @PathVariable String spaceId) {
        return ResponseEntity.ok(ApiResponse.success(participantService.list(spaceId, userId)));
    }

    @PatchMapping("/api/participants/{participantId}")
    public ResponseEntity<ApiResponse<ParticipantResponse>> update(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String participantId,
                                                                     @RequestBody ParticipantUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(participantService.update(participantId, userId, request)));
    }

    @DeleteMapping("/api/participants/{participantId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String participantId) {
        participantService.delete(participantId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

- [ ] **Step 8: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: Participant(공간 참여자) CRUD API 구현"
```

---

### Task 3: SpaceQuestion 도메인

**Files:**
- Create: `src/main/java/com/bsbowl/onti/domain/space/entity/QuestionSource.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/entity/SpaceQuestion.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/repository/SpaceQuestionRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/SpaceQuestionCreateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/SpaceQuestionUpdateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/SpaceQuestionResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/service/SpaceQuestionService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/controller/SpaceQuestionController.java`
- Test: `src/test/java/com/bsbowl/onti/domain/space/service/SpaceQuestionServiceTest.java`

**Interfaces:**
- Consumes: `SpaceService.getOwnedSpace`(Task 1), `ParticipantRepository`(Task 2)
- Produces: `SpaceQuestion` 엔티티, `POST/GET /api/spaces/{spaceId}/questions`, `PATCH/DELETE /api/questions/{questionId}`. Question 생성 시 `createdByParticipantId`가 **같은 공간 소속인지** 검증한다 (Task 8의 Record↔Chapter 교차 검증과 동일한 패턴) — 아니면 `PARTICIPANT_NOT_FOUND`.

- [ ] **Step 1: QuestionSource, SpaceQuestion 엔티티 작성**

`src/main/java/com/bsbowl/onti/domain/space/entity/QuestionSource.java`

```java
package com.bsbowl.onti.domain.space.entity;

public enum QuestionSource {
    ONTI, CUSTOM
}
```

`src/main/java/com/bsbowl/onti/domain/space/entity/SpaceQuestion.java`

```java
package com.bsbowl.onti.domain.space.entity;

import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "space_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RecordSpace space;

    @Column(nullable = false, length = 1000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionSource source;

    // ponytail: 참여자 삭제 시 그가 만든 질문도 함께 지운다 (단순한 기본값).
    // 질문만 남기고 작성자만 지우고 싶어지면 SET_NULL + nullable로 바꾸면 됨.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_participant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Participant createdBy;

    @ElementCollection
    @CollectionTable(name = "space_question_recipients", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "participant_id")
    private List<String> sentToParticipantIds = new ArrayList<>();

    @Builder
    private SpaceQuestion(RecordSpace space, String text, QuestionSource source, Participant createdBy,
                           List<String> sentToParticipantIds) {
        this.space = space;
        this.text = text;
        this.source = source;
        this.createdBy = createdBy;
        this.sentToParticipantIds = sentToParticipantIds != null
                ? new ArrayList<>(sentToParticipantIds) : new ArrayList<>();
    }

    public void update(String text, List<String> sentToParticipantIds) {
        if (text != null) this.text = text;
        if (sentToParticipantIds != null) {
            this.sentToParticipantIds.clear();
            this.sentToParticipantIds.addAll(sentToParticipantIds);
        }
    }
}
```

- [ ] **Step 2: Repository, DTO 작성**

`src/main/java/com/bsbowl/onti/domain/space/repository/SpaceQuestionRepository.java`

```java
package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.SpaceQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceQuestionRepository extends JpaRepository<SpaceQuestion, String> {
    List<SpaceQuestion> findAllBySpaceId(String spaceId);
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/SpaceQuestionCreateRequest.java`

```java
package com.bsbowl.onti.domain.space.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SpaceQuestionCreateRequest(@NotBlank String text, @NotBlank String createdByParticipantId,
                                          List<String> sentToParticipantIds) {
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/SpaceQuestionUpdateRequest.java`

```java
package com.bsbowl.onti.domain.space.dto;

import java.util.List;

public record SpaceQuestionUpdateRequest(String text, List<String> sentToParticipantIds) {
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/SpaceQuestionResponse.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.QuestionSource;
import com.bsbowl.onti.domain.space.entity.SpaceQuestion;

import java.util.List;

public record SpaceQuestionResponse(String id, String spaceId, String text, QuestionSource source,
                                     String createdByParticipantId, List<String> sentToParticipantIds) {
    public static SpaceQuestionResponse from(SpaceQuestion question) {
        return new SpaceQuestionResponse(question.getId(), question.getSpace().getId(), question.getText(),
                question.getSource(), question.getCreatedBy().getId(), question.getSentToParticipantIds());
    }
}
```

- [ ] **Step 3: 실패하는 SpaceQuestionServiceTest 작성**

`src/test/java/com/bsbowl/onti/domain/space/service/SpaceQuestionServiceTest.java`

```java
package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.SpaceQuestionCreateRequest;
import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceQuestion;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
import com.bsbowl.onti.domain.space.repository.SpaceQuestionRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceQuestionServiceTest {

    @Mock
    private SpaceQuestionRepository spaceQuestionRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private SpaceService spaceService;
    @InjectMocks
    private SpaceQuestionService spaceQuestionService;

    @Test
    void create_savesQuestionWithCustomSource() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        ReflectionTestUtils.setField(space, "id", "space-1");
        Participant participant = Participant.builder().space(space).displayName("엄마").email("mom@onti.com").build();
        when(spaceService.getOwnedSpace("space-1", "user-1")).thenReturn(space);
        when(participantRepository.findById("participant-1")).thenReturn(Optional.of(participant));
        when(spaceQuestionRepository.save(any(SpaceQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = spaceQuestionService.create("space-1", "user-1",
                new SpaceQuestionCreateRequest("가장 행복했던 순간은?", "participant-1", List.of()));

        assertThat(response.text()).isEqualTo("가장 행복했던 순간은?");
        assertThat(response.source()).isEqualTo(com.bsbowl.onti.domain.space.entity.QuestionSource.CUSTOM);
    }

    @Test
    void create_participantFromDifferentSpace_throwsParticipantNotFound() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        ReflectionTestUtils.setField(space, "id", "space-1");
        RecordSpace otherSpace = RecordSpace.builder().owner(user).title("다른 공간").build();
        ReflectionTestUtils.setField(otherSpace, "id", "space-2");
        Participant foreignParticipant = Participant.builder().space(otherSpace).displayName("남").email("x@onti.com").build();
        when(spaceService.getOwnedSpace("space-1", "user-1")).thenReturn(space);
        when(participantRepository.findById("participant-2")).thenReturn(Optional.of(foreignParticipant));

        assertThatThrownBy(() -> spaceQuestionService.create("space-1", "user-1",
                new SpaceQuestionCreateRequest("질문", "participant-2", List.of())))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }
}
```

- [ ] **Step 4: 테스트 실패 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.space.service.SpaceQuestionServiceTest"`
Expected: FAIL (컴파일 에러 — `SpaceQuestionService` 클래스가 아직 없음)

- [ ] **Step 5: SpaceQuestionService 구현**

`src/main/java/com/bsbowl/onti/domain/space/service/SpaceQuestionService.java`

```java
package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.SpaceQuestionCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceQuestionResponse;
import com.bsbowl.onti.domain.space.dto.SpaceQuestionUpdateRequest;
import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.QuestionSource;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceQuestion;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
import com.bsbowl.onti.domain.space.repository.SpaceQuestionRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SpaceQuestionService {

    private final SpaceQuestionRepository spaceQuestionRepository;
    private final ParticipantRepository participantRepository;
    private final SpaceService spaceService;

    public SpaceQuestionService(SpaceQuestionRepository spaceQuestionRepository,
                                 ParticipantRepository participantRepository, SpaceService spaceService) {
        this.spaceQuestionRepository = spaceQuestionRepository;
        this.participantRepository = participantRepository;
        this.spaceService = spaceService;
    }

    @Transactional
    public SpaceQuestionResponse create(String spaceId, String userId, SpaceQuestionCreateRequest request) {
        RecordSpace space = spaceService.getOwnedSpace(spaceId, userId);
        Participant createdBy = participantRepository.findById(request.createdByParticipantId())
                .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND));
        if (!createdBy.getSpace().getId().equals(spaceId)) {
            throw new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
        SpaceQuestion question = SpaceQuestion.builder()
                .space(space)
                .text(request.text())
                .source(QuestionSource.CUSTOM)
                .createdBy(createdBy)
                .sentToParticipantIds(request.sentToParticipantIds())
                .build();
        return SpaceQuestionResponse.from(spaceQuestionRepository.save(question));
    }

    public List<SpaceQuestionResponse> list(String spaceId, String userId) {
        spaceService.getOwnedSpace(spaceId, userId);
        return spaceQuestionRepository.findAllBySpaceId(spaceId).stream()
                .map(SpaceQuestionResponse::from)
                .toList();
    }

    @Transactional
    public SpaceQuestionResponse update(String questionId, String userId, SpaceQuestionUpdateRequest request) {
        SpaceQuestion question = getOwnedQuestion(questionId, userId);
        question.update(request.text(), request.sentToParticipantIds());
        return SpaceQuestionResponse.from(question);
    }

    @Transactional
    public void delete(String questionId, String userId) {
        spaceQuestionRepository.delete(getOwnedQuestion(questionId, userId));
    }

    private SpaceQuestion getOwnedQuestion(String questionId, String userId) {
        SpaceQuestion question = spaceQuestionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPACE_QUESTION_NOT_FOUND));
        spaceService.getOwnedSpace(question.getSpace().getId(), userId);
        return question;
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.space.service.SpaceQuestionServiceTest"`
Expected: `BUILD SUCCESSFUL`, 2개 테스트 통과

- [ ] **Step 7: SpaceQuestionController 구현**

`src/main/java/com/bsbowl/onti/domain/space/controller/SpaceQuestionController.java`

```java
package com.bsbowl.onti.domain.space.controller;

import com.bsbowl.onti.domain.space.dto.SpaceQuestionCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceQuestionResponse;
import com.bsbowl.onti.domain.space.dto.SpaceQuestionUpdateRequest;
import com.bsbowl.onti.domain.space.service.SpaceQuestionService;
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
public class SpaceQuestionController {

    private final SpaceQuestionService spaceQuestionService;

    public SpaceQuestionController(SpaceQuestionService spaceQuestionService) {
        this.spaceQuestionService = spaceQuestionService;
    }

    @PostMapping("/api/spaces/{spaceId}/questions")
    public ResponseEntity<ApiResponse<SpaceQuestionResponse>> create(@AuthenticationPrincipal String userId,
                                                                       @PathVariable String spaceId,
                                                                       @Valid @RequestBody SpaceQuestionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceQuestionService.create(spaceId, userId, request)));
    }

    @GetMapping("/api/spaces/{spaceId}/questions")
    public ResponseEntity<ApiResponse<List<SpaceQuestionResponse>>> list(@AuthenticationPrincipal String userId,
                                                                           @PathVariable String spaceId) {
        return ResponseEntity.ok(ApiResponse.success(spaceQuestionService.list(spaceId, userId)));
    }

    @PatchMapping("/api/questions/{questionId}")
    public ResponseEntity<ApiResponse<SpaceQuestionResponse>> update(@AuthenticationPrincipal String userId,
                                                                       @PathVariable String questionId,
                                                                       @RequestBody SpaceQuestionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceQuestionService.update(questionId, userId, request)));
    }

    @DeleteMapping("/api/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String questionId) {
        spaceQuestionService.delete(questionId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

- [ ] **Step 8: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: SpaceQuestion(공간 질문) CRUD API 구현"
```

---

### Task 4: SpaceRecord + SpaceRecordImage 도메인

**Files:**
- Create: `src/main/java/com/bsbowl/onti/domain/space/entity/SpaceRecord.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/entity/SpaceRecordImage.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/repository/SpaceRecordRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/repository/SpaceRecordImageRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/SpaceRecordCreateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/SpaceRecordUpdateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/SpaceRecordResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/SpaceRecordImageResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/SpaceRecordImageCreateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/service/SpaceRecordService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/controller/SpaceRecordController.java`
- Test: `src/test/java/com/bsbowl/onti/domain/space/service/SpaceRecordServiceTest.java`

**Interfaces:**
- Consumes: `SpaceService.getOwnedSpace`(Task 1), `ParticipantRepository`(Task 2), `SpaceQuestionRepository`(Task 3), 기존 `domain.record.entity.RecordType`(재사용, 새 enum 안 만듦)
- Produces: `SpaceRecord`/`SpaceRecordImage` 엔티티, `SpaceRecordService.getOwnedSpaceRecord(String recordId, String userId): SpaceRecord` (public — Task 5가 재사용), `POST/GET /api/spaces/{spaceId}/records`, `GET/PATCH/DELETE /api/space-records/{recordId}`, `POST /api/space-records/{recordId}/images`, `DELETE /api/space-records/{recordId}/images/{imageId}`.

- [ ] **Step 1: SpaceRecord, SpaceRecordImage 엔티티 작성**

`src/main/java/com/bsbowl/onti/domain/space/entity/SpaceRecord.java`

```java
package com.bsbowl.onti.domain.space.entity;

import com.bsbowl.onti.domain.record.entity.RecordType;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(name = "space_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RecordSpace space;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordType type;

    private String title;

    @Column(length = 4000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_participant_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Participant author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_question_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private SpaceQuestion answeredQuestion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceVisibility visibility;

    private String occurredAt;

    @Builder
    private SpaceRecord(RecordSpace space, RecordType type, String title, String content, Participant author,
                         SpaceQuestion answeredQuestion, SpaceVisibility visibility, String occurredAt) {
        this.space = space;
        this.type = type;
        this.title = title;
        this.content = content;
        this.author = author;
        this.answeredQuestion = answeredQuestion;
        this.visibility = visibility != null ? visibility : SpaceVisibility.PRIVATE;
        this.occurredAt = occurredAt;
    }

    public void update(String title, String content, SpaceVisibility visibility, String occurredAt) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (visibility != null) this.visibility = visibility;
        if (occurredAt != null) this.occurredAt = occurredAt;
    }
}
```

`src/main/java/com/bsbowl/onti/domain/space/entity/SpaceRecordImage.java`

```java
package com.bsbowl.onti.domain.space.entity;

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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(name = "space_record_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceRecordImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_record_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SpaceRecord spaceRecord;

    @Column(nullable = false)
    private String url;

    private String caption;

    @Column(name = "sort_order", nullable = false)
    private int order;

    @Builder
    private SpaceRecordImage(SpaceRecord spaceRecord, String url, String caption, int order) {
        this.spaceRecord = spaceRecord;
        this.url = url;
        this.caption = caption;
        this.order = order;
    }
}
```

- [ ] **Step 2: Repository, DTO 작성**

`src/main/java/com/bsbowl/onti/domain/space/repository/SpaceRecordRepository.java`

```java
package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceRecordRepository extends JpaRepository<SpaceRecord, String> {
    List<SpaceRecord> findAllBySpaceId(String spaceId);
}
```

`src/main/java/com/bsbowl/onti/domain/space/repository/SpaceRecordImageRepository.java`

```java
package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.SpaceRecordImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceRecordImageRepository extends JpaRepository<SpaceRecordImage, String> {
    List<SpaceRecordImage> findAllBySpaceRecordIdOrderByOrderAsc(String spaceRecordId);
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/SpaceRecordCreateRequest.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.record.entity.RecordType;
import com.bsbowl.onti.domain.space.entity.SpaceVisibility;
import jakarta.validation.constraints.NotNull;

public record SpaceRecordCreateRequest(@NotNull RecordType type, String title, String content,
                                        String authorParticipantId, String answeredQuestionId,
                                        SpaceVisibility visibility, String occurredAt) {
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/SpaceRecordUpdateRequest.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.SpaceVisibility;

public record SpaceRecordUpdateRequest(String title, String content, SpaceVisibility visibility, String occurredAt) {
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/SpaceRecordImageResponse.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.SpaceRecordImage;

public record SpaceRecordImageResponse(String id, String url, String caption, int order) {
    public static SpaceRecordImageResponse from(SpaceRecordImage image) {
        return new SpaceRecordImageResponse(image.getId(), image.getUrl(), image.getCaption(), image.getOrder());
    }
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/SpaceRecordImageCreateRequest.java`

```java
package com.bsbowl.onti.domain.space.dto;

import jakarta.validation.constraints.NotBlank;

public record SpaceRecordImageCreateRequest(@NotBlank String url, String caption) {
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/SpaceRecordResponse.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.record.entity.RecordType;
import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import com.bsbowl.onti.domain.space.entity.SpaceVisibility;

import java.util.List;

public record SpaceRecordResponse(String id, String spaceId, RecordType type, String title, String content,
                                   String authorParticipantId, String answeredQuestionId, SpaceVisibility visibility,
                                   String occurredAt, List<SpaceRecordImageResponse> images) {
    public static SpaceRecordResponse from(SpaceRecord record, List<SpaceRecordImageResponse> images) {
        return new SpaceRecordResponse(
                record.getId(),
                record.getSpace().getId(),
                record.getType(),
                record.getTitle(),
                record.getContent(),
                record.getAuthor() != null ? record.getAuthor().getId() : null,
                record.getAnsweredQuestion() != null ? record.getAnsweredQuestion().getId() : null,
                record.getVisibility(),
                record.getOccurredAt(),
                images
        );
    }
}
```

- [ ] **Step 3: 실패하는 SpaceRecordServiceTest 작성**

`src/test/java/com/bsbowl/onti/domain/space/service/SpaceRecordServiceTest.java`

```java
package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.record.entity.RecordType;
import com.bsbowl.onti.domain.space.dto.SpaceRecordCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceRecordImageCreateRequest;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import com.bsbowl.onti.domain.space.entity.SpaceRecordImage;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
import com.bsbowl.onti.domain.space.repository.SpaceQuestionRepository;
import com.bsbowl.onti.domain.space.repository.SpaceRecordImageRepository;
import com.bsbowl.onti.domain.space.repository.SpaceRecordRepository;
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
class SpaceRecordServiceTest {

    @Mock
    private SpaceRecordRepository spaceRecordRepository;
    @Mock
    private SpaceRecordImageRepository spaceRecordImageRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private SpaceQuestionRepository spaceQuestionRepository;
    @Mock
    private SpaceService spaceService;
    @InjectMocks
    private SpaceRecordService spaceRecordService;

    @Test
    void create_withoutAuthorOrQuestion_savesRecord() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        when(spaceService.getOwnedSpace("space-1", "user-1")).thenReturn(space);
        when(spaceRecordRepository.save(any(SpaceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRecordImageRepository.findAllBySpaceRecordIdOrderByOrderAsc(any())).thenReturn(Collections.emptyList());

        var response = spaceRecordService.create("space-1", "user-1",
                new SpaceRecordCreateRequest(RecordType.TEXT, "제목", "내용", null, null, null, "1998년 봄"));

        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.occurredAt()).isEqualTo("1998년 봄");
        assertThat(response.images()).isEmpty();
    }

    @Test
    void addImage_assignsNextOrder() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        SpaceRecord record = SpaceRecord.builder().space(space).type(RecordType.PHOTO).build();
        when(spaceRecordRepository.findById("record-1")).thenReturn(Optional.of(record));
        when(spaceService.getOwnedSpace(any(), any())).thenReturn(space);
        when(spaceRecordImageRepository.findAllBySpaceRecordIdOrderByOrderAsc("record-1")).thenReturn(Collections.emptyList());
        when(spaceRecordImageRepository.save(any(SpaceRecordImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = spaceRecordService.addImage("record-1", "user-1",
                new SpaceRecordImageCreateRequest("https://example.com/a.jpg", "설명"));

        assertThat(response.order()).isEqualTo(0);
        assertThat(response.url()).isEqualTo("https://example.com/a.jpg");
    }

    @Test
    void deleteImage_imageFromDifferentRecord_throwsImageNotFound() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        SpaceRecord record = SpaceRecord.builder().space(space).type(RecordType.PHOTO).build();
        SpaceRecord otherRecord = SpaceRecord.builder().space(space).type(RecordType.PHOTO).build();
        org.springframework.test.util.ReflectionTestUtils.setField(record, "id", "record-1");
        org.springframework.test.util.ReflectionTestUtils.setField(otherRecord, "id", "record-2");
        SpaceRecordImage foreignImage = SpaceRecordImage.builder().spaceRecord(otherRecord).url("https://x").order(0).build();
        when(spaceRecordRepository.findById("record-1")).thenReturn(Optional.of(record));
        when(spaceService.getOwnedSpace(any(), any())).thenReturn(space);
        when(spaceRecordImageRepository.findById("image-2")).thenReturn(Optional.of(foreignImage));

        assertThatThrownBy(() -> spaceRecordService.deleteImage("record-1", "image-2", "user-1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPACE_RECORD_IMAGE_NOT_FOUND);
    }
}
```

- [ ] **Step 4: 테스트 실패 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.space.service.SpaceRecordServiceTest"`
Expected: FAIL (컴파일 에러 — `SpaceRecordService` 클래스가 아직 없음)

- [ ] **Step 5: SpaceRecordService 구현**

`src/main/java/com/bsbowl/onti/domain/space/service/SpaceRecordService.java`

```java
package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.SpaceRecordCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceRecordImageCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceRecordImageResponse;
import com.bsbowl.onti.domain.space.dto.SpaceRecordResponse;
import com.bsbowl.onti.domain.space.dto.SpaceRecordUpdateRequest;
import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceQuestion;
import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import com.bsbowl.onti.domain.space.entity.SpaceRecordImage;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
import com.bsbowl.onti.domain.space.repository.SpaceQuestionRepository;
import com.bsbowl.onti.domain.space.repository.SpaceRecordImageRepository;
import com.bsbowl.onti.domain.space.repository.SpaceRecordRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SpaceRecordService {

    private final SpaceRecordRepository spaceRecordRepository;
    private final SpaceRecordImageRepository spaceRecordImageRepository;
    private final ParticipantRepository participantRepository;
    private final SpaceQuestionRepository spaceQuestionRepository;
    private final SpaceService spaceService;

    public SpaceRecordService(SpaceRecordRepository spaceRecordRepository,
                               SpaceRecordImageRepository spaceRecordImageRepository,
                               ParticipantRepository participantRepository,
                               SpaceQuestionRepository spaceQuestionRepository,
                               SpaceService spaceService) {
        this.spaceRecordRepository = spaceRecordRepository;
        this.spaceRecordImageRepository = spaceRecordImageRepository;
        this.participantRepository = participantRepository;
        this.spaceQuestionRepository = spaceQuestionRepository;
        this.spaceService = spaceService;
    }

    @Transactional
    public SpaceRecordResponse create(String spaceId, String userId, SpaceRecordCreateRequest request) {
        RecordSpace space = spaceService.getOwnedSpace(spaceId, userId);
        Participant author = resolveParticipant(request.authorParticipantId(), spaceId);
        SpaceQuestion answeredQuestion = resolveQuestion(request.answeredQuestionId(), spaceId);
        SpaceRecord record = SpaceRecord.builder()
                .space(space)
                .type(request.type())
                .title(request.title())
                .content(request.content())
                .author(author)
                .answeredQuestion(answeredQuestion)
                .visibility(request.visibility())
                .occurredAt(request.occurredAt())
                .build();
        return toResponse(spaceRecordRepository.save(record));
    }

    public List<SpaceRecordResponse> list(String spaceId, String userId) {
        spaceService.getOwnedSpace(spaceId, userId);
        return spaceRecordRepository.findAllBySpaceId(spaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    public SpaceRecordResponse get(String recordId, String userId) {
        return toResponse(getOwnedSpaceRecord(recordId, userId));
    }

    @Transactional
    public SpaceRecordResponse update(String recordId, String userId, SpaceRecordUpdateRequest request) {
        SpaceRecord record = getOwnedSpaceRecord(recordId, userId);
        record.update(request.title(), request.content(), request.visibility(), request.occurredAt());
        return toResponse(record);
    }

    @Transactional
    public void delete(String recordId, String userId) {
        spaceRecordRepository.delete(getOwnedSpaceRecord(recordId, userId));
    }

    @Transactional
    public SpaceRecordImageResponse addImage(String recordId, String userId, SpaceRecordImageCreateRequest request) {
        SpaceRecord record = getOwnedSpaceRecord(recordId, userId);
        int nextOrder = spaceRecordImageRepository.findAllBySpaceRecordIdOrderByOrderAsc(recordId).size();
        SpaceRecordImage image = SpaceRecordImage.builder()
                .spaceRecord(record)
                .url(request.url())
                .caption(request.caption())
                .order(nextOrder)
                .build();
        return SpaceRecordImageResponse.from(spaceRecordImageRepository.save(image));
    }

    @Transactional
    public void deleteImage(String recordId, String imageId, String userId) {
        getOwnedSpaceRecord(recordId, userId);
        SpaceRecordImage image = spaceRecordImageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPACE_RECORD_IMAGE_NOT_FOUND));
        if (!image.getSpaceRecord().getId().equals(recordId)) {
            throw new CustomException(ErrorCode.SPACE_RECORD_IMAGE_NOT_FOUND);
        }
        spaceRecordImageRepository.delete(image);
    }

    public SpaceRecord getOwnedSpaceRecord(String recordId, String userId) {
        SpaceRecord record = spaceRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPACE_RECORD_NOT_FOUND));
        spaceService.getOwnedSpace(record.getSpace().getId(), userId);
        return record;
    }

    private Participant resolveParticipant(String participantId, String spaceId) {
        if (participantId == null) return null;
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND));
        if (!participant.getSpace().getId().equals(spaceId)) {
            throw new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
        return participant;
    }

    private SpaceQuestion resolveQuestion(String questionId, String spaceId) {
        if (questionId == null) return null;
        SpaceQuestion question = spaceQuestionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPACE_QUESTION_NOT_FOUND));
        if (!question.getSpace().getId().equals(spaceId)) {
            throw new CustomException(ErrorCode.SPACE_QUESTION_NOT_FOUND);
        }
        return question;
    }

    private SpaceRecordResponse toResponse(SpaceRecord record) {
        List<SpaceRecordImageResponse> images = spaceRecordImageRepository
                .findAllBySpaceRecordIdOrderByOrderAsc(record.getId()).stream()
                .map(SpaceRecordImageResponse::from)
                .toList();
        return SpaceRecordResponse.from(record, images);
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.space.service.SpaceRecordServiceTest"`
Expected: `BUILD SUCCESSFUL`, 3개 테스트 통과

- [ ] **Step 7: SpaceRecordController 구현**

`src/main/java/com/bsbowl/onti/domain/space/controller/SpaceRecordController.java`

```java
package com.bsbowl.onti.domain.space.controller;

import com.bsbowl.onti.domain.space.dto.SpaceRecordCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceRecordImageCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceRecordImageResponse;
import com.bsbowl.onti.domain.space.dto.SpaceRecordResponse;
import com.bsbowl.onti.domain.space.dto.SpaceRecordUpdateRequest;
import com.bsbowl.onti.domain.space.service.SpaceRecordService;
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
public class SpaceRecordController {

    private final SpaceRecordService spaceRecordService;

    public SpaceRecordController(SpaceRecordService spaceRecordService) {
        this.spaceRecordService = spaceRecordService;
    }

    @PostMapping("/api/spaces/{spaceId}/records")
    public ResponseEntity<ApiResponse<SpaceRecordResponse>> create(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String spaceId,
                                                                     @Valid @RequestBody SpaceRecordCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceRecordService.create(spaceId, userId, request)));
    }

    @GetMapping("/api/spaces/{spaceId}/records")
    public ResponseEntity<ApiResponse<List<SpaceRecordResponse>>> list(@AuthenticationPrincipal String userId,
                                                                         @PathVariable String spaceId) {
        return ResponseEntity.ok(ApiResponse.success(spaceRecordService.list(spaceId, userId)));
    }

    @GetMapping("/api/space-records/{recordId}")
    public ResponseEntity<ApiResponse<SpaceRecordResponse>> get(@AuthenticationPrincipal String userId,
                                                                  @PathVariable String recordId) {
        return ResponseEntity.ok(ApiResponse.success(spaceRecordService.get(recordId, userId)));
    }

    @PatchMapping("/api/space-records/{recordId}")
    public ResponseEntity<ApiResponse<SpaceRecordResponse>> update(@AuthenticationPrincipal String userId,
                                                                     @PathVariable String recordId,
                                                                     @RequestBody SpaceRecordUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceRecordService.update(recordId, userId, request)));
    }

    @DeleteMapping("/api/space-records/{recordId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String recordId) {
        spaceRecordService.delete(recordId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/api/space-records/{recordId}/images")
    public ResponseEntity<ApiResponse<SpaceRecordImageResponse>> addImage(@AuthenticationPrincipal String userId,
                                                                            @PathVariable String recordId,
                                                                            @Valid @RequestBody SpaceRecordImageCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(spaceRecordService.addImage(recordId, userId, request)));
    }

    @DeleteMapping("/api/space-records/{recordId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@AuthenticationPrincipal String userId,
                                                           @PathVariable String recordId,
                                                           @PathVariable String imageId) {
        spaceRecordService.deleteImage(recordId, imageId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

- [ ] **Step 8: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: SpaceRecord/SpaceRecordImage CRUD 및 이미지 추가/삭제 API 구현"
```

---

### Task 5: BookRecordLink 도메인

**Files:**
- Create: `src/main/java/com/bsbowl/onti/domain/space/entity/BookRecordLink.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/repository/BookRecordLinkRepository.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/BookRecordLinkCreateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/dto/BookRecordLinkResponse.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/service/BookRecordLinkService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/space/controller/BookRecordLinkController.java`
- Test: `src/test/java/com/bsbowl/onti/domain/space/service/BookRecordLinkServiceTest.java`

**Interfaces:**
- Consumes: `BookService.getOwnedBook`(기존, Task 5 of 첫 라운드), `ChapterRepository`(기존), `SpaceRecordService.getOwnedSpaceRecord`(이번 라운드 Task 4)
- Produces: `POST /api/books/{bookId}/record-links`, `GET /api/books/{bookId}/record-links`, `DELETE /api/record-links/{linkId}`. 생성 시 **책 소유권과 기록(공간) 소유권을 모두 검증**하고, `chapterId`가 주어지면 그 챕터가 같은 책 소속인지도 검증한다 (Task 8의 Record↔Chapter 교차 검증과 동일 패턴).

- [ ] **Step 1: BookRecordLink 엔티티 작성**

`src/main/java/com/bsbowl/onti/domain/space/entity/BookRecordLink.java`

```java
package com.bsbowl.onti.domain.space.entity;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(name = "book_record_links", uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "record_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookRecordLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SpaceRecord spaceRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Chapter chapter;

    @Column(name = "sort_order", nullable = false)
    private int order;

    @Builder
    private BookRecordLink(Book book, SpaceRecord spaceRecord, Chapter chapter, int order) {
        this.book = book;
        this.spaceRecord = spaceRecord;
        this.chapter = chapter;
        this.order = order;
    }
}
```

- [ ] **Step 2: Repository, DTO 작성**

`src/main/java/com/bsbowl/onti/domain/space/repository/BookRecordLinkRepository.java`

```java
package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.BookRecordLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRecordLinkRepository extends JpaRepository<BookRecordLink, String> {
    List<BookRecordLink> findAllByBookIdOrderByOrderAsc(String bookId);
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/BookRecordLinkCreateRequest.java`

```java
package com.bsbowl.onti.domain.space.dto;

import jakarta.validation.constraints.NotBlank;

public record BookRecordLinkCreateRequest(@NotBlank String spaceRecordId, String chapterId) {
}
```

`src/main/java/com/bsbowl/onti/domain/space/dto/BookRecordLinkResponse.java`

```java
package com.bsbowl.onti.domain.space.dto;

import com.bsbowl.onti.domain.space.entity.BookRecordLink;

public record BookRecordLinkResponse(String id, String bookId, String spaceRecordId, String chapterId, int order) {
    public static BookRecordLinkResponse from(BookRecordLink link) {
        return new BookRecordLinkResponse(link.getId(), link.getBook().getId(), link.getSpaceRecord().getId(),
                link.getChapter() != null ? link.getChapter().getId() : null, link.getOrder());
    }
}
```

- [ ] **Step 3: 실패하는 BookRecordLinkServiceTest 작성**

`src/test/java/com/bsbowl/onti/domain/space/service/BookRecordLinkServiceTest.java`

```java
package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
import com.bsbowl.onti.domain.record.entity.RecordType;
import com.bsbowl.onti.domain.space.dto.BookRecordLinkCreateRequest;
import com.bsbowl.onti.domain.space.entity.BookRecordLink;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import com.bsbowl.onti.domain.space.repository.BookRecordLinkRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookRecordLinkServiceTest {

    @Mock
    private BookRecordLinkRepository bookRecordLinkRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private BookService bookService;
    @Mock
    private SpaceRecordService spaceRecordService;
    @InjectMocks
    private BookRecordLinkService bookRecordLinkService;

    @Test
    void create_assignsNextOrder() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        SpaceRecord record = SpaceRecord.builder().space(space).type(RecordType.TEXT).build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(spaceRecordService.getOwnedSpaceRecord("record-1", "user-1")).thenReturn(record);
        when(bookRecordLinkRepository.findAllByBookIdOrderByOrderAsc("book-1")).thenReturn(Collections.emptyList());
        when(bookRecordLinkRepository.save(any(BookRecordLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookRecordLinkService.create("book-1", "user-1",
                new BookRecordLinkCreateRequest("record-1", null));

        assertThat(response.order()).isEqualTo(0);
        assertThat(response.chapterId()).isNull();
    }

    @Test
    void create_chapterFromDifferentBook_throwsChapterNotFound() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("내 책").build();
        ReflectionTestUtils.setField(book, "id", "book-1");
        Book otherBook = Book.builder().user(user).title("다른 책").build();
        ReflectionTestUtils.setField(otherBook, "id", "book-2");
        Chapter foreignChapter = Chapter.builder().book(otherBook).title("남의 챕터").order(0).build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        SpaceRecord record = SpaceRecord.builder().space(space).type(RecordType.TEXT).build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(spaceRecordService.getOwnedSpaceRecord("record-1", "user-1")).thenReturn(record);
        when(chapterRepository.findById("chapter-2")).thenReturn(java.util.Optional.of(foreignChapter));

        assertThatThrownBy(() -> bookRecordLinkService.create("book-1", "user-1",
                new BookRecordLinkCreateRequest("record-1", "chapter-2")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAPTER_NOT_FOUND);
    }
}
```

- [ ] **Step 4: 테스트 실패 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.space.service.BookRecordLinkServiceTest"`
Expected: FAIL (컴파일 에러 — `BookRecordLinkService` 클래스가 아직 없음)

- [ ] **Step 5: BookRecordLinkService 구현**

`src/main/java/com/bsbowl/onti/domain/space/service/BookRecordLinkService.java`

```java
package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
import com.bsbowl.onti.domain.space.dto.BookRecordLinkCreateRequest;
import com.bsbowl.onti.domain.space.dto.BookRecordLinkResponse;
import com.bsbowl.onti.domain.space.entity.BookRecordLink;
import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import com.bsbowl.onti.domain.space.repository.BookRecordLinkRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookRecordLinkService {

    private final BookRecordLinkRepository bookRecordLinkRepository;
    private final ChapterRepository chapterRepository;
    private final BookService bookService;
    private final SpaceRecordService spaceRecordService;

    public BookRecordLinkService(BookRecordLinkRepository bookRecordLinkRepository, ChapterRepository chapterRepository,
                                  BookService bookService, SpaceRecordService spaceRecordService) {
        this.bookRecordLinkRepository = bookRecordLinkRepository;
        this.chapterRepository = chapterRepository;
        this.bookService = bookService;
        this.spaceRecordService = spaceRecordService;
    }

    @Transactional
    public BookRecordLinkResponse create(String bookId, String userId, BookRecordLinkCreateRequest request) {
        Book book = bookService.getOwnedBook(bookId, userId);
        SpaceRecord spaceRecord = spaceRecordService.getOwnedSpaceRecord(request.spaceRecordId(), userId);
        Chapter chapter = null;
        if (request.chapterId() != null) {
            chapter = chapterRepository.findById(request.chapterId())
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));
            if (!chapter.getBook().getId().equals(bookId)) {
                throw new CustomException(ErrorCode.CHAPTER_NOT_FOUND);
            }
        }
        int nextOrder = bookRecordLinkRepository.findAllByBookIdOrderByOrderAsc(bookId).size();
        BookRecordLink link = BookRecordLink.builder()
                .book(book)
                .spaceRecord(spaceRecord)
                .chapter(chapter)
                .order(nextOrder)
                .build();
        return BookRecordLinkResponse.from(bookRecordLinkRepository.save(link));
    }

    public List<BookRecordLinkResponse> list(String bookId, String userId) {
        bookService.getOwnedBook(bookId, userId);
        return bookRecordLinkRepository.findAllByBookIdOrderByOrderAsc(bookId).stream()
                .map(BookRecordLinkResponse::from)
                .toList();
    }

    @Transactional
    public void delete(String linkId, String userId) {
        BookRecordLink link = bookRecordLinkRepository.findById(linkId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_RECORD_LINK_NOT_FOUND));
        bookService.getOwnedBook(link.getBook().getId(), userId);
        bookRecordLinkRepository.delete(link);
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.space.service.BookRecordLinkServiceTest"`
Expected: `BUILD SUCCESSFUL`, 2개 테스트 통과

- [ ] **Step 7: BookRecordLinkController 구현**

`src/main/java/com/bsbowl/onti/domain/space/controller/BookRecordLinkController.java`

```java
package com.bsbowl.onti.domain.space.controller;

import com.bsbowl.onti.domain.space.dto.BookRecordLinkCreateRequest;
import com.bsbowl.onti.domain.space.dto.BookRecordLinkResponse;
import com.bsbowl.onti.domain.space.service.BookRecordLinkService;
import com.bsbowl.onti.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookRecordLinkController {

    private final BookRecordLinkService bookRecordLinkService;

    public BookRecordLinkController(BookRecordLinkService bookRecordLinkService) {
        this.bookRecordLinkService = bookRecordLinkService;
    }

    @PostMapping("/api/books/{bookId}/record-links")
    public ResponseEntity<ApiResponse<BookRecordLinkResponse>> create(@AuthenticationPrincipal String userId,
                                                                        @PathVariable String bookId,
                                                                        @Valid @RequestBody BookRecordLinkCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bookRecordLinkService.create(bookId, userId, request)));
    }

    @GetMapping("/api/books/{bookId}/record-links")
    public ResponseEntity<ApiResponse<List<BookRecordLinkResponse>>> list(@AuthenticationPrincipal String userId,
                                                                            @PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success(bookRecordLinkService.list(bookId, userId)));
    }

    @DeleteMapping("/api/record-links/{linkId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal String userId,
                                                      @PathVariable String linkId) {
        bookRecordLinkService.delete(linkId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

- [ ] **Step 8: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: BookRecordLink(기록-책 연결) API 구현"
```

---

### Task 6: 계정 설정 (프로필/비밀번호/탈퇴) + User→Book 삭제 cascade

**Files:**
- Modify: `src/main/java/com/bsbowl/onti/domain/user/entity/User.java`
- Modify: `src/main/java/com/bsbowl/onti/domain/user/dto/UserResponse.java`
- Modify: `src/main/java/com/bsbowl/onti/domain/book/entity/Book.java`
- Create: `src/main/java/com/bsbowl/onti/domain/user/dto/UserProfileUpdateRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/user/dto/PasswordChangeRequest.java`
- Create: `src/main/java/com/bsbowl/onti/domain/user/service/UserService.java`
- Create: `src/main/java/com/bsbowl/onti/domain/user/controller/UserController.java`
- Test: `src/test/java/com/bsbowl/onti/domain/user/service/UserServiceTest.java`

**Interfaces:**
- Consumes: 기존 `UserRepository`, `PasswordEncoder` 빈
- Produces: `PATCH /api/users/me`, `PATCH /api/users/me/password`, `DELETE /api/users/me`. `AuthController`/`AuthService`는 이 태스크에서 건드리지 않는다 (읽기 전용 `/api/auth/me`는 그대로 유지, 프로필 수정은 `/api/users/me`로 분리).

- [ ] **Step 1: User 엔티티에 `bio` 필드 추가, Book 엔티티에 User→Book cascade 추가**

`src/main/java/com/bsbowl/onti/domain/user/entity/User.java`를 연다. 기존
`private String avatarUrl;` 필드 바로 아래에 아래 필드를 추가한다:

```java
    @Column(length = 500)
    private String bio;
```

기존 `update` 메서드가 없다면(현재 `User`는 getter만 있고 업데이트 메서드가 없음),
클래스 맨 아래(닫는 `}` 바로 위)에 아래 두 메서드를 추가한다:

```java
    public void updateProfile(String name, String avatarUrl, String bio) {
        if (name != null) this.name = name;
        if (avatarUrl != null) this.avatarUrl = avatarUrl;
        if (bio != null) this.bio = bio;
    }

    public void changePassword(String newEncodedPassword) {
        this.password = newEncodedPassword;
    }
```

`src/main/java/com/bsbowl/onti/domain/book/entity/Book.java`를 연다. 기존
`@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn(name = "user_id", nullable = false)`
바로 아래 줄(필드 선언 `private User user;` 바로 위)에 아래 한 줄을 추가한다:

```java
    @OnDelete(action = OnDeleteAction.CASCADE)
```

즉 최종 형태는:

```java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;
```

파일 상단 import에 아래 두 줄이 없다면 추가한다:

```java
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
```

- [ ] **Step 2: UserResponse에 bio 추가**

`src/main/java/com/bsbowl/onti/domain/user/dto/UserResponse.java`를 연다.
기존 코드가 아래와 같을 것이다:

```java
public record UserResponse(String id, String email, String name, String avatarUrl) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl());
    }
}
```

아래로 교체한다 (`bio` 필드 추가):

```java
public record UserResponse(String id, String email, String name, String avatarUrl, String bio) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl(), user.getBio());
    }
}
```

- [ ] **Step 3: 새 DTO 작성**

`src/main/java/com/bsbowl/onti/domain/user/dto/UserProfileUpdateRequest.java`

```java
package com.bsbowl.onti.domain.user.dto;

public record UserProfileUpdateRequest(String name, String avatarUrl, String bio) {
}
```

`src/main/java/com/bsbowl/onti/domain/user/dto/PasswordChangeRequest.java`

```java
package com.bsbowl.onti.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
}
```

- [ ] **Step 4: 실패하는 UserServiceTest 작성**

`src/test/java/com/bsbowl/onti/domain/user/service/UserServiceTest.java`

```java
package com.bsbowl.onti.domain.user.service;

import com.bsbowl.onti.domain.user.dto.PasswordChangeRequest;
import com.bsbowl.onti.domain.user.dto.UserProfileUpdateRequest;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void updateProfile_appliesOnlyNonNullFields() {
        User user = User.builder().email("a@onti.com").password("x").name("옛이름").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        var response = userService.updateProfile("user-1", new UserProfileUpdateRequest("새이름", null, "소개글"));

        assertThat(response.name()).isEqualTo("새이름");
        assertThat(response.bio()).isEqualTo("소개글");
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsInvalidCurrentPassword() {
        User user = User.builder().email("a@onti.com").password("encoded-old").name("a").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("user-1",
                new PasswordChangeRequest("wrong", "new-password")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CURRENT_PASSWORD);
    }

    @Test
    void changePassword_correctCurrentPassword_encodesAndStoresNewPassword() {
        User user = User.builder().email("a@onti.com").password("encoded-old").name("a").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");

        userService.changePassword("user-1", new PasswordChangeRequest("old", "new-password"));

        assertThat(user.getPassword()).isEqualTo("encoded-new");
    }

    @Test
    void deleteAccount_deletesUser() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        userService.deleteAccount("user-1");

        verify(userRepository).delete(user);
    }
}
```

- [ ] **Step 5: 테스트 실패 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.user.service.UserServiceTest"`
Expected: FAIL (컴파일 에러 — `UserService` 클래스가 아직 없음)

- [ ] **Step 6: UserService 구현**

`src/main/java/com/bsbowl/onti/domain/user/service/UserService.java`

```java
package com.bsbowl.onti.domain.user.service;

import com.bsbowl.onti.domain.user.dto.PasswordChangeRequest;
import com.bsbowl.onti.domain.user.dto.UserProfileUpdateRequest;
import com.bsbowl.onti.domain.user.dto.UserResponse;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse updateProfile(String userId, UserProfileUpdateRequest request) {
        User user = getUser(userId);
        user.updateProfile(request.name(), request.avatarUrl(), request.bio());
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(String userId, PasswordChangeRequest request) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void deleteAccount(String userId) {
        userRepository.delete(getUser(userId));
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
```

- [ ] **Step 7: 테스트 통과 확인**

Run: `./gradlew test --tests "com.bsbowl.onti.domain.user.service.UserServiceTest"`
Expected: `BUILD SUCCESSFUL`, 4개 테스트 통과

- [ ] **Step 8: UserController 구현**

`src/main/java/com/bsbowl/onti/domain/user/controller/UserController.java`

```java
package com.bsbowl.onti.domain.user.controller;

import com.bsbowl.onti.domain.user.dto.PasswordChangeRequest;
import com.bsbowl.onti.domain.user.dto.UserProfileUpdateRequest;
import com.bsbowl.onti.domain.user.dto.UserResponse;
import com.bsbowl.onti.domain.user.service.UserService;
import com.bsbowl.onti.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@AuthenticationPrincipal String userId,
                                                                     @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userId, request)));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal String userId,
                                                              @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@AuthenticationPrincipal String userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

- [ ] **Step 9: 전체 빌드 확인 및 커밋**

Run: `./gradlew build -x test`
Expected: `BUILD SUCCESSFUL`

```bash
git add src
git commit -m "feat: 계정 설정(프로필/비밀번호/탈퇴) API 및 User->Book 삭제 cascade 추가"
```

---

### Task 7: 엔드투엔드 스모크 테스트

**Files:** 없음 (검증 전용 태스크)

**Interfaces:**
- Consumes: Task 1~6에서 만든 전체 Space 도메인 + 계정 설정 API, 그리고 기존(1차 라운드) Book/Chapter API

- [ ] **Step 1: 전체 단위 테스트 실행**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL`, 기존 26개 + 이번 라운드에서 추가한 테스트(Task1: 3, Task2: 2, Task3: 2, Task4: 3, Task5: 2, Task6: 4 = 16개) 총 42개 전후 모두 통과 (정확한 개수는 실행 결과로 확인)

- [ ] **Step 2: PostgreSQL 재기동 (스키마 새로 생성)**

`@OnDelete` cascade는 새로 생성되는 FK 제약에만 반영되므로, 기존 로컬 볼륨을
지우고 새로 띄운다.

Run: `docker compose down -v && docker compose up -d`
Expected: `onti-postgres` 컨테이너가 healthy 상태로 실행

- [ ] **Step 3: 서버 기동**

Run (background): `./gradlew bootRun`
Expected: 콘솔에 `Tomcat started on port 8080` 로그, 에러 없이 기동

- [ ] **Step 4: 전체 흐름을 curl로 확인**

```bash
# 회원가입 + 로그인
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"space-smoke@onti.com","password":"password123","name":"스모크"}'
# 응답의 data.accessToken을 TOKEN으로 저장

TOKEN="<위 응답의 accessToken>"

# 기록 공간 생성
curl -s -X POST http://localhost:8080/api/spaces \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"엄마 이야기","topic":"부모님의 삶","subjectName":"엄마"}'
# 응답의 data.id를 SPACE_ID로 저장

SPACE_ID="<위 응답의 id>"

# 참여자 추가
curl -s -X POST "http://localhost:8080/api/spaces/$SPACE_ID/participants" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"displayName":"엄마","email":"mom@onti.com"}'
# 응답의 data.id를 PARTICIPANT_ID로 저장

PARTICIPANT_ID="<위 응답의 id>"

# 질문 생성
curl -s -X POST "http://localhost:8080/api/spaces/$SPACE_ID/questions" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d "{\"text\":\"가장 행복했던 순간은?\",\"createdByParticipantId\":\"$PARTICIPANT_ID\"}"
# 응답의 data.id를 QUESTION_ID로 저장

QUESTION_ID="<위 응답의 id>"

# 공간 기록 생성 (참여자+질문 연결)
curl -s -X POST "http://localhost:8080/api/spaces/$SPACE_ID/records" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d "{\"type\":\"TEXT\",\"title\":\"결혼식 날\",\"content\":\"내용\",\"authorParticipantId\":\"$PARTICIPANT_ID\",\"answeredQuestionId\":\"$QUESTION_ID\",\"occurredAt\":\"1998년 봄\"}"
# 응답의 data.id를 SPACE_RECORD_ID로 저장

SPACE_RECORD_ID="<위 응답의 id>"

# 이미지 추가
curl -s -X POST "http://localhost:8080/api/space-records/$SPACE_RECORD_ID/images" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"url":"https://example.com/a.jpg","caption":"그날 사진"}'

# 책/챕터 생성 (기존 API)
curl -s -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"우리 가족 이야기"}'
# 응답의 data.id를 BOOK_ID로 저장

BOOK_ID="<위 응답의 id>"

curl -s -X POST "http://localhost:8080/api/books/$BOOK_ID/chapters" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"1장"}'
# 응답의 data.id를 CHAPTER_ID로 저장

CHAPTER_ID="<위 응답의 id>"

# 기록을 책의 챕터에 연결
curl -s -X POST "http://localhost:8080/api/books/$BOOK_ID/record-links" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d "{\"spaceRecordId\":\"$SPACE_RECORD_ID\",\"chapterId\":\"$CHAPTER_ID\"}"

# 연결 목록 확인
curl -s "http://localhost:8080/api/books/$BOOK_ID/record-links" -H "Authorization: Bearer $TOKEN"

# 챕터 삭제 → 연결은 남고 chapterId가 null이 되는지 확인 (SET_NULL)
curl -s -X DELETE "http://localhost:8080/api/chapters/$CHAPTER_ID" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/books/$BOOK_ID/record-links" -H "Authorization: Bearer $TOKEN"

# 책 삭제 → 연결이 사라지는지 확인 (CASCADE)
curl -s -X DELETE "http://localhost:8080/api/books/$BOOK_ID" -H "Authorization: Bearer $TOKEN"

# 프로필 수정
curl -s -X PATCH http://localhost:8080/api/users/me \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"bio":"기록을 좋아하는 사람"}'

# 비밀번호 변경 후 재로그인
curl -s -X PATCH http://localhost:8080/api/users/me/password \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"currentPassword":"password123","newPassword":"newpassword456"}'
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"space-smoke@onti.com","password":"newpassword456"}'

# 회원 탈퇴 → 소유한 공간/기록도 같이 지워지는지(재로그인 실패로 간접 확인,
# 필요하면 docker exec psql로 직접 SELECT해서 확인)
curl -s -X DELETE http://localhost:8080/api/users/me -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"space-smoke@onti.com","password":"newpassword456"}'
```

Expected: 모든 성공 응답이 `{"success":true,...}` 형태. 챕터 삭제 후
`record-links` 목록에서 해당 링크의 `chapterId`가 `null`로 바뀌어 있고
링크 자체는 남아있어야 한다(SET_NULL). 책 삭제 후에는 그 책의 모든 링크가
사라져야 한다(CASCADE, 책이 없으니 `GET .../record-links` 자체가 404).
탈퇴 후 같은 이메일로 로그인 시도하면 `INVALID_CREDENTIALS`(계정이 없으므로)가
나와야 한다.

- [ ] **Step 5: Swagger UI 확인**

Run: `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui.html`
Expected: `200` (또는 200으로 귀결되는 리다이렉트)

- [ ] **Step 6: 서버 종료 및 정리**

bootRun 프로세스를 종료한다. `docker compose down`은 실행하지 않고 컨테이너는
계속 띄워둔다 (다음 로컬 개발에 사용).

- [ ] **Step 7: 발견된 문제 수정 시 커밋**

스모크 테스트 중 실제 버그를 발견해 고쳤다면 커밋한다 (이전 라운드 Task 10처럼
컬럼명 예약어, 인코딩 문제 등 통합 시점에만 드러나는 문제가 있을 수 있다).
문제가 없었다면 커밋 없이 태스크 종료.
