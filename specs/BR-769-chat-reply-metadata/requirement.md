# BR-769 모든 채팅방 답장 데이터 처리

## 기능 요약

오픈채팅(OPEN), 파생톡방(DERIVED), 룸메톡방(roommate) 세 채팅 도메인에 답장(reply) 기능을 추가한다.
답장 메시지에 원본 메시지 메타데이터를 저장하고, 조회 시 원본의 현재 상태를 함께 반환한다.
메시지 soft-delete도 이 BR에서 함께 구현한다.

---

## 동작 명세

### 답장 전송

1. 클라이언트가 STOMP(또는 REST) 메시지 전송 시 `replyToMessageId`(nullable)를 포함해 전송한다.
2. `replyToMessageId`가 존재하면 서버는 해당 메시지를 조회하여 reply 메타데이터 필드를 채운다.
   - 원본 메시지가 존재하지 않으면 전송 실패(예외).
   - 원본 메시지가 `isDeleted = true`이면 전송 실패(예외).
   - 원본 메시지가 답장 불가 유형(SYSTEM, BOT, ROOM_LINK, STUDENT_ID_REQUEST)이면 전송 실패(예외).
3. 원본이 `REOPEN_CARD` 타입이면 content JSON에서 `derivedRoomId`를 파싱하여 `replyToDerivedRoomId`에 저장한다.
4. `replyToMessageId`가 null이면 일반 메시지로 저장한다.

### 답장 조회

메시지 목록 조회 시, 답장 메시지(`replyToMessageId != null`)에는 `replySource` 객체를 포함한다.
`replySource`의 `status`는 원본 메시지의 현재 상태를 나타낸다.

| status | 조건 |
|---|---|
| `NORMAL` | 원본 메시지 존재 + `isDeleted = false` + REOPEN_CARD 아닌 경우 |
| `DELETED` | 원본 메시지 존재 + `isDeleted = true` |
| `NOT_FOUND` | 원본 메시지 ID가 DB에 없음 |
| `RECRUITING` | 원본이 REOPEN_CARD + 파생 톡방 `recruitmentClosed = false` |
| `RECRUITMENT_CLOSED` | 원본이 REOPEN_CARD + 파생 톡방 `recruitmentClosed = true` |

### 메시지 soft-delete

- 메시지 삭제 시 실제 행을 삭제하지 않고 `isDeleted = true`로 변경한다.
- 삭제된 메시지는 목록 조회에서 기존과 동일하게 포함되나 content 대신 삭제 표시 처리는 클라이언트 담당이다.
- 삭제 권한: 메시지 발신자 본인만 삭제 가능.

---

## 도메인 데이터

### 신규 enum — `shared/enums/ChatRoomType`

```java
public enum ChatRoomType {
    ROOMMATE, OPEN, DERIVED
}
```

### 신규 enum — `shared/enums/ReplySourceStatus`

```java
public enum ReplySourceStatus {
    NORMAL, DELETED, NOT_FOUND, RECRUITING, RECRUITMENT_CLOSED
}
```

### `OpenChatMessage` 추가 필드

| 필드명 | 타입 | nullable | 설명 |
|---|---|---|---|
| `isDeleted` | boolean | No (default false) | soft-delete 여부 |
| `replyToMessageId` | Long | Yes | 원본 메시지 ID |
| `replyToMessageType` | OpenChatMessageType | Yes | 원본 메시지 유형 |
| `replyToSenderId` | Long | Yes | 원본 작성자 ID |
| `replyToRoomId` | Long | Yes | 원본 채팅방 ID |
| `replyToRoomType` | ChatRoomType | Yes | 원본 채팅방 유형 (OPEN 또는 DERIVED) |
| `replyToDerivedRoomId` | Long | Yes | 파생 톡방 카드 답장 시 해당 파생 톡방 ID |

### `RoommateChattingChat` 추가 필드

| 필드명 | 타입 | nullable | 설명 |
|---|---|---|---|
| `isDeleted` | boolean | No (default false) | soft-delete 여부 |
| `replyToMessageId` | Long | Yes | 원본 메시지 ID |
| `replyToSenderId` | Long | Yes | 원본 작성자 ID |
| `replyToRoomId` | Long | Yes | 원본 채팅방 ID (RoommateChattingRoom.id) |

- `RoommateChattingChat` 답장의 `replyToRoomType`은 항상 `ROOMMATE`이므로 DB 저장 없이 DTO 조립 시 상수로 포함.
- 룸메톡방 메시지는 별도 메시지 유형 enum이 없으므로(일반 메시지는 `messageType = null`) `replyToMessageType` 필드 추가 없이 `replyToSenderId`만 저장.

### 요청 DTO 변경

- `RequestOpenChatMessageDto`: `replyToMessageId` (Long, nullable) 추가
- `RequestRoommateChatDto`: `replyToMessageId` (Long, nullable) 추가

### 응답 DTO — `ReplySourceDto` (신규)

```
ReplySourceDto {
    Long         replyToMessageId
    ReplySourceStatus status
    Long         replyToSenderId          // DELETED, NOT_FOUND이면 null
    String       replyToSenderNickname    // DELETED, NOT_FOUND이면 null
    String       contentPreview           // DELETED, NOT_FOUND이면 null; 최대 100자
    ChatRoomType replyToRoomType
    Long         replyToRoomId
    Long         replyToDerivedRoomId     // RECRUITING, RECRUITMENT_CLOSED이면 포함; 그 외 null
}
```

- `ResponseOpenChatMessageDto`와 `ResponseRoommateChatDto`에 `ReplySourceDto replySource` 추가. 답장이 아닌 메시지는 null.

---

## 비즈니스 규칙 / 제약

### 답장 가능 메시지 유형

| 도메인 | 답장 가능 | 답장 불가 |
|---|---|---|
| 룸메톡방 | `isSystem = false` (텍스트·사진 메시지) | `isSystem = true` (시스템, STUDENT_ID_REQUEST) |
| 오픈채팅(OPEN) | TEXT, IMAGE, REOPEN_CARD | SYSTEM, ROOM_LINK, STUDENT_ID_REQUEST, BOT |
| 파생톡방(DERIVED) | TEXT, IMAGE | SYSTEM, ROOM_LINK, STUDENT_ID_REQUEST, BOT |

### 답장 범위

- 답장은 반드시 같은 채팅방 내 메시지에 대해서만 가능하다. 다른 채팅방의 메시지에 대한 교차 답장 불가.
- 중첩 답장(답장에 대한 답장)은 불가. `replyToMessageId`가 이미 있는 메시지에 답장 시도 → 예외.

### soft-delete 보호

- 삭제된 메시지(`isDeleted = true`)에 대한 새 답장 전송은 불가.
- 삭제된 메시지를 원본으로 갖는 기존 답장은 유지되며, 조회 시 `status = DELETED`로 반환.

### PERSONAL 타입 OpenChatRoom

- `OpenChatRoomType.PERSONAL` 방은 이번 범위에서 제외. 답장 지원 안 함.

---

## 예외 · 경계 상황

| 상황 | 기대 동작 |
|---|---|
| 존재하지 않는 `replyToMessageId`로 전송 | 예외 (MESSAGE_NOT_FOUND) |
| 이미 삭제된 메시지에 답장 전송 | 예외 (MESSAGE_ALREADY_DELETED) |
| 답장 불가 유형 메시지에 답장 전송 | 예외 (REPLY_NOT_ALLOWED_FOR_MESSAGE_TYPE) |
| 다른 방의 메시지에 답장 전송 | 예외 (REPLY_TARGET_NOT_IN_SAME_ROOM) |
| 중첩 답장 시도 | 예외 (NESTED_REPLY_NOT_ALLOWED) |
| REOPEN_CARD 답장 시 content에서 `derivedRoomId` 파싱 실패 | 예외 (DERIVED_ROOM_ID_PARSE_FAILED) |
| 삭제 요청자가 메시지 발신자가 아닌 경우 | 예외 (FORBIDDEN) |
| 답장 조회 시 원본 메시지 ID가 DB에 없음 | 예외 없이 `status = NOT_FOUND`로 반환 |
| 답장 조회 시 REOPEN_CARD의 파생 톡방이 없음 | `status = NOT_FOUND`로 반환 |

---

## 비목표 (Non-goals)

- 중첩 답장(답장에 대한 답장)
- 답장 전송 시 원본 메시지 발신자에게 FCM 알림
- 답장 메시지의 미읽음 카운트 별도 처리
- PERSONAL 타입 OpenChatRoom 답장
- 오픈채팅 / 파생톡방 SYSTEM, ROOM_LINK, BOT, STUDENT_ID_REQUEST 타입 답장
- 룸메톡방 STUDENT_ID_REQUEST 답장
- 기존 메시지의 수정(edit) 기능

---

## 수용 기준 (Acceptance Criteria)

### 답장 전송

1. **[OPEN] 텍스트 메시지 답장 저장**
   - Given: OPEN 방에 TEXT 메시지 A 존재
   - When: 같은 방에서 `replyToMessageId = A.id`로 TEXT 메시지 전송
   - Then: 저장된 메시지의 `replyToMessageId = A.id`, `replyToSenderId = A.senderId`, `replyToRoomType = OPEN`, `replyToDerivedRoomId = null`

2. **[OPEN] REOPEN_CARD 답장 시 파생 톡방 ID 저장**
   - Given: OPEN 방에 REOPEN_CARD 메시지 C 존재 (content에 `derivedRoomId=5`)
   - When: `replyToMessageId = C.id`로 TEXT 메시지 전송
   - Then: `replyToMessageType = REOPEN_CARD`, `replyToDerivedRoomId = 5`

3. **[DERIVED] 텍스트 메시지 답장 저장**
   - Given: DERIVED 방에 TEXT 메시지 D 존재
   - When: 같은 방에서 `replyToMessageId = D.id`로 TEXT 메시지 전송
   - Then: `replyToRoomType = DERIVED`

4. **[ROOMMATE] 텍스트 메시지 답장 저장**
   - Given: 룸메 채팅방에 일반 메시지 R 존재
   - When: `replyToMessageId = R.id`로 메시지 전송
   - Then: `replyToMessageId = R.id`, `replyToSenderId = R.member.id`, `replyToRoomId = R.roommateChattingRoom.id`

5. **[FAIL] 존재하지 않는 원본 ID로 답장**
   - Given: 존재하지 않는 messageId = 9999
   - When: `replyToMessageId = 9999`로 전송
   - Then: MESSAGE_NOT_FOUND 예외

6. **[FAIL] 삭제된 메시지에 답장**
   - Given: `isDeleted = true`인 메시지
   - When: 해당 메시지에 답장 전송
   - Then: MESSAGE_ALREADY_DELETED 예외

7. **[FAIL] 답장 불가 유형(SYSTEM)에 답장**
   - Given: SYSTEM 타입 메시지
   - When: 해당 메시지에 답장 전송
   - Then: REPLY_NOT_ALLOWED_FOR_MESSAGE_TYPE 예외

8. **[FAIL] 다른 방의 메시지에 답장**
   - Given: 방 A의 메시지, 방 B에서 전송 시도
   - When: `replyToMessageId`가 방 A의 메시지 ID
   - Then: REPLY_TARGET_NOT_IN_SAME_ROOM 예외

9. **[FAIL] 중첩 답장 시도**
   - Given: 이미 `replyToMessageId`가 있는 답장 메시지 M
   - When: M에 대해 답장 전송
   - Then: NESTED_REPLY_NOT_ALLOWED 예외

### 답장 조회 — replySource 상태

10. **NORMAL 상태 반환**
    - Given: 정상 TEXT 메시지에 대한 답장
    - When: 메시지 목록 조회
    - Then: `replySource.status = NORMAL`, `contentPreview` 포함

11. **DELETED 상태 반환**
    - Given: 답장의 원본 메시지가 `isDeleted = true`
    - When: 메시지 목록 조회
    - Then: `replySource.status = DELETED`, `contentPreview = null`

12. **NOT_FOUND 상태 반환**
    - Given: 저장된 `replyToMessageId`가 DB에 없음
    - When: 메시지 목록 조회
    - Then: `replySource.status = NOT_FOUND`

13. **RECRUITING 상태 반환**
    - Given: 원본이 REOPEN_CARD, 파생 톡방 `recruitmentClosed = false`
    - When: 메시지 목록 조회
    - Then: `replySource.status = RECRUITING`, `replyToDerivedRoomId` 포함

14. **RECRUITMENT_CLOSED 상태 반환**
    - Given: 원본이 REOPEN_CARD, 파생 톡방 `recruitmentClosed = true`
    - When: 메시지 목록 조회
    - Then: `replySource.status = RECRUITMENT_CLOSED`

### soft-delete

15. **메시지 발신자 본인이 soft-delete 가능**
    - Given: userId=1이 보낸 메시지
    - When: userId=1이 삭제 요청
    - Then: `isDeleted = true`, 행은 DB에 유지

16. **타인은 soft-delete 불가**
    - Given: userId=1이 보낸 메시지
    - When: userId=2가 삭제 요청
    - Then: FORBIDDEN 예외
