# BR-764 부모 오픈채팅 카드 관리 — 도메인 설계

---

## 엔티티 / 값 객체

### OpenChatRoom (수정)
기존 엔티티에 필드 2개 추가.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `originRoomId` | `Long` | nullable | DERIVED 방이 파생된 부모 방 ID |
| `transitionCount` | `int` | not null, default 0 | CLOSED→OPEN 전이 횟수. 재모집 카드 중복 방지 기준 |

도메인 메서드 추가:
- `incrementTransitionCount()` — `transitionCount += 1`. CLOSED→OPEN 시 Service에서 호출.
- `setOriginRoomId(Long id)` — 파생 방 생성 팩토리에서만 호출하도록 package-private가 아닌 package level 정적 팩토리에서 직접 설정.

### OpenChatMessage (수정)
기존 엔티티에 필드 1개 추가.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `duplKey` | `String(100)` | nullable, unique | 재모집 카드 중복 방지 키. 형식: `{derivedRoomId}_reopen_{transitionCount}` |

팩토리 메서드 추가:
- `createReopenCard(Long roomId, Long senderId, String content, String duplKey)`

---

## 애그리거트 경계

- `OpenChatRoom`이 루트. `originRoomId`는 부모 방을 **ID 참조**로만 가리킨다. 객체 참조(@ManyToOne) 없음.
- `OpenChatMessage`가 독립 루트. `duplKey` 필드는 `OpenChatMessage` 애그리거트 내부 속성.

---

## 연관관계

신규 연관관계 없음. `originRoomId`는 `Long` 타입 ID 참조.

```
OpenChatRoom (DERIVED)
  originRoomId → Long (부모 방 ID, 객체 참조 없음)
```

---

## DB 스키마 변경

```sql
-- open_chat_room 테이블에 컬럼 추가
ALTER TABLE open_chat_room
    ADD COLUMN origin_room_id    BIGINT       NULL,
    ADD COLUMN transition_count  INT          NOT NULL DEFAULT 0;

-- open_chat_message 테이블에 컬럼 추가
ALTER TABLE open_chat_message
    ADD COLUMN dupl_key VARCHAR(100) NULL,
    ADD UNIQUE INDEX uq_open_chat_message_dupl_key (dupl_key);
```

---

## 도메인 계층 구조

```
domain/openChat/
├── controller/
│   └── (변경 없음 — BR-759 컨트롤러 재사용)
├── service/
│   ├── OpenChatRoomService.java        [수정] updateRecruitmentStatus() 후처리 추가
│   └── OpenChatMessageService.java     [수정] 2개 메서드 추가
├── repository/
│   └── OpenChatMessageRepository.java  [수정] existsByDuplKey() 추가
├── entity/
│   ├── OpenChatRoom.java               [수정] originRoomId, transitionCount 필드 + 메서드
│   └── OpenChatMessage.java            [수정] duplKey 필드 + createReopenCard() 팩토리
├── dto/
│   └── response/
│       ├── ResponseOpenChatMessageDto.java        [수정] linkedRoomRecruitmentStatus 필드 추가
│       └── ResponseRecruitmentStatusEventDto.java [신규] WebSocket 이벤트 DTO
└── enums/
    └── OpenChatMessageType.java        [수정] REOPEN_CARD 추가
```

### 수정: OpenChatRoomService.updateRecruitmentStatus()

현재 흐름:
1. 방·유저 조회, 권한 검증
2. `room.updateRecruitmentStatus(status, actorId)` — 상태 전이

추가 흐름 (이번 BR):
3. `room.getOriginRoomId()`가 null이면 종료
4. `openChatMessageService.sendRecruitmentStatusEvent(originRoomId, roomId, status)` 호출
5. `status == OPEN`이면:
   a. `room.incrementTransitionCount()`
   b. `openChatMessageService.sendReopenCardMessage(originRoomId, actorId, roomId, ...)` 호출

### 신규: OpenChatMessageService 메서드

```java
// 1) WebSocket 이벤트 전송 (STOMP 별도 토픽)
public void sendRecruitmentStatusEvent(
    Long parentRoomId, Long derivedRoomId, OpenChatRoomRecruitmentStatus status)
// → messagingTemplate.convertAndSend(
//       "/sub/openchat/{parentRoomId}/recruitment-status",
//       ResponseRecruitmentStatusEventDto.of(derivedRoomId, status))

// 2) 재모집 카드 메시지 생성 + 브로드캐스트
public void sendReopenCardMessage(
    Long parentRoomId, Long actorId, Long derivedRoomId,
    String roomName, String description, int maxParticipants, int transitionCount)
// duplKey = "{derivedRoomId}_reopen_{transitionCount}"
// existsByDuplKey 확인 → 이미 존재하면 return (no-op)
// OpenChatMessage.createReopenCard(parentRoomId, actorId, jsonContent, duplKey) 저장
// messagingTemplate.convertAndSend("/sub/openchat/{parentRoomId}", responseDto)
```

### 신규: ResponseRecruitmentStatusEventDto

```java
@Getter
public class ResponseRecruitmentStatusEventDto {
    private final Long derivedRoomId;
    private final OpenChatRoomRecruitmentStatus recruitmentStatus;

    public static ResponseRecruitmentStatusEventDto of(
        Long derivedRoomId, OpenChatRoomRecruitmentStatus status) { ... }
}
```

### 수정: ResponseOpenChatMessageDto

`linkedRoomRecruitmentStatus: OpenChatRoomRecruitmentStatus` 필드 추가.
기존 `linkedRoomRecruitmentClosed` boolean 유지. `fromRoomLink()` 오버로드에 `recruitmentStatus` 파라미터 추가.
`REOPEN_CARD` 타입도 `fromRoomLink()`와 동일한 빌더 경로로 생성한다.

---

## 비목표

- `OpenChatRoom`에 부모 방 객체 참조(`@ManyToOne`) 추가 — ID 참조로 충분
- `OpenChatParticipant` 테이블 변경 없음
- 새 Controller·엔드포인트 추가 없음 (BR-759 컨트롤러에 Service 후처리만 연결)
- 이벤트 전송 실패 시 재시도·트랜잭션 outbox 패턴
- 기존 ROOM_LINK 메시지의 content JSON 구조 변경
- FCM 알림 로직 추가
