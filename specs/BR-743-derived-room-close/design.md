# BR-743 파생 톡방 마감 — 도메인 설계

명세: [`requirement.md`](./requirement.md)

## 엔티티 / 값 객체

### `OpenChatRoom` (기존 엔티티 확장)
기존 필드 유지. 아래 3개 필드 추가.

| 필드 | 자바 타입 | JPA 매핑 | 제약 | 설명 |
|---|---|---|---|---|
| `recruitmentClosed` | `boolean` | `@Column(nullable = false)` | not null, default `false` | 모집 마감 여부. 단방향(true 이후 되돌릴 수 없음). |
| `closedAt` | `LocalDateTime` | `@Column` | nullable | 마감 처리 시각. `recruitmentClosed=true` 인 경우에만 값 존재. |
| `closedBy` | `Long` | `@Column` | nullable | 마감 트리거 유저(방장 또는 관리자)의 `userId`. |

**도메인 메서드 (신규):**
```java
public void closeRecruitment(Long actorId) {
    if (this.roomType != OpenChatRoomType.DERIVED) {
        throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_DERIVED);
    }
    if (this.recruitmentClosed) {
        throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_ALREADY_CLOSED);
    }
    this.recruitmentClosed = true;
    this.closedAt = LocalDateTime.now();
    this.closedBy = actorId;
}
```

- 도메인이 타입·중복 검증을 책임진다. 서비스는 조회·인가·호출 순서만 담당.
- `reopen()` 계열 public 메서드는 노출하지 않는다 (단방향 보장).
- 기존 `create()` / `createDerived()` 팩토리는 그대로 유지. `recruitmentClosed` 필드는 기본값 `false` 로 초기화된다.

### 테스트용 팩토리 (필요 시 추가)
`OpenChatRoom` 에 `createForTest(...)` 오버로드 또는 인자로 `recruitmentClosed`, `closedAt`, `closedBy` 를 받는 별도 팩토리를 추가한다. Fixture는 실제 객체를 반환한다 (Mockito.mock 금지 — `antipatterns-test.md`).

---

## 애그리거트 경계

- 애그리거트 루트: `OpenChatRoom`. 마감 상태(3개 필드)는 이 애그리거트 내부 상태다.
- `OpenChatParticipant` 는 별도 애그리거트로 취급(이미 그렇게 운용 중). 마감 상태 전이는 참여자 애그리거트를 건드리지 않는다.
- `OpenChatMessage` 는 마감 상태를 참조만 한다 (`ROOM_LINK` 메시지 응답 시). 메시지 엔티티에 마감 정보를 저장하지 않는다 — 스냅샷이 아니라 조회 시점 실시간 상태.

---

## 연관관계

이 BR에서 새 연관관계는 없다.
- `closedBy` 는 `Long userId` 로 저장한다. `User` 와의 `@ManyToOne` 매핑은 하지 않는다. 이유:
  - 감사 성격의 참조라 실시간 조인 필요 없음
  - 기존 `createdBy` 도 동일한 방식(`Long`, FK 매핑 없음) → 스타일 일관성

기존 `OpenChatRoom` 의 fetch 전략은 유지. 이 BR에서 EAGER/LAZY 변경 없음.

---

## DB 스키마 변경

테이블: `open_chat_room` (기존)

컬럼 3개 추가:
```sql
ALTER TABLE open_chat_room
    ADD COLUMN recruitment_closed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN closed_at DATETIME(6) NULL,
    ADD COLUMN closed_by BIGINT NULL;
```

- 인덱스: 추가하지 않는다. 마감 여부로 목록/검색을 필터링하지 않기로 결정했기 때문(requirement 비목표 참조). 향후 필터가 생기면 그때 인덱스 추가.
- 기존 데이터: `recruitment_closed = false` 로 자연 채워짐. 별도 백필 스크립트 불필요.

---

## 도메인 계층 구조

```
domain/openChat/
├── controller/
│   ├── OpenChatRoomController.java             [수정] PATCH /{roomId}/close-recruitment 단일 엔드포인트 (방장·관리자 공용)
│   └── OpenChatRoomApiSpecification.java       [수정] 새 엔드포인트 Swagger 어노테이션
├── service/
│   ├── OpenChatRoomService.java                [수정] closeRecruitment(actorId, roomId) 단일 메서드 (role 분기), joinRoom 가드
│   └── OpenChatMessageService.java             [수정] toRoomLinkDto batch 조회 + linkedRoomRecruitmentClosed 세팅
├── repository/
│   └── OpenChatRoomRepository.java             [기존 유지] `findAllById` 사용 (Spring Data JPA 기본 제공)
├── entity/
│   └── OpenChatRoom.java                       [수정] 필드 3개 + closeRecruitment(...) 도메인 메서드
├── dto/
│   ├── request/  (없음 — path variable만)
│   └── response/
│       ├── ResponseOpenChatRoomDetailDto.java  [수정] recruitmentClosed 필드 추가
│       ├── ResponseOpenChatRoomDto.java        [수정] recruitmentClosed 필드 추가
│       └── ResponseOpenChatMessageDto.java     [수정] linkedRoomRecruitmentClosed 필드 추가
└── enums/  (변경 없음)
```

전역:
```
global/exception/
└── ErrorCode.java                               [수정] 22029/22030/22031 추가
```

**신규 파일: 없음.** 모두 기존 파일 확장.

---

## 계층별 책임 상세

### Controller
- `OpenChatRoomController.closeRecruitment(...)`: `PATCH /open-chat-rooms/{roomId}/close-recruitment`.
  - 요청 본문 없음. `@AuthenticationPrincipal CustomUserDetails user` 로 요청자 식별.
  - 방장·관리자 공용 단일 엔드포인트 — 관리자용 별도 경로 없음(`kickParticipant` 패턴 준용).
  - 반환: `ResponseEntity<Void>` `noContent()`.

컨트롤러는 서비스 메서드 호출만 담당. 검증 로직 넣지 않는다(`antipatterns.md` — Controller 비즈니스 로직 금지).

### Service — `OpenChatRoomService`
```java
@Transactional
public void closeRecruitment(Long actorId, Long roomId) {
    OpenChatRoom room = openChatRoomRepository.findById(roomId)
        .orElseThrow(() -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

    User actor = userRepository.findById(actorId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    if (actor.getRole() != Role.ROLE_ADMIN
            && !Objects.equals(room.getCreatedBy(), actorId)) {
        throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_FORBIDDEN);
    }

    room.closeRecruitment(actorId);  // NOT_DERIVED / ALREADY_CLOSED 는 도메인에서
}
```

Role-based 분기는 `kickParticipant` 와 동일한 패턴. 관리자는 방장 여부와 무관하게 통과, 일반 유저는 방장일 때만 통과.

`joinRoom(userId, roomId, password)` 가드 (기존 메서드 line 302 수정):
- **위치**: 참여자 여부 검사(`existsByRoomIdAndUserId`) 통과 후, **비참여자 처리 블록 최상단**에서 확인. 이미 참여자는 마감과 무관하게 정상 진입.
- **검증 순서** (기존 대비 앞으로 이동):
  1. 방 조회 (`findByIdWithLock`)
  2. 참여자 여부 → 참여자면 그대로 반환 (기존)
  3. **NEW: `room.isRecruitmentClosed()` → true 이면 `OPEN_CHAT_ROOM_CLOSED_FOR_JOIN`**
  4. 유저 조회, 비밀번호, 스코프, 정원 (기존)

```java
if (room.isRecruitmentClosed()) {
    throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_CLOSED_FOR_JOIN);
}
```

`toDetailDto(...)` 수정: 응답 DTO에 `recruitmentClosed` 매핑.
`buildOpenChatDtos(...)` / `ResponseOpenChatRoomDto.from(...)` 도 새 필드를 채워야 함.

### Service — `OpenChatMessageService.toRoomLinkDto(...)`
기존은 단일 메시지 단위로 `linkedRoomId` 등을 세팅. 마감 상태를 함께 노출하려면 **부모 톡방 메시지 목록 조회 지점에서 batch 조회**가 필요하다.

수정 지점: `getMessages(...)` 흐름의 `messages.stream()...map(...)` 앞단.
- ROOM_LINK 메시지의 `linkedRoomId` 를 수집 → `openChatRoomRepository.findAllById(linkedRoomIds)` → `Map<Long, OpenChatRoom>` 구성.
- `toRoomLinkDto(msg, nickname, unreadCount, linkedRoomMap)` 시그니처로 확장. Map 조회 결과가 없으면(파생방 삭제됨) `linkedRoomRecruitmentClosed = false`, 다른 링크 필드는 기존과 동일하게 원본 메시지 본문에서 파싱.
- N+1 방지 (`antipatterns-jpa.md` §"N+1 쿼리"). 방 목록 응답에 대해 `imageUrlsMap` 이 이미 사용하는 batch 패턴을 그대로 따른다.

전송 지점(`sendRoomLinkMessage`)에서는 마감 정보를 저장할 필요 없다 — 조회 시점에 채운다.

### DTO 응답 필드
- `ResponseOpenChatRoomDto.recruitmentClosed: boolean` — 기본 false. `from(room, ...)` 팩토리에서 `room.isRecruitmentClosed()` 로 세팅.
- `ResponseOpenChatRoomDetailDto.recruitmentClosed: boolean` — 위와 동일.
- `ResponseOpenChatMessageDto.linkedRoomRecruitmentClosed: Boolean` — nullable로 두어 ROOM_LINK 외 메시지 타입 응답에는 자연스럽게 null 세팅되도록 함. ROOM_LINK 응답만 명시적으로 값을 채운다.

### 새 ErrorCode (`ErrorCode.java`)
```java
OPEN_CHAT_ROOM_NOT_DERIVED(BAD_REQUEST, 22029, "[OpenChat] 파생 톡방이 아닌 방은 마감할 수 없습니다."),
OPEN_CHAT_ROOM_ALREADY_CLOSED(CONFLICT, 22030, "[OpenChat] 이미 마감된 채팅방입니다."),
OPEN_CHAT_ROOM_CLOSED_FOR_JOIN(CONFLICT, 22031, "[OpenChat] 마감된 채팅방에는 참여할 수 없습니다."),
```

기존 재사용: `OPEN_CHAT_ROOM_NOT_FOUND`(22001), `OPEN_CHAT_ROOM_FORBIDDEN`(22002).

---

## 트랜잭션 · 락 정책

- `closeRecruitmentByOwner` / `closeRecruitmentByAdmin` 는 단순 상태 전이 → 기본 `@Transactional` (readOnly=false). `findByIdWithLock` 을 쓸 필요는 없다:
  - 이미 마감된 방을 다시 마감해도 도메인 메서드가 예외를 던지므로, 두 요청이 동시에 성공적으로 상태 전이할 가능성은 없다. 두 요청 중 하나는 `OPEN_CHAT_ROOM_ALREADY_CLOSED` 로 실패한다.
  - JPA optimistic lock(현재 없음)이나 pessimistic lock 도입은 이 BR의 스코프 밖.
- `joinRoom` 은 이미 `findByIdWithLock` 을 쓰고 있음 → 마감 가드는 그 락 내부에서 확인되므로 concurrent join 대비 이미 안전.
- 조회 계열 서비스(`getRooms`, 메시지 목록 등)는 기존대로 `readOnly = true` 유지.

---

## 비목표

명세의 비목표를 재확인한다. 이 설계에서 의도적으로 넣지 않은 것:

- **reopen 도메인 메서드 / API 없음** — `OpenChatRoom` 에 `reopenRecruitment()` 를 만들지 않는다. 단방향 보장.
- **`recruitment_closed` 인덱스 없음** — 조회 필터가 없으므로 인덱스 없음.
- **`closedBy` FK 매핑 안 함** — `Long` 필드로 단순 저장.
- **마감 시 시스템 메시지 / FCM 발송 로직 없음** — `OpenChatNotificationService`, `OpenChatMessageService.sendSystemMessage` 는 이 BR에서 호출하지 않는다.
- **초대 도메인 연동 안 함** — main 코드에 `OpenChatInvitation` 엔티티/서비스가 없다. 나중에 도입되면 그때 별도 BR로 마감 가드 추가.
- **자동 마감 스케줄러 / deadline 필드 없음** — `@Scheduled` 코드 추가 없음.
- **관리자 마감을 위한 `/admin/**` 별도 엔드포인트 없음** — `kickParticipant` 와 동일하게 단일 엔드포인트에서 role 분기로 처리한다.
- **`OpenChatRoom.update(...)` 시그니처 변경 없음** — 마감 필드는 별도 도메인 메서드로만 변경 가능하게 함(`update()` 로 실수로 마감 상태를 되돌릴 수 없도록).
