# BR-772 메시지 수정 API 명세서

> Base URL: `http://localhost:8080`
> 인증: 모든 엔드포인트에 `Authorization: Bearer {JWT}` 헤더 필요

---

## 텍스트 메시지 수정

| 항목 | 내용 |
|------|------|
| **메서드** | `PATCH` |
| **경로** | `/open-chat-rooms/{roomId}/messages/{messageId}` |
| **인증** | Bearer Token (필수) |
| **설명** | 본인이 작성한 TEXT 타입 메시지의 내용을 수정한다. 성공 시 수정 이벤트를 WebSocket으로 broadcast한다. |

### Request

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `roomId` | `Long` | ✅ | 메시지가 속한 채팅방 ID |
| `messageId` | `Long` | ✅ | 수정할 메시지 ID |

#### Request Body

Content-Type: `application/json`

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `content` | `String` | ✅ | 공백 불가 (`@NotBlank`) | 수정할 새 텍스트 내용 |

```json
{
  "content": "수정된 메시지 내용입니다."
}
```

---

### Response

#### 성공 응답 — `200 OK`

수정된 메시지의 전체 DTO를 반환한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `messageId` | `Long` | 메시지 ID |
| `roomId` | `Long` | 채팅방 ID |
| `senderId` | `Long` | 발신자 user ID |
| `senderNickname` | `String` | 발신자 닉네임 |
| `content` | `String` | **수정된** 텍스트 내용 |
| `type` | `String` | 메시지 타입 — 항상 `"TEXT"` |
| `imageUrls` | `String[]` | 이미지 URL 목록 — TEXT 메시지는 빈 배열 `[]` |
| `unreadCount` | `Int` | 읽지 않은 참여자 수 — 수정 시 0 고정 |
| `createdAt` | `LocalDateTime` | 최초 전송 시각 |
| `isEdited` | `Boolean` | 수정 여부 — 수정 후 항상 `true` |
| `editedAt` | `LocalDateTime` | 수정 시각 |
| `isBot` | `Boolean` | 봇 메시지 여부 — 항상 `false` |
| `isDeleted` | `Boolean` | 삭제 여부 — 항상 `false` |
| `linkedRoomId` | `Long\|null` | 링크된 채팅방 ID (ROOM_LINK 전용) — `null` |
| `linkedRoomName` | `String\|null` | 링크된 채팅방 이름 — `null` |
| `linkedRoomDescription` | `String\|null` | 링크된 채팅방 설명 — `null` |
| `linkedRoomMaxParticipants` | `Int\|null` | 링크된 채팅방 최대 인원 — `null` |
| `linkedRoomRecruitmentClosed` | `Boolean\|null` | 링크된 채팅방 마감 여부 — `null` |
| `linkedRoomRecruitmentStatus` | `String\|null` | 링크된 채팅방 모집 상태 — `null` |
| `disclosureRequestId` | `Long\|null` | 학번 공개 요청 ID — `null` |
| `replySource` | `ReplySourceDto\|null` | 답장 인용 정보 — 해당 없으면 `null` |

**`replySource` 내부 구조** (답장 메시지에만 존재):

| 필드 | 타입 | 설명 |
|------|------|------|
| `replyToMessageId` | `Long` | 인용된 원본 메시지 ID |
| `status` | `String` | `NORMAL` \| `DELETED` \| `NOT_FOUND` \| `RECRUITING` \| `RECRUITMENT_CLOSED` |
| `replyToSenderId` | `Long\|null` | 원본 발신자 ID |
| `replyToSenderNickname` | `String\|null` | 원본 발신자 닉네임 |
| `contentPreview` | `String\|null` | 원본 내용 앞 100자 |
| `replyToRoomType` | `String\|null` | 원본 채팅방 타입 |
| `replyToRoomId` | `Long\|null` | 원본 채팅방 ID |
| `replyToDerivedRoomId` | `Long\|null` | 원본 파생 톡방 ID |

```json
{
  "messageId": 101,
  "roomId": 5,
  "senderId": 42,
  "senderNickname": "홍길동",
  "content": "수정된 메시지 내용입니다.",
  "type": "TEXT",
  "imageUrls": [],
  "unreadCount": 0,
  "createdAt": "2026-10-05T10:00:00",
  "isEdited": true,
  "editedAt": "2026-10-05T10:15:00",
  "isBot": false,
  "isDeleted": false,
  "linkedRoomId": null,
  "linkedRoomName": null,
  "linkedRoomDescription": null,
  "linkedRoomMaxParticipants": null,
  "linkedRoomRecruitmentClosed": null,
  "linkedRoomRecruitmentStatus": null,
  "disclosureRequestId": null,
  "replySource": null
}
```

#### 에러 응답 형식

```json
{
  "code": 22039,
  "name": "OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE",
  "message": "[OpenChat] 텍스트 메시지만 수정할 수 있습니다.",
  "errors": null
}
```

| 상태 코드 | `code` | `name` | 발생 조건 |
|-----------|--------|--------|-----------|
| `400 Bad Request` | `2001` | `VALIDATION_FAILED` | `content`가 빈 문자열 또는 공백만 포함 (`@NotBlank` 위반) — `errors` 배열에 필드별 오류 목록 포함 |
| `400 Bad Request` | `22033` | `OPEN_CHAT_MESSAGE_ALREADY_DELETED` | 이미 삭제된 메시지 수정 시도 |
| `400 Bad Request` | `22039` | `OPEN_CHAT_MESSAGE_EDIT_FORBIDDEN_TYPE` | TEXT가 아닌 타입(`IMAGE`, `SYSTEM`, `BOT`, `ROOM_LINK`, `STUDENT_ID_REQUEST`, `REOPEN_CARD`) 수정 시도 |
| `400 Bad Request` | `22040` | `OPEN_CHAT_MESSAGE_CONTENT_UNCHANGED` | 기존 내용과 동일한 내용으로 수정 시도 |
| `401 Unauthorized` | `1007` | `JWT_ENTRY_POINT` | 인증 토큰 없음 또는 만료 |
| `403 Forbidden` | `22038` | `OPEN_CHAT_MESSAGE_NOT_OWNED_BY_USER` | 본인이 작성한 메시지가 아님 |
| `404 Not Found` | `22020` | `OPEN_CHAT_MESSAGE_NOT_FOUND` | 존재하지 않는 `messageId` |

---

## WebSocket 수정 이벤트

메시지 수정 성공 시 채팅방 구독자 전체에게 broadcast된다.

| 항목 | 내용 |
|------|------|
| **Topic** | `/sub/openchat/{roomId}/edit` |
| **발행 시점** | REST 수정 요청 성공 직후 |

### Payload

| 필드 | 타입 | 설명 |
|------|------|------|
| `messageId` | `Long` | 수정된 메시지 ID |
| `roomId` | `Long` | 채팅방 ID |
| `content` | `String` | 수정 후 텍스트 내용 |
| `editedAt` | `LocalDateTime` | 수정 시각 |

```json
{
  "messageId": 101,
  "roomId": 5,
  "content": "수정된 메시지 내용입니다.",
  "editedAt": "2026-10-05T10:15:00"
}
```

---

## 메시지 조회 응답 변경 (기존 엔드포인트)

### GET `/open-chat-rooms/{roomId}/messages`

기존 메시지 목록 조회 응답에 신규 필드가 추가된다.

**`messages[].` 신규 필드:**

| 필드 | 타입 | 설명 |
|------|------|------|
| `isEdited` | `Boolean` | 수정 여부. 수정된 메시지는 `true`, 미수정은 `false` |
| `editedAt` | `LocalDateTime\|null` | 수정 시각. 미수정 메시지는 `null` |

> `replySource.contentPreview`는 원본 메시지의 현재 `content`를 실시간 반영하므로,
> 수정된 메시지를 인용한 답장은 별도 처리 없이 갱신된 미리보기를 반환한다.
