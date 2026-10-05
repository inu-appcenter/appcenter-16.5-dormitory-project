# BR-772 메시지 수정 API — 도메인 설계

---

## 엔티티 / 값 객체

### OpenChatMessage (기존 엔티티 수정)

| 필드 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `editedAt` | `LocalDateTime` | nullable | null = 미수정, non-null = 수정됨 |

추가 도메인 메서드:
```java
public void updateContent(String newContent) {
    this.content = newContent;
    this.editedAt = LocalDateTime.now();
}
```

`createForTest` 시그니처에 `editedAt` 파라미터를 추가하거나, 기존 시그니처를 유지하고 별도 setter 없이 테스트에서 `updateContent()`를 호출하는 방식으로 처리한다.

---

### ResponseOpenChatMessageEditEventDto (신규 — WebSocket 이벤트)

WebSocket topic `/sub/openchat/{roomId}/edit`으로 broadcast되는 경량 DTO.

| 필드 | 타입 |
|------|------|
| `messageId` | `Long` |
| `roomId` | `Long` |
| `content` | `String` |
| `editedAt` | `LocalDateTime` |

---

### RequestEditOpenChatMessageDto (신규 — REST 요청)

| 필드 | 타입 | 제약 |
|------|------|------|
| `content` | `String` | `@NotBlank` |

---

## 애그리거트 경계

- `OpenChatMessage`는 독립 애그리거트 루트다. `roomId`, `senderId`는 ID 참조로만 보유한다.
- 서비스 레이어에서 `openChatRoomRepository`로 `OpenChatRoom`을 별도 조회해 `lastMessage`를 갱신한다.

---

## 연관관계

변경 없음. 기존 필드가 ID 참조(`roomId`, `senderId`)를 사용하므로 `@ManyToOne` 추가 불필요.

---

## DB 스키마 변경

```sql
ALTER TABLE open_chat_message
    ADD COLUMN edited_at DATETIME(6) NULL;
```

> 인덱스 불필요 — 수정 여부는 단건 조회 후 판단, `edited_at`으로 직접 조회하는 쿼리 없음.

---

## 도메인 계층 구조

```
domain/openChat/
├── controller/
│   ├── OpenChatMessageController.java          [수정] PATCH 엔드포인트 추가
│   └── OpenChatMessageApiSpecification.java    [수정] PATCH 선언 추가
├── service/
│   └── OpenChatMessageService.java             [수정] editMessage() 추가
├── repository/                                 [변경 없음]
│   ├── OpenChatMessageRepository.java
│   ├── OpenChatMessageQuerydslRepository.java
│   └── OpenChatMessageQuerydslRepositoryImpl.java
├── entity/
│   └── OpenChatMessage.java                    [수정] editedAt 필드 + updateContent() 추가
├── dto/
│   ├── request/
│   │   └── RequestEditOpenChatMessageDto.java  [신규]
│   └── response/
│       ├── ResponseOpenChatMessageDto.java     [수정] isEdited, editedAt 필드 추가
│       └── ResponseOpenChatMessageEditEventDto.java  [신규]
└── enums/                                      [변경 없음]
```

```
global/exception/
└── ErrorCode.java  [수정] 22039, 22040 추가
```

---

## 클래스별 변경 상세

### `OpenChatMessage` — 수정

- `private LocalDateTime editedAt;` 필드 추가 (nullable, no annotation)
- `updateContent(String newContent)` 메서드 추가
- `createForTest(...)` 오버로드 추가: `editedAt` 파라미터 포함 버전

### `ResponseOpenChatMessageDto` — 수정

- `private boolean isEdited;` 필드 추가
- `private LocalDateTime editedAt;` 필드 추가
- 기존 `from(...)` 팩토리 메서드들에 `.isEdited(message.getEditedAt() != null).editedAt(message.getEditedAt())` 추가

### `RequestEditOpenChatMessageDto` — 신규

```java
@Getter @NoArgsConstructor
public class RequestEditOpenChatMessageDto {
    @NotBlank
    private String content;
}
```

### `ResponseOpenChatMessageEditEventDto` — 신규

```java
@Getter @Builder
public class ResponseOpenChatMessageEditEventDto {
    private Long messageId;
    private Long roomId;
    private String content;
    private LocalDateTime editedAt;

    public static ResponseOpenChatMessageEditEventDto from(OpenChatMessage message) { ... }
}
```

### `OpenChatMessageService.editMessage(...)` — 신규 메서드

```
editMessage(Long requesterId, Long roomId, Long messageId, RequestEditOpenChatMessageDto dto)
→ ResponseOpenChatMessageDto
```

처리 순서:
1. `findById(messageId)` → 없으면 `OPEN_CHAT_MESSAGE_NOT_FOUND`
2. `message.getSenderId() != requesterId` → `OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER`
3. `message.getType() != TEXT` → `OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE`
4. `message.isDeleted()` → `OPEN_CHAT_MESSAGE_ALREADY_DELETED`
5. `dto.getContent().equals(message.getContent())` → `OPEN_CHAT_MESSAGE_CONTENT_UNCHANGED`
6. `message.updateContent(dto.getContent())`
7. `openChatMessageRepository.save(message)`
8. `findLatestMessageIdByRoomId(roomId)` == `messageId` 이면 `room.updateLastMessage(content, editedAt)`
9. `messagingTemplate.convertAndSend("/sub/openchat/" + roomId + "/edit", ResponseOpenChatMessageEditEventDto.from(message))`
10. 응답 DTO 조립 후 반환 (`unreadCount` = 0 고정, 수정은 읽음 상태 변경 없음)

### `OpenChatMessageController` — 수정

```java
@PatchMapping("/{roomId}/messages/{messageId}")
public ResponseEntity<ResponseOpenChatMessageDto> editMessage(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable Long roomId,
        @PathVariable Long messageId,
        @RequestBody @Valid RequestEditOpenChatMessageDto dto) {
    return ResponseEntity.ok(openChatMessageService.editMessage(user.getId(), roomId, messageId, dto));
}
```

### `ErrorCode` — 수정

```java
OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE(BAD_REQUEST, 22039, "[OpenChat] 텍스트 메시지만 수정할 수 있습니다."),
OPEN_CHAT_MESSAGE_CONTENT_UNCHANGED(BAD_REQUEST, 22040, "[OpenChat] 기존 내용과 동일한 수정 요청입니다."),
```

---

## 레포지토리 변경

없음. `findLatestMessageIdByRoomId(Long roomId)`가 `OpenChatMessageQuerydslRepositoryImpl`에 이미 구현되어 있다.

---

## 비목표

- 수정 이력 테이블 신규 생성 없음
- `ReplySourceStatus`에 `EDITED` 값 추가 없음
- FCM 발송 없음
- `OpenChatRoom` 조회 시 없는 경우 예외 처리 없음 (sendMessage와 동일 패턴 — null 이면 lastMessage 갱신 생략)
