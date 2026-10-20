# BR-769 도메인 설계 — 모든 채팅방 답장 데이터 처리

---

## 엔티티 / 값 객체

### OpenChatMessage — 추가 필드

| 필드명 | JPA 타입 | DB 타입 | nullable | 기본값 |
|---|---|---|---|---|
| `isDeleted` | `boolean` | `BIT(1)` | No | `false` |
| `replyToMessageId` | `Long` | `BIGINT` | Yes | null |
| `replyToMessageType` | `OpenChatMessageType` | `VARCHAR(30)` | Yes | null |
| `replyToSenderId` | `Long` | `BIGINT` | Yes | null |
| `replyToRoomId` | `Long` | `BIGINT` | Yes | null |
| `replyToRoomType` | `ChatRoomType` | `VARCHAR(20)` | Yes | null |
| `replyToDerivedRoomId` | `Long` | `BIGINT` | Yes | null |

추가할 도메인 메서드:
- `softDelete()` — `isDeleted = true`
- `attachReply(Long replyToMessageId, OpenChatMessageType replyToMessageType, Long replyToSenderId, Long replyToRoomId, ChatRoomType replyToRoomType, Long replyToDerivedRoomId)` — reply 필드 일괄 설정

### RoommateChattingChat — 추가 필드

| 필드명 | JPA 타입 | DB 타입 | nullable | 기본값 |
|---|---|---|---|---|
| `isDeleted` | `boolean` | `BIT(1)` | No | `false` |
| `replyToMessageId` | `Long` | `BIGINT` | Yes | null |
| `replyToSenderId` | `Long` | `BIGINT` | Yes | null |
| `replyToRoomId` | `Long` | `BIGINT` | Yes | null |

- `replyToRoomType`은 항상 `ChatRoomType.ROOMMATE`이므로 DB 저장 없이 DTO 조립 시 상수 주입.
- 추가할 도메인 메서드:
  - `softDelete()`
  - `attachReply(Long replyToMessageId, Long replyToSenderId, Long replyToRoomId)`

### 신규 enum — `shared/enums/ReplySourceStatus`

```java
public enum ReplySourceStatus {
    NORMAL, DELETED, NOT_FOUND, RECRUITING, RECRUITMENT_CLOSED
}
```

### 기존 enum 수정 — `shared/enums/ChatRoomType`

```java
public enum ChatRoomType {
    GROUP_ORDER,   // 기존
    ROOMMATE,      // 신규
    OPEN,          // 신규
    DERIVED        // 신규
}
```

### 신규 DTO — `shared/dto/ReplySourceDto`

```java
@Getter
@Builder
public class ReplySourceDto {
    private Long replyToMessageId;
    private ReplySourceStatus status;
    private Long replyToSenderId;
    private String replyToSenderNickname;   // DELETED·NOT_FOUND이면 null
    private String contentPreview;           // DELETED·NOT_FOUND이면 null, 최대 100자
    private ChatRoomType replyToRoomType;
    private Long replyToRoomId;
    private Long replyToDerivedRoomId;       // RECRUITING·RECRUITMENT_CLOSED이면 포함, 그 외 null
}
```

---

## 애그리거트 경계

- `OpenChatMessage`는 독립 애그리거트. reply 메타데이터는 자신의 필드로 소유.
- `RoommateChattingChat`은 독립 애그리거트. 동일.
- 답장 조회 시 원본 메시지 참조는 ID 참조(`replyToMessageId`)로만 유지. 객체 참조(`@ManyToOne`) 사용 금지.
  - 이유: 원본 메시지가 다른 엔티티 타입(`RoommateChattingChat` vs `OpenChatMessage`)일 수 있으며, NOT_FOUND 상태를 처리해야 하기 때문.

---

## 연관관계

이번 BR에서 **신규 연관관계(@ManyToOne/@OneToMany)는 추가하지 않는다.**
모든 참조는 `Long` ID 필드로 유지한다. 필요한 조인은 서비스 계층에서 batch 조회로 처리.

---

## DB 스키마 변경

### `open_chat_message` 컬럼 추가

```sql
ALTER TABLE open_chat_message
    ADD COLUMN is_deleted         BIT(1)      NOT NULL DEFAULT 0,
    ADD COLUMN reply_to_message_id   BIGINT,
    ADD COLUMN reply_to_message_type VARCHAR(30),
    ADD COLUMN reply_to_sender_id    BIGINT,
    ADD COLUMN reply_to_room_id      BIGINT,
    ADD COLUMN reply_to_room_type    VARCHAR(20),
    ADD COLUMN reply_to_derived_room_id BIGINT;
```

### `roommate_chatting_chat` 컬럼 추가

```sql
ALTER TABLE roommate_chatting_chat
    ADD COLUMN is_deleted           BIT(1) NOT NULL DEFAULT 0,
    ADD COLUMN reply_to_message_id  BIGINT,
    ADD COLUMN reply_to_sender_id   BIGINT,
    ADD COLUMN reply_to_room_id     BIGINT;
```

인덱스: 추가 없음. `replyToMessageId`로 원본을 조회할 때 PK(id) 기준으로 `findAllById`를 사용하므로 별도 인덱스 불필요.

---

## 도메인 계층 구조

### 새로 생성하는 클래스

```
shared/
├── enums/
│   └── ReplySourceStatus.java         [NEW]
└── dto/
    └── ReplySourceDto.java             [NEW]

global/exception/
└── ErrorCode.java                      [MODIFY — 에러코드 추가]
```

### openChat 도메인 — 수정

```
domain/openChat/
├── entity/
│   └── OpenChatMessage.java            [MODIFY — isDeleted + reply 필드 + softDelete/attachReply 메서드]
├── dto/
│   ├── request/
│   │   └── RequestOpenChatMessageDto.java   [MODIFY — replyToMessageId 추가]
│   └── response/
│       └── ResponseOpenChatMessageDto.java  [MODIFY — replySource 필드 추가 + from() 오버로드]
├── service/
│   └── OpenChatMessageService.java     [MODIFY — sendMessage/sendImageMessage에 reply 처리, getMessages에 replySource 조립, deleteMessage 추가]
└── controller/
    ├── OpenChatMessageController.java  [MODIFY — DELETE /{roomId}/messages/{messageId} 엔드포인트 추가]
    └── OpenChatMessageApiSpecification.java [MODIFY — deleteMessage API 명세 추가]
```

### roommate 도메인 — 수정

```
domain/roommate/
├── entity/
│   └── RoommateChattingChat.java       [MODIFY — isDeleted + reply 필드 + softDelete/attachReply 메서드]
├── dto/
│   ├── request/
│   │   └── RequestRoommateChatDto.java      [MODIFY — replyToMessageId 추가]
│   └── response/
│       └── ResponseRoommateChatDto.java     [MODIFY — replySource 필드 추가 + entityToDto() 시그니처 변경]
├── service/
│   └── RoommateChattingChatService.java [MODIFY — sendChat에 reply 처리, getChatList에 replySource 조립, deleteChat 추가]
└── controller/
    ├── RoommateChattingChatController.java  [MODIFY — DELETE /messages/{chatId} 엔드포인트 추가]
    └── RoommateChatApiSpecification.java    [MODIFY — deleteChat API 명세 추가]
```

### 신규 ErrorCode 항목

```
// OpenChat — reply (22033~22038)
OPEN_CHAT_MESSAGE_ALREADY_DELETED          (BAD_REQUEST,  22033)
OPEN_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE       (BAD_REQUEST,  22034)
OPEN_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM    (BAD_REQUEST,  22035)
OPEN_CHAT_NESTED_REPLY_NOT_ALLOWED         (BAD_REQUEST,  22036)
OPEN_CHAT_DERIVED_ROOM_ID_PARSE_FAILED     (INTERNAL_SERVER_ERROR, 22037)
OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER        (FORBIDDEN,    22038)

// RoommateChat — reply (10005~10010)
ROOMMATE_CHAT_MESSAGE_NOT_FOUND            (NOT_FOUND,    10005)
ROOMMATE_CHAT_MESSAGE_ALREADY_DELETED      (BAD_REQUEST,  10006)
ROOMMATE_CHAT_REPLY_NOT_ALLOWED_FOR_TYPE   (BAD_REQUEST,  10007)
ROOMMATE_CHAT_REPLY_TARGET_NOT_IN_SAME_ROOM(BAD_REQUEST,  10008)
ROOMMATE_CHAT_NESTED_REPLY_NOT_ALLOWED     (BAD_REQUEST,  10009)
ROOMMATE_CHAT_MESSAGE_NOT_OWNED_BY_USER    (FORBIDDEN,    10010)
```

---

## 서비스 계층 핵심 로직

### 답장 전송 공통 흐름 (sendMessage / sendChat)

```
1. replyToMessageId가 null → 일반 메시지 저장 (기존 흐름)
2. replyToMessageId가 있을 때:
   a. 원본 메시지 조회 → 없으면 MESSAGE_NOT_FOUND
   b. 원본.isDeleted = true → ALREADY_DELETED
   c. 원본.roomId ≠ 현재 roomId → REPLY_TARGET_NOT_IN_SAME_ROOM
   d. 원본이 답장 메시지(replyToMessageId != null) → NESTED_REPLY_NOT_ALLOWED
   e. 원본 type이 답장 불가 유형 → REPLY_NOT_ALLOWED_FOR_TYPE
   f. 원본이 REOPEN_CARD → content JSON에서 derivedRoomId 파싱
      실패 시 → DERIVED_ROOM_ID_PARSE_FAILED
   g. message.attachReply(…) 호출 후 저장
```

### ReplySource 조립 흐름 (getMessages / getChatList)

```
1. 페이지 내 답장 메시지(replyToMessageId != null) 수집
2. 해당 replyToMessageId 목록으로 원본 메시지 batch 조회
   (openChatMessageRepository.findAllById / roommateChattingChatRepository.findAllById)
3. REOPEN_CARD 원본에 대해 replyToDerivedRoomId 목록 추출
   → openChatRoomRepository.findAllById(derivedRoomIds) batch 조회
4. 원본 senderId 목록으로 닉네임 batch 조회 (userRepository.findAllById)
5. 각 답장 메시지에 대해 ReplySourceDto 조립:
   - 원본이 Map에 없음 → status = NOT_FOUND
   - 원본 isDeleted = true → status = DELETED (senderId/nickname/preview = null)
   - 원본 type = REOPEN_CARD → 파생 톡방 recruitmentClosed 여부로 RECRUITING / RECRUITMENT_CLOSED
   - 그 외 → status = NORMAL, contentPreview = content.substring(0, min(100, length))
```

### 메시지 soft-delete (deleteMessage / deleteChat)

```
1. 메시지 조회 → 없으면 MESSAGE_NOT_FOUND
2. message.senderId(또는 member.id) ≠ 요청자 userId → NOT_OWNED_BY_USER
3. message.isDeleted = true 이면 이미 삭제됨 (ALREADY_DELETED 또는 멱등 처리)
4. message.softDelete() 호출
```

---

## 비목표

requirement.md의 비목표를 그대로 준수:

- 중첩 답장 설계 없음
- FCM 알림 변경 없음
- PERSONAL 타입 OpenChatRoom 답장 처리 없음
- `RoommateChattingChat`에 별도 메시지 유형 enum 추가 없음 (`messageType` 필드 신규 추가 없음)
- 답장 조회 시 N+1 방지를 위해 연관관계 객체 참조 추가 없음 (batch ID 조회로 대체)
