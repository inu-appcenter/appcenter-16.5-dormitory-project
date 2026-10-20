# BR-772 메시지 수정 API

## 기능 요약

오픈채팅 메시지 작성자가 자신의 텍스트 메시지를 수정한다.
수정 내역은 엔티티에 영구 저장되며, 이를 인용하는 답장의 `contentPreview`와 채팅방 최근 메시지 미리보기에 즉시 반영된다.
REST 요청으로 수정을 수행하고, 수정 이벤트를 WebSocket으로 broadcast한다.

---

## 동작 명세

### 정상 흐름

1. 클라이언트가 `PATCH /open-chat-rooms/{roomId}/messages/{messageId}`를 호출한다.
2. 서버는 메시지를 조회한다.
3. 요청자 == 발신자인지 확인한다.
4. 메시지 타입이 `TEXT`인지 확인한다.
5. 메시지가 삭제되지 않았는지 확인한다.
6. 새 내용이 공백이 아닌지 확인한다.
7. 새 내용이 기존 내용과 다른지 확인한다.
8. `message.content`를 새 내용으로 교체하고, `message.editedAt`을 현재 시각으로 기록한다.
9. 해당 메시지가 채팅방의 가장 최신 메시지라면 `room.lastMessage`와 `room.lastMessageAt`을 수정된 내용으로 갱신한다.
10. WebSocket topic `/sub/openchat/{roomId}/edit`으로 수정 이벤트를 broadcast한다.
11. HTTP 200 + 수정된 메시지 DTO를 반환한다.

### 답장 인용 갱신

`buildReplySources`는 원본 메시지를 DB에서 실시간으로 읽어 `contentPreview`를 구성한다.
따라서 `content`가 수정되면 이후 메시지 목록 조회 시 답장의 인용 텍스트가 자동으로 최신 내용을 반영한다.
별도 테이블이나 추가 쿼리는 필요하지 않다.

### WebSocket 이벤트

브로드캐스트 DTO (`ResponseOpenChatMessageEditEventDto`):

| 필드 | 타입 | 설명 |
|------|------|------|
| `messageId` | Long | 수정된 메시지 ID |
| `roomId` | Long | 채팅방 ID |
| `content` | String | 수정 후 내용 |
| `editedAt` | LocalDateTime | 수정 시각 |

---

## 도메인 데이터

### OpenChatMessage — 신규 컬럼

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `editedAt` | LocalDateTime | nullable | 최초 수정 시 기록, null이면 미수정 |

> `isEdited` 별도 플래그 없이 `editedAt != null`로 수정 여부를 판단한다.

### ResponseOpenChatMessageDto — 신규 응답 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `isEdited` | boolean | `editedAt != null` |
| `editedAt` | LocalDateTime | 수정 시각, 미수정이면 null |

---

## 비즈니스 규칙 / 제약

| 규칙 | 위반 시 ErrorCode |
|------|------------------|
| 메시지가 존재해야 함 | `OPEN_CHAT_MESSAGE_NOT_FOUND` (22020) |
| 요청자 == `message.senderId` | `OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER` (22038) |
| `message.type == TEXT` | `OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE` (22039, 신규) |
| `message.isDeleted == false` | `OPEN_CHAT_MESSAGE_ALREADY_DELETED` (22033) |
| 새 내용이 공백이 아님 (`@NotBlank`) | 400 BAD_REQUEST (Bean Validation) |
| 새 내용 ≠ 기존 내용 | `OPEN_CHAT_MESSAGE_CONTENT_UNCHANGED` (22040, 신규) |

수정 횟수 제한 없음. 재수정 가능.

---

## 예외 · 경계 상황

| 상황 | 기대 동작 |
|------|-----------|
| 존재하지 않는 `messageId` | 404 + `OPEN_CHAT_MESSAGE_NOT_FOUND` |
| 타인의 메시지 수정 | 403 + `OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER` |
| `IMAGE`, `SYSTEM`, `BOT`, `ROOM_LINK`, `STUDENT_ID_REQUEST`, `REOPEN_CARD` 타입 | 400 + `OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE` |
| 삭제된 메시지 수정 | 400 + `OPEN_CHAT_MESSAGE_ALREADY_DELETED` |
| 빈 문자열·공백만 입력 | 400 (Bean Validation) |
| 기존 내용과 동일한 내용 | 400 + `OPEN_CHAT_MESSAGE_CONTENT_UNCHANGED` |
| 수정 대상이 채팅방 최신 메시지인 경우 | 수정 내용을 `room.lastMessage`에 반영 |
| 수정 대상이 채팅방 최신 메시지가 아닌 경우 | `room.lastMessage` 변경 없음 |

---

## 비목표 (Non-goals)

- 수정 이력(히스토리) 저장 — 최신 내용만 보존
- 관리자의 타인 메시지 강제 수정
- 이미지·시스템·봇 메시지 수정
- 수정 알림 푸시(FCM) 발송
- 수정 횟수 제한
- 수정된 답장 인용에 별도 `EDITED` ReplySourceStatus 추가

---

## 수용 기준 (Acceptance Criteria)

### AC-1. 정상 텍스트 메시지 수정
```
Given  본인이 작성한 TEXT 메시지(미삭제)가 존재하고
When   PATCH /open-chat-rooms/{roomId}/messages/{messageId} { "content": "수정된 내용" } 요청 시
Then   HTTP 200 반환
       message.content == "수정된 내용"
       message.editedAt != null
       응답 DTO.isEdited == true
```

### AC-2. 타인 메시지 수정 차단
```
Given  다른 사용자가 작성한 메시지가 존재하고
When   PATCH 요청 시
Then   HTTP 403 + OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER
```

### AC-3. 비(非)TEXT 메시지 수정 차단
```
Given  IMAGE / SYSTEM / BOT / ROOM_LINK / STUDENT_ID_REQUEST / REOPEN_CARD 타입 메시지
When   PATCH 요청 시
Then   HTTP 400 + OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE
```

### AC-4. 삭제된 메시지 수정 차단
```
Given  isDeleted == true인 본인 메시지
When   PATCH 요청 시
Then   HTTP 400 + OPEN_CHAT_MESSAGE_ALREADY_DELETED
```

### AC-5. 동일 내용 수정 차단
```
Given  현재 content == "기존 내용"인 메시지
When   PATCH { "content": "기존 내용" } 요청 시
Then   HTTP 400 + OPEN_CHAT_MESSAGE_CONTENT_UNCHANGED
```

### AC-6. 빈 내용 차단
```
Given  유효한 텍스트 메시지
When   PATCH { "content": "   " } 요청 시
Then   HTTP 400
```

### AC-7. 최신 메시지 수정 시 채팅방 미리보기 갱신
```
Given  수정 대상 메시지가 채팅방의 가장 마지막 메시지이고
When   정상 수정 요청 시
Then   room.lastMessage == 수정된 내용
```

### AC-8. 최신 메시지가 아닌 경우 채팅방 미리보기 유지
```
Given  수정 대상 메시지 이후에 다른 메시지가 존재하고
When   정상 수정 요청 시
Then   room.lastMessage 변경 없음
```

### AC-9. 수정 후 답장 인용 텍스트 갱신
```
Given  수정 대상 메시지를 인용하는 답장 메시지가 존재하고
When   메시지 목록 조회 시
Then   해당 답장의 replySource.contentPreview == 수정된 내용 (앞 100자)
       replySource.status == NORMAL
```

### AC-10. WebSocket 수정 이벤트 broadcast
```
Given  정상 수정 요청이 처리되면
Then   /sub/openchat/{roomId}/edit 으로
       { messageId, roomId, content, editedAt } 이벤트 전송
```
